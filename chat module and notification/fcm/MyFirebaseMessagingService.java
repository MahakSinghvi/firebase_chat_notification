package com.ps.agrostand.fcm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;


import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.ps.agrostand.MainActivity;
import com.ps.agrostand.R;
import com.ps.agrostand.activity.AgroLeadActivity;
import com.ps.agrostand.activity.AgrostandActivity;
import com.ps.agrostand.activity.GovSchemeDetailsActivity;
import com.ps.agrostand.activity.MoreGovSchemesActivity;
import com.ps.agrostand.activity.MoreLatestNewsActivity;
import com.ps.agrostand.activity.MyRequestActivity;
import com.ps.agrostand.activity.NewsDetailsActivity;
import com.ps.agrostand.activity.PostDetailActivity;
import com.ps.agrostand.common.AgrostandBaseActivity;
import com.ps.agrostand.retrofit.Constants;
import com.ps.agrostand.session.SessionManager;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONObject;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MessagingService";
    private SessionManager sessionManager;
    private Map<String, String> data;
    private boolean is_data_obj = false;
    String post_id = "", case_name = "", news_id = "", scheme_id = "";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        sessionManager = new SessionManager(this);
        // log the getting message from firebase
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        //  if remote message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            is_data_obj = true;
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            data = remoteMessage.getData();
            if (sessionManager.isLogin()) {
                handleNow(data);
            }
        }
        // if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            is_data_obj = false;
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            String user_type = "", notification_type = "";
            if (data != null) {
                if (data.containsKey("user_type")) {
                    user_type = data.get("user_type");
                }
                if (data.containsKey("notification_type")) {
                    notification_type = data.get("notification_type");
                }
            }
            if (!is_data_obj) {

//                String title = getString(R.string.app_name);
//                String detail = getString(R.string.app_name);
//                title = remoteMessage.getNotification().getTitle();
//                title = "" + Html.fromHtml((StringEscapeUtils.unescapeJava(title).replace("\n", "<br>")));
//                detail = remoteMessage.getNotification().getBody();
//                detail = "" + Html.fromHtml((StringEscapeUtils.unescapeJava(detail).replace("\n", "<br>")));
               /* if (sessionManager.isLogin()) {*/
                    sendNotification(remoteMessage.getNotification().getTitle(),
                            remoteMessage.getNotification().getBody(), notification_type, user_type, "");
               /* }*/
//                sendNotification(title,
//                        detail, notification_type, user_type, "");

            }
        }
    }


    private void storeRegIdInPref(String token) {
        SharedPreferences pref = getApplicationContext().getSharedPreferences(Constants.SHARED_PREF, 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("firebase_device_token", token);
        editor.commit();
    }

    /**
     * Persist token on third-party servers using your Retrofit APIs client.
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        // make a own server request here using your http client
    }

    JSONObject jsonObject;

    private void handleNow(Map<String, String> data) {
        try {

            Log.d(TAG, "handleNow: " + data.toString());
            if (data.containsKey("data")) {
                jsonObject = new JSONObject(data.get("data"));
            }
            Log.d(TAG, "handleNow:json " + jsonObject.toString());

            String title = getString(R.string.app_name);
            String detail = getString(R.string.app_name);

            if (jsonObject.has("post_id")) {
                post_id = jsonObject.getString("post_id");
            }

            if (jsonObject.has("case")) {
                case_name = jsonObject.getString("case");
            }
            if (jsonObject.has("news_id")) {
                news_id = jsonObject.getString("news_id");
            }
            if (jsonObject.has("scheme_id")) {
                scheme_id = jsonObject.getString("scheme_id");
            }

            if (data.containsKey("title")) {
                title = data.get("title");
//            title = StringEscapeUtils.unescapeJava(title);
//            title = title.replace("\n", "<br>");
            }
            if (data.containsKey("message")) {
                String user_type = "", notification_type = "";
                if (data.containsKey("user_type")) {
                    user_type = data.get("user_type");
                }
                if (data.containsKey("notification_type")) {
                    notification_type = data.get("notification_type");
                }

                if (data.containsKey("rfp_detail")) {
                    detail = data.get("rfp_detail");
                }
//            if (!MyApplication.isMessageVisible())
                /*if (sessionManager.isLogin()) {*/
                    sendNotification(title, data.get("message"), notification_type, user_type, detail);
                /*}*/
            }

        } catch (Exception e) {
            Log.d(TAG, "handleNow:Exception " + e);
        }

    }

    /**
     * Create and show notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private void sendNotification(String title, String messageBody, String notification_type, String userType, String detail) {
        Log.d(TAG, "\ttitle\t" + title
                + "\nmessageBody\t" + messageBody
                + "\nnotification_type\t" + notification_type
                + "\nuserType\t" + userType
                + "\ndetail\t" + detail
                + "\npost_id\t" + post_id
                + "\ncase_name\t" + case_name
                + "\nnews_id\t" + news_id
                + "\nscheme_id\t" + scheme_id
        );
        Intent intent = new Intent(this, MainActivity.class);

        //todo aagr post paar koi bhi action perform hua hai tho wo post detail pr jaayega
        if (case_name.equalsIgnoreCase("post_like") || case_name.equalsIgnoreCase("post_comment")) {
            intent = new Intent(this, PostDetailActivity.class);
            intent.putExtra("post_id", post_id);
        }
        //todo aagr koi bhi lead genrate hui hai tho wo lead ki list pr jyega
        if (case_name.equalsIgnoreCase("agro_lead")) {
            intent = new Intent(this, AgroLeadActivity.class);
        }
        //todo aagr ko sell se request aayi hai tho
        if (case_name.equalsIgnoreCase("sell_request")) {
            intent = new Intent(this, MyRequestActivity.class);
        }
      /*  //todo aagr koi Latest news aai
        if (case_name.equalsIgnoreCase("news")) {
            intent = new Intent(this, MoreLatestNewsActivity.class);
        }
        //todo aagr koi new scheme aai
        if (case_name.equalsIgnoreCase("scheme")) {
            intent = new Intent(this, MoreGovSchemesActivity.class);
        }*/

        //todo aagr koi Latest news aai
        if (case_name.equalsIgnoreCase("news")) {
            intent = new Intent(this, NewsDetailsActivity.class);
            intent.putExtra("news_id", news_id);
        }
        //todo aagr koi new scheme aai
        if (case_name.equalsIgnoreCase("scheme")) {
            intent = new Intent(this, GovSchemeDetailsActivity.class);
            intent.putExtra("scheme_id", scheme_id);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);


        //https://stackoverflow.com/questions/27373465/android-notification-add-typeface-for-title-and-content
        RemoteViews contentView = new RemoteViews(this
                .getApplicationContext().getPackageName(),
                R.layout.notification_large);
        contentView.setImageViewResource(R.id.notification_image,
                R.drawable.icon_app_logo);

        contentView.setTextViewText(R.id.notification_title1, title);
        contentView.setTextViewText(R.id.notification_message, messageBody);

        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.icon_app_logo)
                       /* .setContentTitle(title)
                        .setContentText(messageBody)*/
                        .setContent(contentView)
                        .setAutoCancel(true)
                        .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel human readable title
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Cloud Messaging Service",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.notify(0 /* ID ofok notification */, notificationBuilder.build());
    }
}