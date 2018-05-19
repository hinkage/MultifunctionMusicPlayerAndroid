package at.technikum.mti.fancycoverflow.example;

import android.content.Context;
import android.graphics.Bitmap;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.android.uamp.model.LocalSource;
import com.example.android.uamp.model.RemoteJSONSource;
import com.example.android.uamp.utils.Global;

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

        customViewGroup.getImageView().setImageBitmap((Bitmap) this.getItem(position));

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
                return RemoteJSONSource.mBitmapList.get(position);
            } else {
                return LocalSource.mBitmap;
            }
        } else {
            return LocalSource.mBitmap;
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
            this.button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("tag", "onClick: CoverFlow item");
                }
            });

            this.addView(this.imageView);
            this.addView(this.button);
        }

        private ImageView getImageView() {
            return imageView;
        }
    }
}
