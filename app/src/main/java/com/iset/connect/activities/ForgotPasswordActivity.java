package com.iset.connect.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.iset.connect.R;
import com.iset.connect.network.ApiClient;
import com.iset.connect.network.ApiService;
import com.iset.connect.utils.SessionManager;

/** Password resets are handled by an administrator; no fake reset endpoint is used. */
public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String ADMIN_EMAIL = "admin@iset.tn";

    private SessionManager session;
    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        session = new SessionManager(this);
        api = ApiClient.getService(session);

        setSupportActionBar((androidx.appcompat.widget.Toolbar) findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Button btnEmailAdmin = findViewById(R.id.btn_email_admin);
        TextView tvBackLogin = findViewById(R.id.tv_back_login);
        btnEmailAdmin.setOnClickListener(v -> emailAdministrator());
        tvBackLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void emailAdministrator() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + ADMIN_EMAIL));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{ADMIN_EMAIL});
        intent.putExtra(Intent.EXTRA_SUBJECT, "ISET Connect - password reset request");
        intent.putExtra(Intent.EXTRA_TEXT, "Hello,\n\n"
                + "My account email address is: " + session.getEmail() + "\n"
                + "Please reset my password and let me know when it is done.\n\nThank you.");

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "No email application is installed.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
