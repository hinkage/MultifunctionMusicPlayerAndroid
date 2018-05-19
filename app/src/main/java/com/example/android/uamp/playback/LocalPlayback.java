package com.example.android.uamp.playback;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;

import com.example.android.uamp.MusicService;
import com.example.android.uamp.model.MusicProvider;
import com.example.android.uamp.model.MusicProviderSource;
import com.example.android.uamp.utils.Global;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.MediaIDHelper;

import java.io.IOException;

import static android.support.v4.media.session.MediaSessionCompat.QueueItem;

/**
 * Interface representing either Local or Remote Playback. The {@link MusicService} works
 * directly with an instance of the Playback object to make the various calls such as
 * play, pause etc.
 */
public class LocalPlayback implements Playback{
    private enum MediaPlayerState {
        STATE_NONE, STATE_PREPARED, STATE_BUFFERING, STATE_PAUSED, STATE_PLAYING, STATE_END
    }

    @Override
    public void switchSource(MusicProvider provider) {
        this.mMusicProvider = provider;
    }

    private static final String TAG = LogHelper.makeLogTag(LocalPlayback.class);
    // 需要对Callback接口里的两个方法进行调用，来传递播放器的播放状态，在此写一个method来调用它们
    private void onPlayerStateChanged(MediaPlayerState newState) {
        this.mMediaPlayerState = newState;
        switch (this.mMediaPlayerState) {
            case STATE_PAUSED:
            case STATE_BUFFERING:
            case STATE_PREPARED:
            case STATE_PLAYING:
                if (this.mCallback != null) {
                    this.mCallback.onPlaybackStatusChanged(this.getState());
                }
                break;
            case STATE_END:
                if (this.mCallback != null) {
                    this.mCallback.onCompletion();
                }
                break;
        }
    }

