package com.iset.connect.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.iset.connect.R;
import com.iset.connect.activities.admin.AdminDashboardActivity;
import com.iset.connect.activities.student.StudentDashboardActivity;
import com.iset.connect.network.ApiClient;
import com.iset.connect.network.ApiService;
import com.iset.connect.utils.Constants;
import com.iset.connect.utils.SessionManager;

/** Brief brand screen that routes to the correct screen from the saved session. */
public class SplashActivity extends AppCompatActivity {

    private SessionManager session;
    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        session = new SessionManager(this);
        api = ApiClient.getService(session);

        new Handler(Looper.getMainLooper()).postDelayed(this::continueToApp, 1200);
    }

    private void continueToApp() {
        Class<?> target;
        String status = null;

        if (!session.isLoggedIn()) {
            target = LoginActivity.class;
        } else if (session.isAdmin()) {
            target = AdminDashboardActivity.class;
        } else if (session.isStudent()) {
            status = session.getStatus();
            if (Constants.STATUS_APPROVED.equals(status)) {
                target = StudentDashboardActivity.class;
            } else if (Constants.STATUS_PENDING.equals(status)) {
                target = WaitingApprovalActivity.class;
            } else {
                target = RejectedAccountActivity.class;
            }
        } else {
            session.logout();
            target = LoginActivity.class;
        }

        Intent intent = new Intent(this, target);
        if (status != null) {
            intent.putExtra(Constants.EXTRA_STATUS, status);
        }
        startActivity(intent);
        finish();
    }
}
