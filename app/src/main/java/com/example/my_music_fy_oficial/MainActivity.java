package com.example.my_music_fy_oficial;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.Manifest;

public class MainActivity extends AppCompatActivity {

    Handler handler = new Handler(Looper.getMainLooper());
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler mainHandler = new Handler(Looper.getMainLooper());
    ListView listView;

    String url_anterior = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //para pedir a permissão de notificacao
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }


        NotificationChannel canal = new NotificationChannel(
                "canal_musica", "Canal Música",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(canal);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextInputLayout textInputLayout = findViewById(R.id.input_layout);
        TextInputEditText editTextNomeMusica = findViewById(R.id.text_input);
        Button botaoSearch = findViewById(R.id.pesquisarbtn);
        ListView listView = findViewById(R.id.lista_resultados); // Corrigido: faltava declarar a variável

        botaoSearch.setOnClickListener(v -> {
            botaoSearch.setEnabled(false);
            String nomeMusica = Objects.requireNonNull(editTextNomeMusica.getText()).toString().trim();

            if (nomeMusica.isEmpty()) {
                textInputLayout.setError("Digite o nome da música");
            } else {
                textInputLayout.setError(null); // limpa erro

                executor.execute(() -> {
                    // Código em background (sem travar UI)
                    if (!Python.isStarted()) {
                        Python.start(new AndroidPlatform(this));
                    }

                    File privateDir = new File(getFilesDir(), "musica.mp3");
                    Python py = Python.getInstance();
                    PyObject module = py.getModule("main");
                    PyObject result = module.callAttr("main", nomeMusica);

                    // Agora volta para a thread principal para atualizar UI
                    mainHandler.post(() -> {
                        System.out.println("oiiiiii " + result);
                        List<Map<String, String>> musicas = new ArrayList<>();
                        for (PyObject item : result.asList()) {
                            Map<PyObject, PyObject> dict = item.asMap();  // ← aqui converte explicitamente o PyObject para um Map

                            if (!dict.containsKey("titulo") || !dict.containsKey("url")) {
                                Log.w("PythonData", "Item com dados faltando: " + item.toString());
                                continue;
                            }

                            String titulo = dict.get("titulo").toString();
                            String url = dict.get("url").toString();

                            Log.d("PythonData", "Título: " + titulo + " | URL: " + url);

                            Map<String, String> map = new HashMap<>();
                            map.put("titulo", titulo);
                            map.put("url", url);
                            musicas.add(map);
                        }

                        SimpleAdapter adapter = new SimpleAdapter(
                                this,
                                musicas,
                                android.R.layout.simple_list_item_1,
                                new String[]{"titulo"},
                                new int[]{android.R.id.text1}
                        );

                        listView.setAdapter(adapter);
                        botaoSearch.setEnabled(true);
                        System.out.println("Python aqui");
                        listView.setOnItemClickListener((parent, view1, position, id) -> {

                            // 👉 TROCA DE TELA AQUI
                            Intent intent = new Intent(MainActivity.this, MusicActivity.class);
                            intent.putExtra("titulo", musicas.get(position).get("titulo"));
                            String url = musicas.get(position).get("url");
                            intent.putExtra("url", url);
                            intent.putExtra("url_anterior", url_anterior);
                            startActivity(intent);

                            url_anterior = url;
                        });
                    });
                });
            }
        });
    }

    /** PARA QUE AO SAIR DO APP O MEDIA PLAYER FECHAR
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();  // Libera recursos do MediaPlayer
            player = null;
        }
    }
    */

}