package com.example.my_music_fy_oficial;

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

public class PlaybackService extends MediaSessionService {
    private static final SessionCommand CUSTOM_COMMAND_FAVORITES =
            new SessionCommand("ACTION_FAVORITES", Bundle.EMPTY);
    @Nullable private MediaSession mediaSession;

    @OptIn(markerClass = UnstableApi.class)
    public void onCreate() {
        super.onCreate();
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

