package com.example.my_music_fy_oficial;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.chaquo.python.PyObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicService extends Service {
    private MediaPlayer player;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean shouldStopWaiting = false;

    private static final String CHANNEL_ID = "canal_musica";

    @Override
    public void onCreate() {
        super.onCreate();
        criarCanalNotificacao();
        mostrarNotificacao();
    }

    private void iniciarPlayer(String path) {
        if (player != null) {
            player.stop();
            player.release();
        }

        player = new MediaPlayer();
        try {
            player.setDataSource(path); // caminho do arquivo de música
            player.prepare();
            player.setLooping(true);
            player.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String url = intent.getStringExtra("url");

        startPythonLoop(url, path -> {
            // Esse trecho roda quando o Python retorna o caminho
            runOnUiThread(() -> iniciarPlayer(path));  // inicia a música
        });

        return START_STICKY;
    }

    private void startPythonLoop(String url, OnPathReadyListener listener) {
        shouldStopWaiting = false;
        executor.submit(() -> {
            while (!shouldStopWaiting) {
                // chamada Python bloqueante
                PyObject result = PythonModelHolder.callModeloFromModel2(
                        url, caminho_arq_final, pasta.getAbsolutePath()
                );

                if (result != null) {
                    String path = result.toString();
                    if (path != null && !path.isEmpty()) {
                        listener.onPathReady(path);
                        break; // interrompe o loop após encontrar o caminho
                    }
                }

                // opcional: pausa para evitar sobrecarga
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }




    @Override
    public void onDestroy() {
        shouldStopWaiting = true;
        executor.shutdownNow();
        if (player != null) {
            player.stop();
            player.release();
        }
        stopForeground(true);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // 🔔 Notificação
    private void mostrarNotificacao() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Música e Processamento")
                .setContentText("Executando tarefas em segundo plano")
                .setSmallIcon(R.drawable.musica_icon)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        Notification notification = builder.build();
        startForeground(1, notification);
    }

    // 🔧 Cria o canal de notificação
    private void criarCanalNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CHANNEL_ID,
                    "Canal Música",
                    NotificationManager.IMPORTANCE_LOW
            );
            canal.setDescription("Canal para notificações do MusicService");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(canal);
            }
        }
    }
}


