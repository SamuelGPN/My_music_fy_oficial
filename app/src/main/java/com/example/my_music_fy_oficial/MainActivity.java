package com.example.my_music_fy_oficial;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.Manifest;

public class MainActivity extends AppCompatActivity {

    private void mostrarNotificacao() {
        boolean tocando = player != null && player.isPlaying();

        int iconeAcao = tocando ? R.drawable.pause_icon : R.drawable.play_icon;
        String textoAcao = tocando ? "Pausar" : "Continuar";

        Intent pauseIntent = new Intent(this, NotificationReceiver.class);
        pauseIntent.setAction("PAUSE_PLAY");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "canal_musica")
                .setSmallIcon(R.drawable.musica_icon) // um ícone genérico pequeno (recomendo algo tipo uma nota musical)
                .setContentTitle("Música")
                .setContentText(tocando ? "Tocando..." : "Pausada")
                .addAction(iconeAcao, textoAcao, pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOnlyAlertOnce(true); // <-- Garante que a notificação seja atualizada, não recriada

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.e("Notificacao", "Permissão de notificação não concedida.");
            return;
        }
        notificationManager.notify(1, builder.build()); // Mesmo ID = substituição da notificação
    }

   // Para lidar com as respostas
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissão concedida!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissão de notificação negada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    Handler handler = new Handler(Looper.getMainLooper());
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler mainHandler = new Handler(Looper.getMainLooper());
    ListView listView;
    public static MediaPlayer player;  // Agora ele continua existindo mesmo que a tela feche

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
        Button botaoPausar = findViewById(R.id.pausarbtn);
        ListView listView = findViewById(R.id.lista_resultados); // Corrigido: faltava declarar a variável

        botaoSearch.setOnClickListener(v -> {
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
                        listView.setOnItemClickListener((parent, view1, position, id) -> {
                            executor.execute(() -> {
                                // Código em background (sem travar UI)
                                String url = musicas.get(position).get("url");
                                PyObject module2 = py.getModule("main_processing");
                                PyObject result2 = module2.callAttr("get_youtube_download_link", url);
                                String resposta = result2.toString();

                                // Agora volta para a thread principal para atualizar UI
                                mainHandler.post(() -> {
                                    Log.d("Python", resposta);
                                    if (player != null) {
                                        player.release();
                                    }
                                    player = new MediaPlayer(); //reinicia o media player criando um novo objeto
                                    player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                                    botaoPausar.setEnabled(false);

                                    try {
                                        player.setDataSource(resposta);
                                        player.setOnPreparedListener(mp -> {
                                            mp.start();
                                            botaoPausar.setEnabled(true);
                                            mostrarNotificacao();
                                        });
                                        player.prepareAsync();
                                    } catch (IOException e) {
                                        Log.e("PythonError", "Erro ao chamar o Python", e);
                                    }
                                    botaoPausar.setOnClickListener(w -> {
                                        if (player != null) {
                                            if (player.isPlaying()) {
                                                player.pause();
                                            } else {
                                                player.start();
                                            }
                                            mostrarNotificacao(); // <-- Atualiza a notificação com o novo estado
                                        }
                                    });
                                });
                            });
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