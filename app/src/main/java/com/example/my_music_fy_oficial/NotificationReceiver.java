package com.example.my_music_fy_oficial;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String acao = intent.getAction();

        if ("PAUSE_PLAY".equals(acao)) {
            if (MainActivity.player != null) {
                if (MainActivity.player.isPlaying()) {
                    MainActivity.player.pause();
                } else {
                    MainActivity.player.start();
                }
            }
        }
    }
}
