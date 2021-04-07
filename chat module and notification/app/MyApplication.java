package com.ps.agrostand.app;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.ps.agrostand.session.SessionManager;

import java.util.ArrayList;

/*Created by Gourav on 12/08/19*/

public class MyApplication extends Application {
    public static final String TAG = MyApplication.class
            .getSimpleName();
    private static boolean activityVisible = false;
    private static MyApplication mInstance;
    private RequestQueue mRequestQueue;
    SessionManager sessionManager;
    public static ArrayList<String> listFriendID;

    public static synchronized MyApplication getInstance() {
        return mInstance;
    }

    public static boolean isActivityVisible() {
        return activityVisible;
    }

    public static void activityResumed() {
        activityVisible = true;
    }

    public static void activityPaused() {
        activityVisible = true;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sessionManager = new SessionManager(this);
        mInstance = this;
         listFriendID= new ArrayList<>();
//        changeApp();
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req, String tag) {
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        getRequestQueue().add(req);
    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }

    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(tag);
        }
    }

//    public void changeApp() {
//        if (sessionManager.isLanguage().equals("1")) {
//            Context context = LocaleHelper.setLocale(this, "vi");
//        } else if (sessionManager.isLanguage().equals("0")) {
//            Context context = LocaleHelper.setLocale(this, "en");
//        }
//    }
}
