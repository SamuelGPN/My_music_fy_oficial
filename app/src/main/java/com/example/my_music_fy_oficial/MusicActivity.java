package com.example.my_music_fy_oficial;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


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

    private volatile boolean shouldStopWaiting = false;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        shouldStopWaiting = true;
        executor.shutdownNow();  // interrompe a thread, acordando-a se estiver dormindo e forca o fechamento de todas as tarefas pendentes

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

        botaoPausar.setEnabled(false);
        if (!Objects.equals(url, url_ant)) {
            System.out.println("Entrou no if - Python");
            if (player != null) {
                player.release();
            }
            tituloMusic.setText("Carregando...");

            // Caminho para /data/data/seu.app.package/files/musics_temp/
            File pasta = new File(getFilesDir(), "musics_temp");
            System.out.println("Verificando se há pasta music_temp - Python");
            if (!pasta.exists()) {
                System.out.println("Não há pasta music_temp - Python");
                pasta.mkdirs(); // Cria a pasta (e qualquer pai necessário)
                System.out.println("Criou pasta music_temp - Python");
            }

            //Para deletar todos os arquivos dessa pasta se houver
            if (pasta.exists() && pasta.isDirectory()) {
                File[] arquivos = pasta.listFiles();
                if (arquivos != null) {
                    for (File arquivo : arquivos) {
                        arquivo.delete(); // Deleta cada arquivo
                    }
                }
            }

            String tituloWebm = titulo + ".wav";
            File arq_final = new File(pasta, tituloWebm);
            String caminho_arq_final = arq_final.getAbsolutePath();

            String tituloWebm_formatado = titulo + "_format.webm";
            String caminho_arq_final_sem_ruido = new File(pasta, tituloWebm_formatado).getAbsolutePath();
            System.out.println("Entrando no laço - Python");

            //shouldStopWaiting = false;
            // Executa o Python em uma thread separada


            executor.execute(() -> {
                System.out.println("Entrou na thread - Python");
                String resposta = "";
                while (resposta.isEmpty() && !shouldStopWaiting) { //!shouldStopWaiting para que se eu sair desse activity ele não continuar com esse loop
                    System.out.println("Entrou no laço - Python");
                    PyObject result = PythonModelHolder.callModeloFromModel2(url, caminho_arq_final, pasta.getAbsolutePath());
                    resposta = result.toString();
                    if (resposta.isEmpty()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                //String ffmpegCommand = "-i " + caminho_arq_final + " " + caminho_arq_final_sem_ruido + ".wav";

                //FFmpegKit.executeAsync(ffmpegCommand, session -> {
                //    ReturnCode returnCode = session.getReturnCode();

                //    if (returnCode.isSuccess()) {
                //        System.out.println("Conversão realizada com sucesso!");
                //    } else {
                //        System.err.println("Erro na conversão: " + returnCode);
                //    }
                // });
                // Quando a resposta estiver pronta, atualiza a UI com o player
                mainHandler.post(() -> {
                    try {
                        player = new MediaPlayer();
                        player.setDataSource(caminho_arq_final);
                        player.prepareAsync();
                        player.setOnPreparedListener(mp -> {
                            mp.start();
                            tituloMusic.setText(titulo);
                            botaoPausar.setEnabled(true);
                            mostrarNotificacao();
                        });
                    } catch (IOException e) {
                        Log.e("PythonError", "Erro ao preparar o player", e);
                    }
                });
            });
        } else {
            botaoPausar.setEnabled(true);
            botaoPausar.setImageResource(player.isPlaying() ? R.drawable.pause_icon : R.drawable.play_icon);
            tituloMusic.setText(titulo);
        }
        botaoPausar.setOnClickListener(w -> {
            if (player != null) {
                if (player.isPlaying()) {
                    player.pause();
                    botaoPausar.setImageResource(R.drawable.play_icon);
                } else {
                    player.start();
                    botaoPausar.setImageResource(R.drawable.pause_icon);
                }
                mostrarNotificacao();
            }
        });
        botaoHome.setOnClickListener(v -> finish());
    }
}
