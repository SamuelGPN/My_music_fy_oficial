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

import android.net.Uri; // Importe Uri
import android.widget.Toast;

import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.media3.common.Player;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import com.chaquo.python.PyObject;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.File;
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

                // >>> INÍCIO: Executar a lógica do ChaquoPy em uma Thread de background <<<
                // Crie um ExecutorService (pode ser um pool de threads único para tarefas sequenciais)
                ExecutorService executor = Executors.newSingleThreadExecutor();

                executor.execute(() -> { // Isso será executado na thread de background
                    String caminhoMusicaLocal = null; // Para armazenar o resultado do Python
                    try {
                        // (Opcional) Mostrar um indicador de carregamento na UI
                        runOnUiThread(() -> {
                            // Suponha que você tenha um ProgressBar com ID R.id.progressBarLoading
                            // ProgressBar loadingSpinner = findViewById(R.id.progressBarLoading);
                            // if (loadingSpinner != null) loadingSpinner.setVisibility(View.VISIBLE);
                            // E talvez desabilitar botões para evitar cliques enquanto carrega
                            // playPauseBtn.setEnabled(false);
                            // seekBar.setEnabled(false);
                        });

                        // --- Lógica de criação de pasta e limpeza ---
                        File pasta = new File(getFilesDir(), "musics_temp");
                        System.out.println("Verificando se há pasta music_temp - Python");
                        if (!pasta.exists()) {
                            System.out.println("Não há pasta music_temp - Python");
                            pasta.mkdirs();
                            System.out.println("Criou pasta music_temp - Python");
                        }

                        if (pasta.exists() && pasta.isDirectory()) {
                            File[] arquivos = pasta.listFiles();
                            if (arquivos != null) {
                                for (File arquivo : arquivos) {
                                    arquivo.delete();
                                }
                            }
                        }

                        String tituloWebm = titulo + ".wav";
                        File arq_final = new File(pasta, tituloWebm);
                        String caminho_arq_final = arq_final.getAbsolutePath();

                        System.out.println("Entrando no laço - Python");

                        String respostaPython = "";
                        // A condição de parada do loop 'shouldStopWaiting' é crucial aqui,
                        // mas para um loop de "esperar o resultado", o ideal é que a função Python
                        // já retorne o resultado final, ou que essa loop seja interna ao Python.
                        // Se for absolutamente necessário um loop de polling, você precisa de um flag:
                        // private volatile boolean shouldStopWaiting = false; (declarado na classe)
                        // E setar 'shouldStopWaiting = true;' em onStop()

                        // Loop de espera pelo resultado do Python. Idealmente, o Python deveria ser
                        // bloqueante ou fornecer um callback. Se a chamada Python já é bloqueante
                        // (espera o download/processamento), o while é redundante aqui.
                        // Se callModeloFromModel2 iniciar algo assíncrono no Python e retornar
                        // um status, então um loop de polling faz sentido, mas com cuidado.
                        // Para este cenário, vamos assumir que callModeloFromModel2 faz o trabalho completo.
                        PyObject result = PythonModelHolder.callModeloFromModel2(url, caminho_arq_final, pasta.getAbsolutePath());
                        respostaPython = result.toString(); // Assumindo que retorna o caminho final

                        // Se o retorno do Python for o caminho da música baixada
                        caminhoMusicaLocal = caminho_arq_final;


                    } catch (Exception e) {
                        e.printStackTrace();
                        final String errorMessage = e.getMessage();
                        runOnUiThread(() -> Toast.makeText(MusicActivity.this, "Erro no processamento Python: " + errorMessage, Toast.LENGTH_LONG).show());
                    } finally {
                        // Sempre volte para a UI Thread para atualizar a interface com o resultado
                        String finalCaminhoMusicaLocal = caminhoMusicaLocal;
                        runOnUiThread(() -> {
                            // (Opcional) Esconder o indicador de carregamento
                            // ProgressBar loadingSpinner = findViewById(R.id.progressBarLoading);
                            // if (loadingSpinner != null) loadingSpinner.setVisibility(View.GONE);
                            // playPauseBtn.setEnabled(true);
                            // seekBar.setEnabled(true);

                            // --- CONFIGURAÇÃO DO MEDIA3 PLAYER COM O CAMINHO LOCAL ---
                            if (finalCaminhoMusicaLocal != null && !finalCaminhoMusicaLocal.isEmpty()) {
                                File musicaFile = new File(finalCaminhoMusicaLocal);

                                if (musicaFile.exists() && musicaFile.isFile()) {
                                    Uri mediaUri = Uri.fromFile(musicaFile);
                                    androidx.media3.common.MediaItem mediaItem = androidx.media3.common.MediaItem.fromUri(mediaUri);
                                    mediaController.setMediaItem(mediaItem);
                                    mediaController.prepare();
                                    mediaController.play();
                                } else {
                                    Toast.makeText(MusicActivity.this, "Erro: Arquivo de música não encontrado após download ou inválido.", Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(MusicActivity.this, "Erro: O processamento da música falhou ou retornou caminho vazio.", Toast.LENGTH_LONG).show();
                            }
                            // O restante das atualizações de UI que dependem do mediaController
                            atualizarBotaoPlayPause();
                        });
                    }
                });
                // <<< FIM: Executar a lógica do ChaquoPy em uma Thread de background >>>


                // Estes listeners e a inicialização da seekbar podem ficar aqui fora
                // pois eles são para o mediaController, não para a lógica do python.
                // O mediaController só vai começar a tocar quando o MediaItem for setado
                // na runOnUiThread() acima.
                mediaController.addListener(new Player.Listener() {
                    @Override
                    public void onIsPlayingChanged(boolean isPlaying) {
                        atualizarBotaoPlayPause();
                    }

                    @Override
                    public void onPlaybackStateChanged(int playbackState) {
                        atualizarBotaoPlayPause();
                        // Lógica de ProgressBar pode ir aqui também para buffering
                    }
                    @Override
                    public void onPlayerError(androidx.media3.common.PlaybackException error) {
                        Player.Listener.super.onPlayerError(error);
                        runOnUiThread(() -> {
                            Toast.makeText(MusicActivity.this, "Erro de reprodução: " + error.getMessage(), Toast.LENGTH_LONG).show();
                            error.printStackTrace();
                        });
                    }
                });

                iniciarSeekBar();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(MusicActivity.this, "Erro ao conectar ao serviço de mídia: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
