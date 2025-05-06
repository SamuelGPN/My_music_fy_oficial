package com.example.my_music_fy_oficial;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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
import java.util.Objects;

public class MainActivity1 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        Button botaoTocar = findViewById(R.id.pesquisarbtn);

        botaoTocar.setOnClickListener(v -> {
            String nomeMusica = Objects.requireNonNull(editTextNomeMusica.getText()).toString().trim();

            if (nomeMusica.isEmpty()) {
                textInputLayout.setError("Digite o nome da música");
            } else {
                textInputLayout.setError(null); // limpa erro

                // Chaquopy
                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(this));
                }

                File privateDir = new File(getFilesDir(), "musica.mp3");  // Isso dá: /data/data/seu.pacote/files
                Python py = Python.getInstance();
                PyObject module = py.getModule("main");
                PyObject result = module.callAttr("main", privateDir.getAbsolutePath(), nomeMusica); //A função do python deve ficar nestas aspas
                String resposta = result.toString();
                Log.d("Python", resposta); //para verificar o log no logcat


                // Agora toca com MediaPlayer
                MediaPlayer player = new MediaPlayer();
                player.setAudioStreamType(AudioManager.STREAM_MUSIC); // Define o tipo de áudio

                try {
                    player.setDataSource(resposta); // sua URL aqui
                    player.setOnPreparedListener(MediaPlayer::start); // inicia quando estiver pronto
                    player.prepareAsync();
                } catch (IOException e) {
                    Log.e("PythonError", "Erro ao chamar o Python", e);
                }

                //try {
                //    player.setDataSource(privateDir.getAbsolutePath());
                //    player.setOnPreparedListener(mp -> mp.start()); // inicia quando estiver pronto
                //    player.prepareAsync(); // prepara de forma assíncrona
                //} catch (IOException e) {
                //    e.printStackTrace();
                //}
            }

        });
    }
}