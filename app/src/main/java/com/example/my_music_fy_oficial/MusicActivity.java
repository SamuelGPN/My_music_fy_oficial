package com.example.my_music_fy_oficial;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri; // Importe Uri
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.media3.common.Player;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import com.chaquo.python.PyObject;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.File;


public class MusicActivity extends AppCompatActivity {
    private MediaController mediaController;

    private ImageButton playPauseBtn;
    private SeekBar seekBar;
    private ImageView background;

    private Handler handler = new Handler();
    private boolean isUserSeeking = false;

    private MyDownloadCompleteReceiver downloadReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        // Referências dos componentes da interface
        playPauseBtn = findViewById(R.id.pausarbtn);
        seekBar = findViewById(R.id.seekBar);
        background = findViewById(R.id.imageViewMusica);

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

        // Registra o BroadcastReceiver
        downloadReceiver = new MyDownloadCompleteReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                downloadReceiver, new IntentFilter("ACTION_DOWNLOAD_COMPLETE"));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                downloadReceiver, new IntentFilter("ACTION_DOWNLOAD_ERROR"));
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Dados recebidos do Intent
        Intent intent = getIntent();
        String titulo = intent.getStringExtra("titulo");
        String url = intent.getStringExtra("url");
        String url_ant = intent.getStringExtra("url_anterior");

        TextView tituloMusic = findViewById(R.id.tituloMusica);
        tituloMusic.setText(titulo);

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

                // *** LÓGICA DE INÍCIO DO DOWNLOAD NO SERVIÇO ***
                // A MusicActivity agora DELEGA o download para o PlaybackService
                if (!Objects.equals(url, url_ant)) {
                    // Chame o método do serviço para iniciar o download
                    // Você precisará de uma forma de chamar o método público do serviço.
                    // Uma forma simples é criar um Intent e enviar comandos,
                    // ou usar um Binder se você já estiver vinculando o serviço.
                    // Para MediaSessionService, você pode adicionar um SessionCommand
                    // personalizado para isso, ou usar um Intent direto.
                    // Exemplo SIMPLES com Intent:
                    Intent serviceIntent = new Intent(this, PlaybackService.class);
                    serviceIntent.setAction("ACTION_START_DOWNLOAD_AND_PLAY");
                    serviceIntent.putExtra("VIDEO_URL", url);
                    startService(serviceIntent); // Inicia/envia intent para o serviço

                    // Mostrar loading na UI da Activity
                    runOnUiThread(() -> { /* mostrar loading */ });

                } else {
                    // Se a música já está sendo tocada pelo serviço, apenas atualize a UI
                    // Você pode precisar de uma forma de pedir ao serviço o caminho da imagem
                    // se ele já foi carregado e não há necessidade de re-baixar.
                    atualizarBotaoPlayPause();
                    // Aqui você também pode pedir ao serviço a thumbnail da música atual para carregar na UI
                    // Ex: mediaController.sendCustomCommand(new SessionCommand("GET_THUMBNAIL"), null);
                }

                // Os listeners devem ser adicionados sempre
                mediaController.addListener(new Player.Listener() {
                    // ... (seus callbacks de isPlayingChanged, onPlaybackStateChanged, onPlayerError) ...
                });

                iniciarSeekBar(); // Inicia a atualização da SeekBar

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Erro ao conectar ao serviço de mídia: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, MoreExecutors.directExecutor());

    }

    @Override
    protected void onStop() {
        super.onStop();
        pararSeekBar();
        // Não chame mediaController.release() se o serviço deve continuar tocando
        // mediaController.release(); // Remova esta linha se você quer que o serviço continue tocando
        // mediaController = null;
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

    // Receiver para receber o caminho da música e da imagem do serviço
    private class MyDownloadCompleteReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("ACTION_DOWNLOAD_COMPLETE".equals(action)) {
                String audioPath = intent.getStringExtra("audioPath");
                String imagePath = intent.getStringExtra("imagePath");

                runOnUiThread(() -> {
                    // (Opcional) Esconder o indicador de carregamento
                    if (imagePath != null && !imagePath.isEmpty()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                        background.setImageBitmap(bitmap);
                    } else {
                        // Lidar com o caso de não ter imagem ou caminho inválido
                        background.setImageResource(R.drawable.default_music_background); // Exemplo
                    }
                    // O MediaItem já deve ter sido setado e tocando pelo serviço
                    // Você não precisa mais setar o MediaItem aqui. A UI apenas se conecta ao controller.
                    atualizarBotaoPlayPause();
                    Toast.makeText(MusicActivity.this, "Download e reprodução iniciados!", Toast.LENGTH_SHORT).show();
                });
            } else if ("ACTION_DOWNLOAD_ERROR".equals(action)) {
                String errorMessage = intent.getStringExtra("errorMessage");
                runOnUiThread(() -> {
                    // (Opcional) Esconder o indicador de carregamento
                    Toast.makeText(MusicActivity.this, "Erro no download: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        }
    }

}
