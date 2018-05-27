package com.example.android.uamp.utils;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;

import com.example.android.uamp.MusicService;
import com.example.android.uamp.model.MusicProvider;

public class Global {
    public static Activity gFullScreenActivity = null;
    public static MusicService gMusicService = null;
    //MusicProvider 只在MusicService的onCreate中被创建了一次
    //public static MusicProvider gMusicProvider = null;
    public static boolean gIsRemote = false;
    public static Context gContext = null;
    public static MediaPlayer gMediaPlayer = null;
}
