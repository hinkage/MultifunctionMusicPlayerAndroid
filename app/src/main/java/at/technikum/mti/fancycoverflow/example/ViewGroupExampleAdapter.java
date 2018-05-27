package at.technikum.mti.fancycoverflow.example;

import android.content.Context;
import android.graphics.Bitmap;

import android.support.v4.media.session.MediaControllerCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.android.uamp.model.LocalSource;
import com.example.android.uamp.model.RemoteJSONSource;
import com.example.android.uamp.ui.FullScreenPlayerActivity;
import com.example.android.uamp.utils.Global;

import java.util.Iterator;
import java.util.Map;

import at.technikum.mti.fancycoverflow.FancyCoverFlow;
import at.technikum.mti.fancycoverflow.FancyCoverFlowAdapter;

public class ViewGroupExampleAdapter extends FancyCoverFlowAdapter{
    @Override
    public View getCoverFlowItem(int position, View reusableView, ViewGroup parent) {
        CustomViewGroup customViewGroup = null;

        if (reusableView != null) {
            customViewGroup = (CustomViewGroup)reusableView;
        } else {
            customViewGroup = new CustomViewGroup(parent.getContext());
            customViewGroup.setLayoutParams(new FancyCoverFlow.LayoutParams(300, 600));
        }

        Object object = this.getItem(position);

        final Map.Entry<String[], Bitmap> entry = (Map.Entry<String[], Bitmap>) object;
        customViewGroup.getButton().setText(entry.getKey()[1]);
        customViewGroup.getButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Log.d("tag", "onClick: CoverFlow item");
                if (Global.gFullScreenActivity != null) {
                    Toast.makeText(Global.gFullScreenActivity, entry.getKey()[0], Toast.LENGTH_SHORT).show();
                    MediaControllerCompat.TransportControls controls = MediaControllerCompat.getMediaController(Global.gFullScreenActivity).getTransportControls();
                    controls.playFromMediaId(entry.getKey()[0], null);
                }
            }
        });
        customViewGroup.getImageView().setImageBitmap(entry.getValue());

        return customViewGroup;
    }

    @Override
    public int getCount() {
        if (Global.gIsRemote == true) {
            return RemoteJSONSource.mCoverFlowSize;
        } else {
            return LocalSource.mCoverFlowSize;
        }
    }

    @Override
    public Object getItem(int position) {
        if (Global.gIsRemote == true) {
            if (RemoteJSONSource.isIconDownloaded == true) {
                Iterator<Map.Entry<String[], Bitmap>> iterator = RemoteJSONSource.mMusicMap.entrySet().iterator();
                Map.Entry<String[], Bitmap> entry = null;
                for (int i = 0; i <= position; i++) {
                    if (iterator.hasNext()) {
                        entry = iterator.next();
                    }
                }
                return entry;
            } else {
                Iterator<Map.Entry<String[], Bitmap>> iterator = RemoteJSONSource.mMusicMap.entrySet().iterator();
                Map.Entry<String[], Bitmap> entry = null;
                for (int i = 0; i <= position; i++) {
                    if (iterator.hasNext()) {
                        entry = iterator.next();
                    }
                }
                return entry;
            }
        } else {
            Iterator<Map.Entry<String[], Bitmap>> iterator = LocalSource.mMusicMap.entrySet().iterator();
            Map.Entry<String[], Bitmap> entry = null;
            for (int i = 0; i <= position; i++) {
                if (iterator.hasNext()) {
                    entry = iterator.next();
                }
            }
            return entry;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private class CustomViewGroup extends LinearLayout{
        private ImageView imageView;
        private Button button;

        private CustomViewGroup(Context context) {
            super(context);

            this.setOrientation(VERTICAL);
            this.setWeightSum(5);

            this.imageView = new ImageView(context);
            this.button = new Button(context);

            LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            this.imageView.setLayoutParams(layoutParams);
            this.button.setLayoutParams(layoutParams);

            this.imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            this.imageView.setAdjustViewBounds(true);

            this.button.setText("CoverFlow");

            this.addView(this.imageView);
            this.addView(this.button);
        }

        private ImageView getImageView() {
            return imageView;
        }
        private Button getButton() {
            return this.button;
        }
    }
}
