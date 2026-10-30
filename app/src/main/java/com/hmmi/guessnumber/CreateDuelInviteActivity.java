package com.hmmi.guessnumber;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.hmmi.sto.R;

import java.util.HashMap;
import java.util.Map;

public class CreateDuelInviteActivity extends AppCompatActivity {

    public static final String DUELS_NODE = "zekaduels";
    public static final String USERS_NODE = "users";

    private TextView btnBack, tvOpponentName, tvPoint, tvDigitType;
    private TextView btnDigit2, btnDigit3, btnDigit4, btnSendInvite, btnCancel;

    private String myUid = "";
    private String myName = "Oyuncu";
    private String opponentUid = "";
    private String opponentName = "Rakip Oyuncu";

    private int digitCount = 3;
    private long winPoint = 1000L;

    private DatabaseReference duelsRef;
    private DatabaseReference usersRef;

    private boolean sending = false;

    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_duel_invite);

        notificationPermissionLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        isGranted -> DuelFcmTokenManager.updateTokenForCurrentUser()
                );

        requestNotificationPermissionIfNeeded();

        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null || uid.trim().isEmpty()) {
            Toast.makeText(this, "Oturum bulunamadı", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        myUid = uid;

        opponentUid = safe(getIntent().getStringExtra("opponentUid"), "");
        opponentName = safe(getIntent().getStringExtra("opponentName"), "Rakip Oyuncu");
        myName = safe(getIntent().getStringExtra("myName"), "Oyuncu");

        winPoint = getIntent().getLongExtra("winPoint", 1000L);
        if (winPoint <= 0) winPoint = 1000L;

        digitCount = getIntent().getIntExtra("digitCount", 3);
        if (digitCount < 2 || digitCount > 4) digitCount = 3;

        if (opponentUid.isEmpty()) {
            Toast.makeText(this, "Rakip bilgisi bulunamadı", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        duelsRef = FirebaseDatabase.getInstance().getReference(DUELS_NODE);
        usersRef = FirebaseDatabase.getInstance().getReference(USERS_NODE);

        initViews();
        setupUI();
        setupClicks();
        loadMyName();

        DuelFcmTokenManager.updateTokenForCurrentUser();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvOpponentName = findViewById(R.id.tvOpponentName);
        tvPoint = findViewById(R.id.tvPoint);
        tvDigitType = findViewById(R.id.tvDigitType);

        btnDigit2 = findViewById(R.id.btnDigit2);
        btnDigit3 = findViewById(R.id.btnDigit3);
        btnDigit4 = findViewById(R.id.btnDigit4);

        btnSendInvite = findViewById(R.id.btnSendInvite);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void setupUI() {
        tvOpponentName.setText(opponentName);
        tvPoint.setText("Kazanırsan +" + winPoint + " puan");
        updateDigitUI();
    }

    private void setupClicks() {
        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());

        btnDigit2.setOnClickListener(v -> {
            digitCount = 2;
            updateDigitUI();
        });

        btnDigit3.setOnClickListener(v -> {
            digitCount = 3;
            updateDigitUI();
        });

        btnDigit4.setOnClickListener(v -> {
            digitCount = 4;
            updateDigitUI();
        });

        btnSendInvite.setOnClickListener(v -> checkAndCreateInvite());
    }

    private void updateDigitUI() {
        tvDigitType.setText(digitCount + " haneli sayı tahmin düellosu");

        setDigitSelected(btnDigit2, digitCount == 2);
        setDigitSelected(btnDigit3, digitCount == 3);
        setDigitSelected(btnDigit4, digitCount == 4);
    }

    private void setDigitSelected(TextView view, boolean selected) {
        if (view == null) return;

        view.setBackgroundResource(
                selected
                        ? R.drawable.bg_duel_button_primary
                        : R.drawable.bg_duel_button_outline
        );

        view.setTextColor(selected ? 0xFFFFFFFF : 0xFF58758F);
    }

    private void loadMyName() {
        usersRef.child(myUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String fullname = safe(snapshot.child("fullname").getValue(String.class), "");
                String kullaniciadi = safe(snapshot.child("kullaniciadi").getValue(String.class),
                        safe(snapshot.child("username").getValue(String.class), ""));
                String name = safe(snapshot.child("name").getValue(String.class), "");

                if (!fullname.isEmpty()) {
                    myName = fullname;
                } else if (!kullaniciadi.isEmpty()) {
                    myName = kullaniciadi;
                } else if (!name.isEmpty()) {
                    myName = name;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void checkAndCreateInvite() {
        if (sending) return;

        if (myUid.equals(opponentUid)) {
            Toast.makeText(this, "Kendine düello daveti gönderemezsin", Toast.LENGTH_SHORT).show();
            return;
        }

        sending = true;
        btnSendInvite.setEnabled(false);
        btnSendInvite.setText("Kontrol ediliyor...");

        duelsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String creatorUid = safe(ds.child("creatorUid").getValue(String.class), "");
                    String opponentUidDb = safe(ds.child("opponentUid").getValue(String.class), "");
                    String status = safe(ds.child("status").getValue(String.class), "");

                    boolean sameUsers =
                            (myUid.equals(creatorUid) && opponentUid.equals(opponentUidDb))
                                    || (myUid.equals(opponentUidDb) && opponentUid.equals(creatorUid));

                    boolean openDuel =
                            "DAVET".equals(status)
                                    || "AKTIF".equals(status);

                    if (sameUsers && openDuel) {
                        resetSendButton();
                        Toast.makeText(
                                CreateDuelInviteActivity.this,
                                "Bu oyuncuyla zaten bekleyen veya aktif bir düellon var.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }
                }

                createInvite();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                resetSendButton();
                Toast.makeText(
                        CreateDuelInviteActivity.this,
                        error.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void createInvite() {
        btnSendInvite.setText("Davet gönderiliyor...");

        DatabaseReference newRef = duelsRef.push();
        String duelId = newRef.getKey();

        if (duelId == null || duelId.trim().isEmpty()) {
            resetSendButton();
            Toast.makeText(this, "Düello oluşturulamadı", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> map = new HashMap<>();

        map.put("duelId", duelId);

        map.put("creatorUid", myUid);
        map.put("creatorName", myName);

        map.put("opponentUid", opponentUid);
        map.put("opponentName", opponentName);

        map.put("status", "DAVET");
        map.put("digitCount", digitCount);

        map.put("winPoint", winPoint);
        map.put("rewardPoint", winPoint);

        map.put("createdAt", ServerValue.TIMESTAMP);
        map.put("updatedAt", ServerValue.TIMESTAMP);

        map.put("creatorReady", true);
        map.put("opponentReady", false);

        map.put("turnUid", opponentUid);
        map.put("secretNumber", "");

        newRef.setValue(map)
                .addOnSuccessListener(unused -> {
                    Intent i = new Intent(CreateDuelInviteActivity.this, DuelGameActivity.class);

                    i.putExtra("duelId", duelId);
                    i.putExtra("digitCount", digitCount);
                    i.putExtra("myUid", myUid);
                    i.putExtra("opponentUid", opponentUid);
                    i.putExtra("winPoint", winPoint);

                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e -> {
                    resetSendButton();
                    Toast.makeText(
                            CreateDuelInviteActivity.this,
                            safe(e == null ? "" : e.getMessage(), "Davet gönderilemedi"),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void resetSendButton() {
        sending = false;

        if (btnSendInvite != null) {
            btnSendInvite.setEnabled(true);
            btnSendInvite.setText("Davet Gönder");
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {

            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private String safe(String value, String def) {
        if (value == null || value.trim().isEmpty()) return def;
        return value.trim();
    }
}