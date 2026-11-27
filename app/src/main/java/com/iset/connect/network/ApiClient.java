package com.iset.connect.network;

import com.google.gson.GsonBuilder;
import com.iset.connect.utils.Constants;
import com.iset.connect.utils.SessionManager;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Singleton Retrofit client. A single BASE_URL comes from BuildConfig. */
public final class ApiClient {
    private static ApiService service;
    private static String currentBaseUrl = "";

    private ApiClient() { }

    public static synchronized ApiService getService(SessionManager session) {
        String baseUrl = session.getBaseUrl();
        if (service == null || !baseUrl.equals(currentBaseUrl)) {
            currentBaseUrl = baseUrl;
            // BASIC level never logs request/response bodies or credentials.
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(new AuthInterceptor(session))
                    .addInterceptor(logging)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().setLenient().create()))
                    .build();
            service = retrofit.create(ApiService.class);
        }
        return service;
    }
}
