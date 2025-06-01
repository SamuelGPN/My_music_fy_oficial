package com.example.my_music_fy_oficial;

import android.content.ComponentName;
import android.content.Intent;

import android.os.Bundle;

import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity; // IMPORTANTE: essa linha
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.media3.ui.PlayerView;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;


public class MusicActivity extends AppCompatActivity { // HERDA AppCompatActivity
    private PlayerView playerView; // Seu player view no layout
    private MediaController mediaController; // Controller para controlar o player remoto

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music); // nome do seu layout

        playerView = findViewById(R.id.player_view);  // <-- Inicializa playerView aqui

        // Recupera os dados do Intent
        Intent intent = getIntent();
        String titulo = intent.getStringExtra("titulo");
        String url = intent.getStringExtra("url");
        String url_ant = intent.getStringExtra("url_anterior");

        TextView tituloMusic = findViewById(R.id.tituloMusica);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Cria o SessionToken apontando para o PlaybackService
        SessionToken sessionToken = new SessionToken(
                this,
                new ComponentName(this, PlaybackService.class)
        );

        // Constrói o MediaController assincronamente
        ListenableFuture<MediaController> controllerFuture =
                new MediaController.Builder(this, sessionToken).buildAsync();

        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();

                // Define o MediaController como player do PlayerView
                playerView.setPlayer(mediaController);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Liberar referência para evitar leaks
        if (mediaController != null) {
            playerView.setPlayer(null);
            mediaController.release();
            mediaController = null;
        }
    }

}
