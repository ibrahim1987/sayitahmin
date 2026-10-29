package com.hmmi.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.hmmi.guessnumber.DuelMainActivity;
import com.hmmi.sto.R;

public class AuthLoginActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private Button btnLoginEmail;
    private TextView btnSignUp, btnForgotPassword;

    private FirebaseAuth auth;
    private String redirectPage;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_login_new);

        getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        initFirebase();
        initViews();
        setupActions();
        checkAlreadyLoggedIn();
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        redirectPage = getIntent().getStringExtra("redirectPage");
    }

    private void initViews() {
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLoginEmail = findViewById(R.id.btnLoginEmail);
        btnSignUp = findViewById(R.id.btnSignUp);

    }

    private void setupActions() {
        btnLoginEmail.setOnClickListener(v -> {
            animateClick(v);
            loginWithEmail();
        });

        btnSignUp.setOnClickListener(v -> {
            animateClick(v);
            openRegisterScreen();
        });


    }

    private void loginWithEmail() {
        if (isLoading) return;

        String email = getEmail();
        String password = getPassword();

        edtEmail.setError(null);
        edtPassword.setError(null);

        if (!validateEmail(email)) return;

        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Şifrenizi giriniz");
            edtPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            edtPassword.setError("Şifre en az 6 karakter olmalı");
            edtPassword.requestFocus();
            return;
        }

        setLoading(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, this::handleLoginResult);
    }

    private void handleLoginResult(@NonNull Task<AuthResult> task) {
        setLoading(false);

        if (task.isSuccessful()) {
            Toast.makeText(this, "Giriş başarılı", Toast.LENGTH_SHORT).show();
            openRedirectTarget();
            return;
        }

        String errorMessage = "Giriş başarısız";

        if (task.getException() != null && task.getException().getMessage() != null) {
            errorMessage = "Giriş başarısız: " + task.getException().getMessage();
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void resetPassword() {
        if (isLoading) return;

        String email = getEmail();
        edtEmail.setError(null);

        if (!validateEmail(email)) return;

        setLoading(true);

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    setLoading(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(
                                this,
                                "Şifre sıfırlama bağlantısı e-posta adresine gönderildi",
                                Toast.LENGTH_LONG
                        ).show();
                    } else {
                        String message = "Şifre sıfırlama işlemi başarısız";

                        if (task.getException() != null && task.getException().getMessage() != null) {
                            message = task.getException().getMessage();
                        }

                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("E-posta adresini giriniz");
            edtEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Geçerli bir e-posta adresi giriniz");
            edtEmail.requestFocus();
            return false;
        }

        return true;
    }

    private String getEmail() {
        return edtEmail.getText() == null ? "" : edtEmail.getText().toString().trim();
    }

    private String getPassword() {
        return edtPassword.getText() == null ? "" : edtPassword.getText().toString().trim();
    }

    private void checkAlreadyLoggedIn() {
        if (auth.getCurrentUser() != null) {
            openRedirectTarget();
        }
    }

    private void openRedirectTarget() {
        Intent intent = null;

        if (!TextUtils.isEmpty(redirectPage)) {
            try {
                Class<?> target = Class.forName(redirectPage);

                if (AppCompatActivity.class.isAssignableFrom(target)) {
                    intent = new Intent(this, target);
                }

            } catch (Exception ignored) {
            }
        }

        if (intent == null) {
            intent = new Intent(this, DuelMainActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        finish();
    }

    private void openRegisterScreen() {
        Intent intent = new Intent(this, RegisterActivity.class);

        if (!TextUtils.isEmpty(redirectPage)) {
            intent.putExtra("redirectPage", redirectPage);
        }

        startActivity(intent);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    private void setLoading(boolean loading)
    {
        isLoading = loading;

        btnLoginEmail.setEnabled(!loading);
        btnSignUp.setEnabled(!loading);
        btnForgotPassword.setEnabled(!loading);
        edtEmail.setEnabled(!loading);
        edtPassword.setEnabled(!loading);

        btnLoginEmail.setText(loading ? "Giriş yapılıyor..." : "Giriş Yap");
    }

    private void animateClick(View view) {
        AlphaAnimation animation = new AlphaAnimation(0.65f, 1.0f);
        animation.setDuration(130);
        view.startAnimation(animation);
    }
}