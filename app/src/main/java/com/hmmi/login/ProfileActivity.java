package com.hmmi.login;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.hmmi.sto.R;
public class ProfileActivity extends AppCompatActivity {

    private EditText edtFullName, edtPassword, edtPasswordAgain;
    private TextView tvUsername, tvEmail, btnDeleteAccount;
    private Button btnUpdateName, btnUpdatePassword;
    private ImageView btnBack;

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_2026);

        getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        initViews();
        initFirebase();
        loadUserData();
        setupActions();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);

        edtFullName = findViewById(R.id.edtFullName);
        edtPassword = findViewById(R.id.edtPassword);
        edtPasswordAgain = findViewById(R.id.edtPasswordAgain);

        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);

        btnUpdateName = findViewById(R.id.btnUpdateName);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.getUid());
        }
    }

    private void loadUserData() {
        if (currentUser == null || userRef == null) {
            Toast.makeText(this, "Oturum bulunamadı", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvEmail.setText(currentUser.getEmail() != null
                ? currentUser.getEmail()
                : "Mail bulunamadı");

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String fullname = snapshot.child("fullname").getValue(String.class);

                String username = snapshot.child("username").getValue(String.class);
                if (TextUtils.isEmpty(username)) {
                    username = snapshot.child("kullaniciadi").getValue(String.class);
                }

                edtFullName.setText(!TextUtils.isEmpty(fullname) ? fullname : "");

                tvUsername.setText(!TextUtils.isEmpty(username)
                        ? username
                        : "Kullanıcı adı bulunamadı");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this,
                        "Bilgiler yüklenemedi: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupActions() {
        btnBack.setOnClickListener(v -> finish());
        btnUpdateName.setOnClickListener(v -> updateFullName());
        btnUpdatePassword.setOnClickListener(v -> updatePassword());
        btnDeleteAccount.setOnClickListener(v -> showDeletePasswordDialog());
    }

    private void updateFullName() {
        if (currentUser == null || userRef == null) return;

        String fullName = edtFullName.getText().toString().trim();

        if (TextUtils.isEmpty(fullName) || fullName.length() < 3) {
            edtFullName.setError("Ad Soyad en az 3 karakter olmalı");
            edtFullName.requestFocus();
            return;
        }

        setNameLoading(true);

        userRef.child("fullname").setValue(fullName)
                .addOnSuccessListener(unused -> {
                    setNameLoading(false);
                    Toast.makeText(this, "Ad Soyad güncellendi", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    setNameLoading(false);
                    Toast.makeText(this,
                            "Güncelleme hatası: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void updatePassword() {
        if (currentUser == null) return;

        String password = edtPassword.getText().toString().trim();
        String passwordAgain = edtPasswordAgain.getText().toString().trim();

        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Yeni şifre giriniz");
            edtPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            edtPassword.setError("Şifre en az 6 karakter olmalı");
            edtPassword.requestFocus();
            return;
        }

        if (!password.equals(passwordAgain)) {
            edtPasswordAgain.setError("Şifreler eşleşmiyor");
            edtPasswordAgain.requestFocus();
            return;
        }

        setPasswordLoading(true);

        currentUser.updatePassword(password)
                .addOnSuccessListener(unused -> {
                    setPasswordLoading(false);
                    edtPassword.setText("");
                    edtPasswordAgain.setText("");
                    Toast.makeText(this, "Şifre güncellendi", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    setPasswordLoading(false);
                    Toast.makeText(this,
                            "Şifre güncellenemedi. Tekrar giriş yapmanız gerekebilir.",
                            Toast.LENGTH_LONG).show();
                });
    }

    private void showDeletePasswordDialog() {
        if (currentUser == null || TextUtils.isEmpty(currentUser.getEmail())) {
            Toast.makeText(this, "E-posta bilgisi bulunamadı", Toast.LENGTH_SHORT).show();
            return;
        }

        final EditText inputPassword = new EditText(this);
        inputPassword.setHint("Mevcut şifrenizi girin");
        inputPassword.setSingleLine(true);
        inputPassword.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        inputPassword.setPadding(40, 25, 40, 25);

        new AlertDialog.Builder(this)
                .setTitle("Hesabı Kalıcı Sil")
                .setMessage("Bu işlem geri alınamaz. Devam etmek için mevcut şifrenizi girin.")
                .setView(inputPassword)
                .setPositiveButton("Hesabı Sil", (dialog, which) -> {
                    String password = inputPassword.getText().toString().trim();

                    if (TextUtils.isEmpty(password)) {
                        Toast.makeText(this, "Şifre girilmedi", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    showFinalDeleteConfirm(password);
                })
                .setNegativeButton("Vazgeç", null)
                .show();
    }

    private void showFinalDeleteConfirm(String password) {
        new AlertDialog.Builder(this)
                .setTitle("Son Onay")
                .setMessage("Profil bilgileriniz ve hesabınız kalıcı olarak silinecek.")
                .setPositiveButton("Evet, Sil", (dialog, which) -> deleteAccountWithReAuth(password))
                .setNegativeButton("Vazgeç", null)
                .show();
    }

    private void deleteAccountWithReAuth(String password) {
        if (auth == null || currentUser == null || userRef == null) return;

        String email = currentUser.getEmail();
        String uid = currentUser.getUid();

        if (TextUtils.isEmpty(email))
        {
            Toast.makeText(this, "E-posta bulunamadı", Toast.LENGTH_SHORT).show();
            return;
        }

        setDeleteLoading(true);

        AuthCredential credential = EmailAuthProvider.getCredential(email, password);

        currentUser.reauthenticate(credential)
                .addOnSuccessListener(unused -> {
                    DatabaseReference db = FirebaseDatabase.getInstance().getReference();

                    db.child("users").child(uid).removeValue()
                            .addOnSuccessListener(unused2 -> {
                                currentUser.delete()
                                        .addOnSuccessListener(unused3 -> {
                                            setDeleteLoading(false);
                                            auth.signOut();
                                            Toast.makeText(this,
                                                    "Hesap tamamen silindi",
                                                    Toast.LENGTH_LONG).show();
                                            finishAffinity();
                                        })
                                        .addOnFailureListener(e -> {
                                            setDeleteLoading(false);
                                            Toast.makeText(this,
                                                    " Hesap silinemedi: " + e.getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                setDeleteLoading(false);
                                Toast.makeText(this,
                                        "Profil verileri silinemedi: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setDeleteLoading(false);
                    Toast.makeText(this,
                            "Şifre hatalı veya tekrar giriş gerekli",
                            Toast.LENGTH_LONG).show();
                });
    }

    private void setNameLoading(boolean loading) {
        btnUpdateName.setEnabled(!loading);
        btnUpdateName.setText(loading ? "Güncelleniyor..." : "Ad Soyadı Güncelle");
    }

    private void setPasswordLoading(boolean loading) {
        btnUpdatePassword.setEnabled(!loading);
        btnUpdatePassword.setText(loading ? "Güncelleniyor..." : "Şifreyi Güncelle");
    }

    private void setDeleteLoading(boolean loading) {
        btnDeleteAccount.setEnabled(!loading);
        btnDeleteAccount.setText(loading ? "Siliniyor..." : "Hesabımı Sil");
    }
}