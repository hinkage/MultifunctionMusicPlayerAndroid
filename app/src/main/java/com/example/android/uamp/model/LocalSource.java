package com.example.android.uamp.model;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.support.v4.media.MediaMetadataCompat;

import com.example.android.uamp.R;
import com.example.android.uamp.utils.Global;
import com.example.android.uamp.utils.MediaIDHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

import static com.example.android.uamp.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE;

public class LocalSource implements MusicProviderSource {
    public static LinkedHashMap<String[], Bitmap> mMusicMap = new LinkedHashMap<>();
    public static Bitmap mBitmap = null;
    public static int mCoverFlowSize = 1;

    private static Cursor mCursor = Global.gContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            new String[] {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.SIZE,
                    MediaStore.Audio.Media.ALBUM},
            null, null, MediaStore.Audio.Media.DATE_ADDED);

    @Override
    public Iterator<MediaMetadataCompat> iterator() {

        ArrayList<MediaMetadataCompat> tracks = new ArrayList<>();
        int num = 0;
        //这里需要先移动到初始位置，否则第二次调用时就会什么都查不到
        mCursor.moveToFirst();
        while(mCursor.moveToNext()) {
            int duration = mCursor.getInt(mCursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            if (30000 > duration) {
                continue;
            }
            String title = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            String album = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            String artist = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            String genre = "本地音乐";
            String source = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            String iconUrl = null;
            int trackNumber = num;
            int totalTrackCount = num;
            //int duration = json.getInt(JSON_DURATION) * 1000; // ms
            String id = String.valueOf(source.hashCode());

            MediaMetadataCompat mediaMetadataCompat = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
                    .putString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE, source)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                    .putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, iconUrl)
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, null)// do not have lyric in local musics
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                    .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNumber)
                    .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, totalTrackCount)
                    .build();

            tracks.add(mediaMetadataCompat);

            String musicId = MediaIDHelper.createMediaID(
                    mediaMetadataCompat.getDescription().getMediaId(), MEDIA_ID_MUSICS_BY_GENRE, genre);
            String[] key = new String[]{null, null};
            key[0] = musicId;
            key[1] = title;
            mMusicMap.put(key, mBitmap);
            num++;
        }
        LocalSource.mCoverFlowSize = num;

        return tracks.iterator();
    }
}