    private boolean playWhenReady = false;
    private final IntentFilter mAudioNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private final BroadcastReceiver mAudioNoisyReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                        LogHelper.d(TAG, "Headphones disconnected.");
                        if (isPlaying()) {
                            Intent i = new Intent(context, MusicService.class);
                            i.setAction(MusicService.ACTION_CMD);
                            i.putExtra(MusicService.CMD_NAME, MusicService.CMD_PAUSE);
                            mContext.startService(i);
                        }
                    }
                }
            };

    private final AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    LogHelper.d(TAG, "onAudioFocusChange. focusChange=", focusChange);
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_GAIN:
                            mCurrentAudioFocusState = AUDIO_FOCUSED;
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            // Audio focus was lost, but it's possible to duck (i.e.: play quietly)
                            mCurrentAudioFocusState = AUDIO_NO_FOCUS_CAN_DUCK;
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            // Lost audio focus, but will gain it back (shortly), so note whether
                            // playback should resume
                            mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
                            mPlayOnFocusGain = mMediaPlayer != null && mMediaPlayerState == MediaPlayerState.STATE_PLAYING;
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS:
                            // Lost audio focus, probably "permanently"
                            mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
                            break;
                    }

                    if (mMediaPlayer != null) {
                        // Update the player state based on the change
                        configurePlayerState();
                    }
                }
            };
    // The volume we set the media player to when we lose audio focus, but are
    // allowed to reduce the volume instead of stopping playback.
    public static final float VOLUME_DUCK = 0.2f;
    // The volume we set the media player when we have audio focus.
    public static final float VOLUME_NORMAL = 1.0f;

    // we don't have audio focus, and can't duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    // we don't have focus, but can duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    // we have full audio focus
    private static final int AUDIO_FOCUSED = 2;

    private Callback mCallback;
    private final Context mContext;
    private WifiManager.WifiLock mWifiLock;
    private MusicProvider mMusicProvider;
    private boolean mAudioNoisyReceiverRegistered;
    private String mCurrentMediaId;

    private final AudioManager mAudioManager;

    private int mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
    private boolean mPlayOnFocusGain;
    private boolean mMediaPlayerNullIsStopped = false;
    private MediaPlayerState mMediaPlayerState = MediaPlayerState.STATE_NONE;


    @Override
    public void setCallback(Playback.Callback callback) {
        this.mCallback = callback;
    }

    public LocalPlayback(Context context, MusicProvider musicProvider) {
        this.mMediaPlayer = new MediaPlayer();
        Global.gMediaPlayer = this.mMediaPlayer;

        Context applicationContext = context.getApplicationContext();
        this.mContext = applicationContext;
        this.mMusicProvider = musicProvider;

        this.mAudioManager =
                (AudioManager) applicationContext.getSystemService(Context.AUDIO_SERVICE);
        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        this.mWifiLock =
                ((WifiManager) applicationContext.getSystemService(Context.WIFI_SERVICE))
                        .createWifiLock(WifiManager.WIFI_MODE_FULL, "uAmp_lock");
    }

    private MediaPlayer mMediaPlayer;

    /**
     * Start/setup the playback.
     * Resources/listeners would be allocated by implementations.
     */
    @Override
    public void start() {
        // Nothing to do
    }

    /**
     * Stop the playback. All resources can be de-allocated by implementations here.
     * @param notifyListeners if true and a callback has been set by setCallback,
     *                        callback.onPlaybackStatusChanged will be called after changing
     *                        the state.
     */
    @Override
    public void stop(boolean notifyListeners) {
        giveUpAudioFocus();
        unregisterAudioNoisyReceiver();
        releaseResources(true);
    }

    /**
     * Set the latest playback state as determined by the caller.
     */
    @Override
    public void setState(int state) {
        // Nothing to do (mExoPlayer holds its own state).
    }

    /**
     * Get the current {@link android.media.session.PlaybackState#getState()}
     */
    @Override
    public int getState() {
        if (this.mMediaPlayer == null) {
            return this.mMediaPlayerNullIsStopped
                    ? PlaybackStateCompat.STATE_STOPPED
                    : PlaybackStateCompat.STATE_NONE;
        }
        switch (this.mMediaPlayerState) {
            case STATE_PAUSED:
                return PlaybackStateCompat.STATE_PAUSED;
            case STATE_BUFFERING:
                return PlaybackStateCompat.STATE_BUFFERING;
            case STATE_PREPARED:
                return PlaybackStateCompat.STATE_PAUSED;
            case STATE_PLAYING:
                return PlaybackStateCompat.STATE_PLAYING;
            default:
                return PlaybackStateCompat.STATE_NONE;
        }
    }

    /**
     * @return boolean that indicates that this is ready to be used.
     */
    @Override
    public boolean isConnected() {
        //java的int型enum竟然不能相互比较
        if (this.mMediaPlayerState == MediaPlayerState.STATE_NONE) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * @return boolean indicating whether the player is playing or is supposed to be
     * playing when we gain audio focus.
     */
    @Override
    public boolean isPlaying() {
        return this.mPlayOnFocusGain ||
                (mMediaPlayer != null && this.mMediaPlayerState == MediaPlayerState.STATE_PLAYING);
    }

    /**
     * @return pos if currently playing an item
     */
    @Override
    public long getCurrentStreamPosition() {
        return this.mMediaPlayer != null ? this.mMediaPlayer.getCurrentPosition() : 0;
    }

    /**
     * Queries the underlying stream and update the internal last known stream position.
     */
    @Override
    public void updateLastKnownStreamPosition() {
        // Nothing to do. Position maintained by ExoPlayer.
    }

    @Override
    public void play(QueueItem item) {
        this.mPlayOnFocusGain = true;
        tryToGetAudioFocus();
        registerAudioNoisyReceiver();
        String mediaId = item.getDescription().getMediaId();
        boolean mediaHasChanged = !TextUtils.equals(mediaId, mCurrentMediaId);
        if (mediaHasChanged) {
            mCurrentMediaId = mediaId;
        }

        if (mediaHasChanged || mMediaPlayer == null) {
            releaseResources(false);
            MediaMetadataCompat track =
                    mMusicProvider.getMusic(
                            MediaIDHelper.extractMusicIDFromMediaID(
                                    item.getDescription().getMediaId()
                            )
                    );

            String source = track.getString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE);
            if (source != null) {
                if (Global.gIsRemote) {//对本地路径做此处理会导致音乐无法播放
                    source = source.replaceAll(" ", "%20"); // Escape spaces for URLs
                }
            }

            if (mMediaPlayer == null) {
                mMediaPlayer = new MediaPlayer();
                Global.gMediaPlayer = this.mMediaPlayer;
            }

            //String mediaSource = this.mMusicProvider.getMusic(this.mCurrentMediaId).getString("source");
            //这里的id是个hash值，而非字符串
            mMediaPlayer.reset();
            try {
                mMediaPlayer.setDataSource(source);
                mMediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mMediaPlayer.seekTo(0);
            mMediaPlayer.start();


            mWifiLock.acquire();
        }

        configurePlayerState();
    }

    @Override
    public void pause() {
        if (mMediaPlayer != null) {
            playWhenReady = false;
            this.mMediaPlayer.pause();
            this.onPlayerStateChanged(MediaPlayerState.STATE_PAUSED);
        }
        releaseResources(false);
        unregisterAudioNoisyReceiver();
    }

    @Override
    public void seekTo(long position) {
        LogHelper.d(TAG, "seekTo called with ", position);
        if (mMediaPlayer != null) {
            registerAudioNoisyReceiver();
            mMediaPlayer.seekTo((int)position);
        }
    }

    @Override
    public void setCurrentMediaId(String mediaId) {
        this.mCurrentMediaId = mediaId;
    }

    @Override
    public String getCurrentMediaId() {
        return this.mCurrentMediaId;
    }

    private void tryToGetAudioFocus() {
        LogHelper.d(TAG, "tryToGetAudioFocus");
        int result =
                mAudioManager.requestAudioFocus(
                        mOnAudioFocusChangeListener,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mCurrentAudioFocusState = AUDIO_FOCUSED;
        } else {
            mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
        }
    }

    private void giveUpAudioFocus() {
        LogHelper.d(TAG, "giveUpAudioFocus");
        if (mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener)
                == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
        }
    }

    private void configurePlayerState() {
        LogHelper.d(TAG, "configurePlayerState. mCurrentAudioFocusState=", mCurrentAudioFocusState);
        if (mCurrentAudioFocusState == AUDIO_NO_FOCUS_NO_DUCK) {
            // We don't have audio focus and can't duck, so we have to pause
            pause();
        } else {
            registerAudioNoisyReceiver();

            if (mCurrentAudioFocusState == AUDIO_NO_FOCUS_CAN_DUCK) {
                // We're permitted to play, but only if we 'duck', ie: play softly
                //mExoPlayer.setVolume(VOLUME_DUCK);
            } else {
                //mExoPlayer.setVolume(VOLUME_NORMAL);
            }

            // If we were playing when we lost focus, we need to resume playing.
            if (mPlayOnFocusGain) {
                //mMediaPlayerState = MediaPlayerState.STATE_PLAYING;
                this.onPlayerStateChanged(MediaPlayerState.STATE_PLAYING);
                mPlayOnFocusGain = false;
            }
        }
    }

    private void registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            mContext.registerReceiver(mAudioNoisyReceiver, mAudioNoisyIntentFilter);
            mAudioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            mContext.unregisterReceiver(mAudioNoisyReceiver);
            mAudioNoisyReceiverRegistered = false;
        }
    }

    private void releaseResources(boolean releasePlayer) {
        LogHelper.d(TAG, "releaseResources. releasePlayer=", releasePlayer);

        // Stops and releases player (if requested and available).
        if (releasePlayer && mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
            mMediaPlayerNullIsStopped = true;
            mPlayOnFocusGain = false;
        }

        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }
    }
}
