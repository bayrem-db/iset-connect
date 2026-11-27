package com.iset.connect.network;

import androidx.annotation.NonNull;

import com.iset.connect.utils.SessionManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/** Attaches the bearer token from SharedPreferences to every API request. */
public class AuthInterceptor implements Interceptor {
    private final SessionManager session;

    public AuthInterceptor(SessionManager session) {
        this.session = session;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        String token = session.getToken();
        Request request = original;
        if (token != null && !token.isEmpty()) {
            request = original.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .method(original.method(), original.body())
                    .build();
        }
        return chain.proceed(request);
    }
}
