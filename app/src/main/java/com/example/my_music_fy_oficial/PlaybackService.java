package com.example.my_music_fy_oficial;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.CommandButton;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionResult;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.chaquo.python.PyObject; // Importe o ChaquoPy
import com.chaquo.python.Python; // Importe o ChaquoPy
import android.util.Log; // Para logs

public class PlaybackService extends MediaSessionService {
    private static final SessionCommand CUSTOM_COMMAND_FAVORITES =
            new SessionCommand("ACTION_FAVORITES", Bundle.EMPTY);
    @Nullable private MediaSession mediaSession;
    private ExecutorService downloadExecutor; // Adicione este campo

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onCreate() {
        super.onCreate();
        // Inicialize o ExecutorService aqui
        downloadExecutor = Executors.newSingleThreadExecutor(); // Para um download por vez
        // ou Executors.newFixedThreadPool(NUM_THREADS) para múltiplos downloads
        // ... (resto do seu onCreate) ...

        CommandButton favoriteButton =
                new CommandButton.Builder(CommandButton.ICON_HEART_UNFILLED)
                        .setDisplayName("Save to favorites")
                        .setSessionCommand(CUSTOM_COMMAND_FAVORITES)
                        .build();
        Player player = new ExoPlayer.Builder(this).build();
        // Build the session with a custom layout.
        mediaSession =
                new MediaSession.Builder(this, player)
                        .setCallback(new MyCallback(player))
                        .setMediaButtonPreferences(ImmutableList.of(favoriteButton))
                        .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaSession != null) {
            mediaSession.getPlayer().release();
            mediaSession.release();
            mediaSession = null;
        }
        if (downloadExecutor != null && !downloadExecutor.isShutdown()) {
            downloadExecutor.shutdownNow(); // Desligue o executor
        }
    }

    // Método para iniciar o download e reprodução no serviço
    public void startMusicDownloadAndPlay(String videoUrl) {
        downloadExecutor.execute(() -> {
            String caminhoMusicaLocal = null;
            String backgroundCaminho = null;
            try {
                // ** Lógica de limpeza de pasta temporária (se ainda necessária) **
                // Nota: O yt-dlp pode ter seu próprio sistema de arquivos temporários,
                // então essa limpeza pode ser mais fácil de gerenciar lá,
                // ou você pode ter uma pasta dedicada para os downloads finalizados.
                // Para este exemplo, manterei a lógica de limpeza aqui.
                File pastaDeDestino = new File(getFilesDir(), "musics_temp");
                if (!pastaDeDestino.exists()) pastaDeDestino.mkdirs();
                if (pastaDeDestino.exists() && pastaDeDestino.isDirectory()) {
                    for (File arquivo : pastaDeDestino.listFiles()) {
                        arquivo.delete(); // Limpa arquivos antigos
                    }
                }

                // CHAMADA AO CÓDIGO PYTHON DO CHACOPY NO SERVIÇO
                // Certifique-se de que Python está inicializado antes de chamar
                // if (! Python.is     Initialized()) {
                //     Python.start(new AndroidPlatform(this));
                // }
                PyObject result = PythonModelHolder.callModeloFromModel2(videoUrl, pastaDeDestino.getAbsolutePath());
                caminhoMusicaLocal = result.asList().get(0).toString();
                backgroundCaminho = result.asList().get(1).toString();

                if (caminhoMusicaLocal != null && !caminhoMusicaLocal.isEmpty()) {
                    File musicaFile = new File(caminhoMusicaLocal);
                    if (musicaFile.exists() && musicaFile.isFile()) {
                        Uri mediaUri = Uri.fromFile(musicaFile);
                        androidx.media3.common.MediaItem mediaItem = androidx.media3.common.MediaItem.fromUri(mediaUri);

                        // Atualize o player na thread principal do serviço (se necessário, o ExoPlayer geralmente é thread-safe para setMediaItem/prepare/play)
                        // ou garanta que o player está sendo usado na thread correta.
                        // Para Media3, setMediaItem e play podem ser chamados de threads de fundo.
                        Player player = mediaSession.getPlayer();
                        player.setMediaItem(mediaItem);
                        player.prepare();
                        player.play();

                        // Você pode enviar um broadcast local ou usar um liveData para notificar a Activity da imagem
                        // ou apenas retornar o caminho da imagem e a Activity busca/exibe.
                        Log.d("PlaybackService", "Música carregada no player: " + caminhoMusicaLocal);

                        // Envie a imagem de volta para a Activity (ex: via um BroadcastReceiver local)
                        // ou use um LiveData/ViewModel compartilhado
                        Intent broadcastIntent = new Intent("ACTION_DOWNLOAD_COMPLETE");
                        broadcastIntent.putExtra("audioPath", caminhoMusicaLocal);
                        broadcastIntent.putExtra("imagePath", backgroundCaminho);
                        // Se você usa uma URL para a imagem, pode enviar a URL também
                        // broadcastIntent.putExtra("imageUrl", imageUrl);
                        sendBroadcast(broadcastIntent);

                    } else {
                        Log.e("PlaybackService", "Erro: Arquivo de música não encontrado ou inválido após download.");
                    }
                } else {
                    Log.e("PlaybackService", "Erro: Caminho vazio do Python após download.");
                }

            } catch (Exception e) {
                Log.e("PlaybackService", "Erro no processamento Python no serviço: " + e.getMessage(), e);
                // Envie um broadcast de erro para a Activity, se necessário
                Intent errorIntent = new Intent("ACTION_DOWNLOAD_ERROR");
                errorIntent.putExtra("errorMessage", e.getMessage());
                sendBroadcast(errorIntent);
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "ACTION_START_DOWNLOAD_AND_PLAY".equals(intent.getAction())) {
            String videoUrl = intent.getStringExtra("VIDEO_URL");
            if (videoUrl != null) {
                startMusicDownloadAndPlay(videoUrl);
            }
        }
        return super.onStartCommand(intent, flags, startId); // IMPORTANTE: Chamar o super para lidar com o MediaSessionService
    }

    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        // This example always accepts the connection request
        return mediaSession;
    }
    private static class MyCallback implements MediaSession.Callback {


        private final Player player;

        @OptIn(markerClass = UnstableApi.class)
        @Override
        public MediaSession.ConnectionResult onConnect(
                MediaSession session, MediaSession.ControllerInfo controller) {
            // Set available player and session commands.
            return new MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(
                            MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                                    .add(CUSTOM_COMMAND_FAVORITES)
                                    .build())
                    .build();
        }
        public MyCallback(Player player) {
            this.player = player;
        }
        public void onPlay() {
            player.play();
        }
        public void onPause() {
            player.pause();
        }
        public void onSkipToPrevious() {
            player.seekToPrevious();
        }
        public void onSkipToNext() {
            player.seekToNext();
        }
        public void onSeekTo(long pos) {
            player.seekTo(pos);
        }

        @Override
        public ListenableFuture onCustomCommand(
                MediaSession session,
                MediaSession.ControllerInfo controller,
                SessionCommand customCommand,
                Bundle args) {
            if (customCommand.customAction.equals(CUSTOM_COMMAND_FAVORITES.customAction)) {
                // Do custom logic here
                saveToFavorites(session.getPlayer().getCurrentMediaItem());
                return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
            }
            return MediaSession.Callback.super.onCustomCommand(
                    session, controller, customCommand, args);
        }

        private void saveToFavorites(MediaItem currentMediaItem) {
        }
    }


}

