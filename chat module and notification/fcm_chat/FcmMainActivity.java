package com.ps.agrostand.fcm_chat;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.ps.agrostand.R;
import com.ps.agrostand.service.LocaleHelper;


public class FcmMainActivity extends AppCompatActivity {
    public static FCM_UserDTO fcmUserDTO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fcm_main);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }
}