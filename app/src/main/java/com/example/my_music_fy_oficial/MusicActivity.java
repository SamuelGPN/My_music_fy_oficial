package com.example.my_music_fy_oficial;


import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.media3.common.Player;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.List;



public class MusicActivity extends AppCompatActivity {
    private MediaController mediaController;

    private ImageButton playPauseBtn;
    private SeekBar seekBar;
    private Handler handler = new Handler();
    private boolean isUserSeeking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        // Dados recebidos do Intent
        Intent intent = getIntent();
        String titulo = intent.getStringExtra("titulo");
        String url = intent.getStringExtra("url");
        String url_ant = intent.getStringExtra("url_anterior");

        TextView tituloMusic = findViewById(R.id.tituloMusica);
        tituloMusic.setText(titulo);

        // Referências dos componentes da interface
        playPauseBtn = findViewById(R.id.pausarbtn);
        seekBar = findViewById(R.id.seekBar);

        // Clique Play/Pause
        playPauseBtn.setOnClickListener(v -> {
            if (mediaController != null) {
                if (mediaController.isPlaying()) {
                    mediaController.pause();
                } else {
                    mediaController.play();
                }
            }
        });

        // Controle SeekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaController != null) {
                    mediaController.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Conectar ao PlaybackService
        SessionToken sessionToken = new SessionToken(
                this,
                new ComponentName(this, PlaybackService.class)
        );

        ListenableFuture<MediaController> controllerFuture =
                new MediaController.Builder(this, sessionToken).buildAsync();

        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();

                atualizarBotaoPlayPause();

                // Listener de eventos
                mediaController.addListener(new Player.Listener() {
                    @Override
                    public void onIsPlayingChanged(boolean isPlaying) {
                        atualizarBotaoPlayPause();
                    }

                    @Override
                    public void onPlaybackStateChanged(int playbackState) {
                        atualizarBotaoPlayPause();
                    }
                });

                iniciarSeekBar();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    protected void onStop() {
        super.onStop();
        pararSeekBar();
        if (mediaController != null) {
            mediaController.release();
            mediaController = null;
        }
    }

    // Atualizar ícone do botão play/pause
    private void atualizarBotaoPlayPause() {
        runOnUiThread(() -> {
            if (mediaController.isPlaying()) {
                playPauseBtn.setImageResource(R.drawable.pause_icon);
            } else {
                playPauseBtn.setImageResource(R.drawable.play_icon);
            }
        });
    }

    // Controle da SeekBar
    private void iniciarSeekBar() {
        handler.postDelayed(seekBarRunnable, 500);
    }

    private void pararSeekBar() {
        handler.removeCallbacks(seekBarRunnable);
    }

    private final Runnable seekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaController != null && !isUserSeeking) {
                long position = mediaController.getCurrentPosition();
                long duration = mediaController.getDuration();

                seekBar.setMax((int) duration);
                seekBar.setProgress((int) position);
            }
            handler.postDelayed(this, 500);
        }
    };
}
