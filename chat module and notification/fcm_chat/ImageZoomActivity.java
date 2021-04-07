package com.ps.agrostand.fcm_chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.bumptech.glide.Glide;
import com.ps.agrostand.R;
import com.ps.agrostand.databinding.ImageZoomViewBinding;
import com.ps.agrostand.service.LocaleHelper;

public class ImageZoomActivity extends AppCompatActivity implements View.OnClickListener {
    private Bitmap bitmap;
    private String vImageUrl = "", vType = "";
    private LinearLayout cancelLL;
    private TouchImageView profileIV;
    private VideoView productVV;
    private ImageZoomViewBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.image_zoom_view);
        binding = DataBindingUtil.setContentView(this, R.layout.image_zoom_view);
        binding.cancelLL.setOnClickListener(this);
        initializeViews();
    }

    //    private void initializeViews() {
//        cancelLL = findViewById(R.id.cancelLL);
//        profileIV = findViewById(R.id.profileIV);
//        productVV = findViewById(R.id.productVV);
//        cancelLL.setOnClickListener(this);
//        try {
//            vImageUrl = getIntent().getStringExtra("IMAGE_URL");
//        } catch (Exception e) {
//
//        }
//        if (vImageUrl == null) {
//            vImageUrl = "";
//        }
//
//        try {
//            vType = getIntent().getStringExtra("TYPE");
//        } catch (Exception e) {
//
//        }
//        if (vType == null) {
//            vType = "";
//        }
//        if (vType.equalsIgnoreCase("IMAGE")) {
//            profileIV.setVisibility(View.VISIBLE);
//            productVV.setVisibility(View.GONE);
//            if (vImageUrl.trim().length() > 0) {
//                Glide.with(this).load(vImageUrl).into(profileIV);
//            } else {
//                Glide.with(this).load(R.drawable.product_profile).into(profileIV);
//            }
//        } else if (vType.equalsIgnoreCase("VIDEO")) {
//            profileIV.setVisibility(View.GONE);
//            productVV.setVisibility(View.VISIBLE);
//            productVV.setVideoPath(vImageUrl/*"http://videocdn.bodybuilding.com/video/mp4/62000/62792m.mp4"*/);
//            productVV.start();
//        }
//    }
    private void initializeViews() {
        String vProfileUrl = "";
        try {
            vProfileUrl = getIntent().getStringExtra("IMAGE_URL");
        } catch (Exception e) {

        }
        //   vProfileUrl = "https://firebasestorage.googleapis.com/v0/b/little-wish-3192a.appspot.com/o/profile_images%2F17f011b4-ef20-4fda-a148-111af540d7f8?alt=media&token=bcbcbe9f-aebc-49f9-bb49-e921d0696271";
        if (vProfileUrl == null) {
            vProfileUrl = "";
        }
        if (vProfileUrl.trim().length() > 0) {
            Glide.with(this).load(vProfileUrl).into(binding.profileIV);
        } else {
            Glide.with(this).load(R.mipmap.avatar).into(binding.profileIV);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancelLL:
                finish();
                break;
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }
}
