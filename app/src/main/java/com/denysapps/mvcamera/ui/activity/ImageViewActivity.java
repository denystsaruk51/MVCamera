package com.denysapps.mvcamera.ui.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.denysapps.mvcamera.R;
import com.denysapps.mvcamera.utility.ResourceUtil;
import com.jsibbold.zoomage.ZoomageView;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ImageViewActivity extends BaseActivity {
    public final static String PARAM_URL = "url";

    @BindView(R.id.imgView)
    ZoomageView imgView;

    private String mUrl;

    public static void createImageViewActivity(Context context, String url) {
        Intent intent = new Intent(context, ImageViewActivity.class);
        intent.putExtra(PARAM_URL, url);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        ButterKnife.bind(this);

        mUrl = getIntent().getStringExtra(PARAM_URL);
        loadImage();
    }
    @Override
    public void onBackPressed(){
        onBack();
    }
    @OnClick(R.id.btn_back)
    public void onBack(){
        finish();
    }

    /*
     delete image file on disk
    */

    @OnClick(R.id.btn_remove)
    public void onRemove(){
        final Dialog warDialog = new Dialog(this);
        warDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        warDialog.setContentView(R.layout.dialog_delete_layout);
        warDialog.getWindow().getDecorView().setBackgroundResource(android.R.color.transparent);
        warDialog.setCanceledOnTouchOutside(false);
        TextView left = warDialog.findViewById(R.id.btn_left);
        TextView right = warDialog.findViewById(R.id.btn_right);
        left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                warDialog.dismiss();

                File file = new File(mUrl);
                if(file.exists()) {
                    file.delete();
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" +  mUrl)));
                }
                onBack();
            }
        });
        right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                warDialog.dismiss();
            }
        });

        warDialog.show();
    }
    /*
     load image from image file path
    */
    private void loadImage() {
        Bitmap bmp = ResourceUtil.decodeSampledBitmapFromFile(mUrl, 1500);
        if (bmp != null) {
            imgView.setImageBitmap(bmp);
        }
    }
}
