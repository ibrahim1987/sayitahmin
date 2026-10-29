package com.hmmi.guessnumber;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.hmmi.login.ProfileActivity;
import com.hmmi.sto.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DuelProfileActivity extends AppCompatActivity {

    private TextView btnBack;
    private TextView tvProfileAvatar, tvProfileName, tvProfileUsername, tvProfileScore;
    private TextView tvStatWins, tvStatLosses, tvStatRatio;
    private TextView btnAccountSettings;
    private RecyclerView rvRecentScores;

    private DatabaseReference userRef, usersRef;
    private ValueEventListener profileListener;

    private String uid = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duel_profile);

        initViews();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Profil için giriş yapmalısın", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        uid = user.getUid();

        usersRef = FirebaseDatabase.getInstance().getReference("users");
        userRef = usersRef.child(uid);

        // Geri tuşu
        btnBack.setOnClickListener(v -> finish());

        // Hesap Bilgileri & Güvenlik butonuna tıklandığında yönlendirme (ProfilActivity'ye)
        btnAccountSettings.setOnClickListener(v -> {
            Intent intent = new Intent(DuelProfileActivity.this, ProfileActivity.class);
            // İsterseniz putExtra ile UID gönderebilirsiniz: intent.putExtra("uid", uid);
            startActivity(intent);
        });

        // RecyclerView ayarları (Geçmiş skorlar için)
        rvRecentScores.setLayoutManager(new LinearLayoutManager(this));
        // Not: Buraya kendi RecyclerView Adapter'ınızı bağlayabilirsiniz.

        loadProfile();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvProfileAvatar = findViewById(R.id.tvProfileAvatar);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileUsername = findViewById(R.id.tvProfileUsername);
        tvProfileScore = findViewById(R.id.tvProfileScore);

        tvStatWins = findViewById(R.id.tvStatWins);
        tvStatLosses = findViewById(R.id.tvStatLosses);
        tvStatRatio = findViewById(R.id.tvStatRatio);

        btnAccountSettings = findViewById(R.id.btnAccountSettings);
        rvRecentScores = findViewById(R.id.rvRecentScores);
    }

    private void loadProfile() {
        profileListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                String username = readString(s.child("username").getValue(), "");
                if (TextUtils.isEmpty(username)) {
                    username = readString(s.child("kullaniciadi").getValue(), "kullanici");
                }

                String name = readString(s.child("fullname").getValue(), "");
                if (TextUtils.isEmpty(name)) {
                    name = readString(s.child("fullName").getValue(), username);
                }

                long totalDuel = readLong(s.child("toplamDuelo").getValue(), 0);
                long winDuel = readLong(s.child("kazandigiDuelo").getValue(), 0);
                long loseDuel = totalDuel - winDuel;
                if (loseDuel < 0) loseDuel = 0;

                long duelPoint = readLong(s.child("duelPoint").getValue(), 0);

                int successRatio = totalDuel <= 0
                        ? 0
                        : (int) Math.round((winDuel * 100.0) / totalDuel);

                // Tasarımdaki alanların doldurulması
                tvProfileName.setText(name);
                tvProfileAvatar.setText(getInitial(name));
                tvProfileUsername.setText("@" + username);
                tvProfileScore.setText(duelPoint + " Puan");

                tvStatWins.setText(String.valueOf(winDuel));
                tvStatLosses.setText(String.valueOf(loseDuel));
                tvStatRatio.setText("%" + successRatio);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(
                        DuelProfileActivity.this,
                        "Profil yüklenemedi: " + error.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        };

        userRef.addValueEventListener(profileListener);
    }

    private String getInitial(String name) {
        if (TextUtils.isEmpty(name)) return "O";
        String cleaned = name.trim();
        if (cleaned.isEmpty()) return "O";
        return cleaned.substring(0, 1).toUpperCase(new Locale("tr", "TR"));
    }

    private String readString(Object value, String def) {
        if (value == null) return def;
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? def : s;
    }

    private long readLong(Object value, long def) {
        if (value == null) return def;
        try {
            if (value instanceof Long) return (Long) value;
            if (value instanceof Integer) return ((Integer) value).longValue();
            if (value instanceof Double) return ((Double) value).longValue();
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception e) {
            return def;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userRef != null && profileListener != null) {
            userRef.removeEventListener(profileListener);
        }
    }
}