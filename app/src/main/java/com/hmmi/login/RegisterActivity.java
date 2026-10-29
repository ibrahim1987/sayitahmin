package com.hmmi.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.messaging.FirebaseMessaging;
import com.hmmi.guessnumber.DuelMainActivity;
import com.hmmi.sto.R;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtFullName, edtUsername, edtEmail, edtPassword, edtRepeatPassword;
    private Button btnRegister;
    private TextView btnBackToLogin;

    private FirebaseAuth auth;
    private boolean isLoading = false;
    private String redirectPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        redirectPage = getIntent().getStringExtra("redirectPage");

        initViews();
        initFirebase();
        setupActions();
    }

    private void initViews() {
        edtFullName = findViewById(R.id.edtFullName);
        edtUsername = findViewById(R.id.edtUsername);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtRepeatPassword = findViewById(R.id.edtRepeatPassword);

        btnRegister = findViewById(R.id.btnLoginEmail);
        btnBackToLogin = findViewById(R.id.btnSignInBack);
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
    }

    private void setupActions() {
        btnBackToLogin.setOnClickListener(v -> {
            animateClick(v);
            finish();
        });

        btnRegister.setOnClickListener(v -> {
            animateClick(v);
            startRegisterFlow();
        });
    }

    private void startRegisterFlow() {
        if (isLoading) return;

        String fullName = edtFullName.getText().toString().trim();
        String usernameRaw = edtUsername.getText().toString().trim();
        String username = normalizeUsername(usernameRaw);
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String repeatPassword = edtRepeatPassword.getText().toString().trim();

        if (!validateInputs(fullName, usernameRaw, username, email, password, repeatPassword)) return;

        checkUsernameAndRegister(fullName, username, email, password);
    }

    private boolean validateInputs(
            String fullName,
            String usernameRaw,
            String username,
            String email,
            String password,
            String repeatPassword
    ) {
        clearErrors();

        if (TextUtils.isEmpty(fullName) || fullName.length() < 3) {
            edtFullName.setError("Ad soyad giriniz");
            edtFullName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(usernameRaw)) {
            edtUsername.setError("Kullanıcı adı giriniz");
            edtUsername.requestFocus();
            return false;
        }

        if (username.length() < 4) {
            edtUsername.setError("Kullanıcı adı en az 4 karakter olmalı");
            edtUsername.requestFocus();
            return false;
        }

        if (!username.matches("^[a-z0-9_]+$")) {
            edtUsername.setError("Sadece küçük harf, rakam ve _ kullanın");
            edtUsername.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Geçerli e-posta giriniz");
            edtEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            edtPassword.setError("Şifre en az 6 karakter olmalı");
            edtPassword.requestFocus();
            return false;
        }

        if (!password.equals(repeatPassword)) {
            edtRepeatPassword.setError("Şifreler uyuşmuyor");
            edtRepeatPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void checkUsernameAndRegister(String fullName, String username, String email, String password) {
        setLoading(true);

        FirebaseDatabase.getInstance().getReference("usernames").child(username).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        setLoading(false);
                        Toast.makeText(this, "Kullanıcı adı kontrol edilemedi", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DataSnapshot snapshot = task.getResult();

                    if (snapshot != null && snapshot.exists()) {
                        setLoading(false);
                        edtUsername.setError("Bu kullanıcı adı zaten alınmış");
                        edtUsername.requestFocus();
                        return;
                    }

                    createFirebaseUser(fullName, username, email, password);
                });
    }

    private void createFirebaseUser(String fullName, String username, String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        setLoading(false);

                        String message = "Hesap oluşturulamadı";
                        if (task.getException() != null && task.getException().getMessage() != null) {
                            message = task.getException().getMessage();
                        }

                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseMessaging.getInstance().getToken()
                            .addOnCompleteListener(tokenTask -> {
                                String fcmToken = "";

                                if (tokenTask.isSuccessful() && tokenTask.getResult() != null) {
                                    fcmToken = tokenTask.getResult();
                                }

                                saveUserToRealtime(fullName, username, email, fcmToken);
                            });
                });
    }

    private void saveUserToRealtime(String fullName, String username, String email, String fcmToken) {
        if (auth.getCurrentUser() == null) {
            setLoading(false);
            Toast.makeText(this, "Oturum bilgisi alınamadı", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> user = new HashMap<>();
        user.put("uid", uid);
        user.put("fullname", fullName);
        user.put("username", username);
        user.put("email", email);
        user.put("fcmToken", fcmToken);

        user.put("puan", 0);
        user.put("duelPoint", 0);
        user.put("monthlyDuelPoint", 0);
        user.put("monthlyDuelMonthKey", "");
        user.put("toplamDuelo", 0);
        user.put("kazandigiDuelo", 0);

        user.put("ortalama", 0.00);
        user.put("city", "");
        user.put("district", "");
        user.put("updatedAt", ServerValue.TIMESTAMP);
        user.put("createdAt", ServerValue.TIMESTAMP);

        Map<String, Object> usernameData = new HashMap<>();
        usernameData.put("uid", uid);
        usernameData.put("username", username);
        usernameData.put("email", email);
        usernameData.put("fcmToken", fcmToken);
        usernameData.put("createdAt", ServerValue.TIMESTAMP);

        Map<String, Object> updates = new HashMap<>();
        updates.put("/users/" + uid, user);
        updates.put("/usernames/" + username, usernameData);

        FirebaseDatabase.getInstance().getReference()
                .updateChildren(updates)
                .addOnCompleteListener(task -> {
                    setLoading(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Hesap oluşturuldu", Toast.LENGTH_SHORT).show();
                        openRedirectTarget();
                    } else {
                        Toast.makeText(this, "Profil kaydedilemedi", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String normalizeUsername(String value) {
        if (value == null) return "";

        String username = value.trim()
                .toLowerCase(new Locale("tr", "TR"))
                .replace("ı", "i")
                .replace("ğ", "g")
                .replace("ü", "u")
                .replace("ş", "s")
                .replace("ö", "o")
                .replace("ç", "c");

        username = username.replaceAll("\\s+", "_");
        username = username.replaceAll("[^a-z0-9_]", "");

        return username;
    }

    private void clearErrors() {
        edtFullName.setError(null);
        edtUsername.setError(null);
        edtEmail.setError(null);
        edtPassword.setError(null);
        edtRepeatPassword.setError(null);
    }

    private void setLoading(boolean loading) {
        isLoading = loading;

        edtFullName.setEnabled(!loading);
        edtUsername.setEnabled(!loading);
        edtEmail.setEnabled(!loading);
        edtPassword.setEnabled(!loading);
        edtRepeatPassword.setEnabled(!loading);
        btnBackToLogin.setEnabled(!loading);
        btnRegister.setEnabled(!loading);

        btnRegister.setText(loading ? "Kontrol ediliyor..." : "Hesap Oluştur");
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

    private void animateClick(android.view.View view) {
        AlphaAnimation animation = new AlphaAnimation(0.65f, 1.0f);
        animation.setDuration(130);
        view.startAnimation(animation);
    }
}