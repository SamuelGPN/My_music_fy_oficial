package com.example.my_music_fy_oficial;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity; // IMPORTANTE: essa linha
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;


public class MusicActivity extends AppCompatActivity { // HERDA AppCompatActivity
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler mainHandler = new Handler(Looper.getMainLooper());
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
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
    public static MediaPlayer player;  // Agora ele continua existindo mesmo que a tela feche

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music); // nome do seu layout

        // Recupera os dados do Intent
        Intent intent = getIntent();
        String titulo = intent.getStringExtra("titulo");
        String url = intent.getStringExtra("url");
        String url_ant = intent.getStringExtra("url_anterior");

        TextView tituloMusic = findViewById(R.id.tituloMusica);
        ImageButton botaoHome = findViewById(R.id.homebtn);
        ImageButton botaoPausar = findViewById(R.id.pausarbtn);


        executor.execute(() -> {
            botaoPausar.setEnabled(false);
            if (!Objects.equals(url, url_ant)) {
                if (player != null) {
                    player.release();
                }
                tituloMusic.setText("Carregando...");

                File privateDir = new File(getFilesDir(), "musica.mp3");
                Python py = Python.getInstance();
                PyObject module2 = py.getModule("main_processing");
                PyObject result2 = module2.callAttr("get_youtube_download_link", url);
                String resposta = result2.toString();
                Log.d("Python", resposta);

                player = new MediaPlayer(); //reinicia o media player criando um novo objeto
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);

                try {
                    player.setDataSource(resposta);
                    player.setOnPreparedListener(mp -> {
                        mp.start();
                        tituloMusic.setText(titulo);
                        botaoPausar.setEnabled(true);
                        mostrarNotificacao();// <-- notificação
                    });
                    player.prepareAsync();
                } catch (IOException e) {
                    Log.e("PythonError", "Erro ao chamar o Python", e);
                }
            }
            else {
                botaoPausar.setEnabled(true);
                if (!player.isPlaying()) {
                    botaoPausar.setImageResource(R.drawable.play_icon); // muda para "play"
                } else {
                    botaoPausar.setImageResource(R.drawable.pause_icon); // muda para "play"
                }
                tituloMusic.setText(titulo);
            }

            botaoPausar.setOnClickListener(w -> {
                if (player != null) {
                    if (player.isPlaying()) {
                        player.pause();
                        botaoPausar.setImageResource(R.drawable.play_icon); // muda para "play"
                    } else {
                        player.start();
                        botaoPausar.setImageResource(R.drawable.pause_icon); // muda para "play"
                    }
                    mostrarNotificacao(); // <-- Atualiza a notificação com o novo estado
                }
            });
            mainHandler.post(() -> {
                botaoHome.setOnClickListener(v -> {
                    finish();
                });
            });
        });
    }
}
