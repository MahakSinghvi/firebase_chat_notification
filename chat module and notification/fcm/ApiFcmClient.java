package com.ps.agrostand.fcm;



import com.ps.agrostand.BuildConfig;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiFcmClient {
    public static NotificationApiService getApiService() {
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.FCM_BASE_URL)
                .client(provideClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NotificationApiService.class);
    }

    private static OkHttpClient provideClient() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return new OkHttpClient.Builder().addInterceptor(interceptor).addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                return chain.proceed(request);
            }
        }).build();
    }
}
