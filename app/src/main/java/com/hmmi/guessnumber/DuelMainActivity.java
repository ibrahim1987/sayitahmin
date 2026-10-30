package com.hmmi.guessnumber;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.hmmi.login.AuthLoginActivity;
import com.hmmi.sto.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DuelMainActivity extends AppCompatActivity {

    private RecyclerView rvDuelPlayers;
    private ImageView btnBack; // Eğer tasarımda eklenirse kullanılabilir
    private LinearLayout btnProfile, bottomMyDuels, bottomHowToPlay, bottomWorkout;
    private TextView btnInviteFriend, btnTabGeneral, btnTabMonth;
    private EditText etSearchUser;

    private DuelRankAdapter adapter;

    private final List<DuelPlayerModel> playerList = new ArrayList<>();
    private final List<DuelPlayerModel> allPlayers = new ArrayList<>();

    private DatabaseReference usersRef, statsRef;

    private String myUid = "";
    private String myEmail = "";
    private String myName = "Oyuncu";
    private String myUsername = "";
    private String myDeviceId = "";
    private String myIp = "";

    private boolean monthMode = false;
    private final Locale trLocale = new Locale("tr", "TR");
    private TextWatcher searchWatcher;

    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private boolean openMyDuelsAfterPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duello_sahnesi);
        initNotificationPermissionLauncher();
        initViews();
        setupFirebaseUser();

        setupRecyclerView();
        setupButtonClicks();
        setupSearchAndTabs();

        if (!TextUtils.isEmpty(myUid)) {
            updateMyFcmToken();
        }

        loadUsersForDuel();
    }

    private boolean checkUserAuthentication() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Bu işlemi yapmak için giriş yapmalısınız!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, AuthLoginActivity.class);
            startActivity(intent);
            return false;
        }
        return true;
    }

    private void initNotificationPermissionLauncher() {
        notificationPermissionLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        isGranted -> {
                            updateMyFcmToken();
                            if (openMyDuelsAfterPermission) {
                                openMyDuelsAfterPermission = false;
                                openMyDuelsPage();
                            }
                        }
                );
    }

    private void initViews() {
        // XML Tasarımındaki güncel id'lerin eşleştirmesi
        btnProfile = findViewById(R.id.btnProfile);
        bottomMyDuels = findViewById(R.id.bottomMyDuels);
        bottomHowToPlay = findViewById(R.id.bottomHowToPlay);
        bottomWorkout = findViewById(R.id.bottomWorkout);
        btnInviteFriend = findViewById(R.id.btnInviteFriend);
        rvDuelPlayers = findViewById(R.id.rvDuelPlayers);
        etSearchUser = findViewById(R.id.etSearchUser);

        btnTabGeneral = findViewById(R.id.btnTabGeneral);
        btnTabMonth = findViewById(R.id.btnTabMonth);

        etSearchUser.setHint("Kullanıcı ara...");
    }

    private void setupFirebaseUser() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser != null) {
            myUid = firebaseUser.getUid();
            myEmail = firebaseUser.getEmail() == null ? "" : firebaseUser.getEmail().trim();
        }

        myDeviceId = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        if (myDeviceId == null) myDeviceId = "";

        usersRef = FirebaseDatabase.getInstance().getReference("users");
        statsRef = FirebaseDatabase.getInstance().getReference("duelStats");
    }

    private void setupRecyclerView() {
        rvDuelPlayers.setHasFixedSize(true);
        rvDuelPlayers.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DuelRankAdapter(playerList, myUid, myName);
        adapter.setMonthMode(monthMode);
        rvDuelPlayers.setAdapter(adapter);
    }

    private void setupButtonClicks() {
        btnProfile.setOnClickListener(v -> {
            if (!checkUserAuthentication()) return;
            startActivity(new Intent(this, DuelProfileActivity.class));
        });

        bottomMyDuels.setOnClickListener(v -> {
            if (!checkUserAuthentication()) return;
            openMyDuelsWithNotificationPermission();
        });
        bottomWorkout.setOnClickListener(v ->
            startActivity(new Intent(this, SingleGameActivity.class))
        );
        bottomHowToPlay.setOnClickListener(v ->
                startActivity(new Intent(this, DuelHowToPlayActivity.class))
        );

        btnInviteFriend.setOnClickListener(v -> {
            if (!checkUserAuthentication()) return;
            shareInviteMessage();
        });
    }

    private void openMyDuelsWithNotificationPermission() {
        if (!checkUserAuthentication()) return;
        updateMyFcmToken();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean granted =
                    ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED;

            if (!granted) {
                openMyDuelsAfterPermission = true;
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }

        openMyDuelsPage();
    }

    private void openMyDuelsPage() {
        if (!checkUserAuthentication()) return;
        startActivity(new Intent(this, MyDuelActivity.class));
    }

    private void setupSearchAndTabs() {
        setSelectedTab(false);

        searchWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applySearchFilter(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etSearchUser.addTextChangedListener(searchWatcher);

        btnTabGeneral.setOnClickListener(v -> {
            if (!monthMode) return;
            monthMode = false;
            setSelectedTab(false);

            if (adapter != null) {
                adapter.setMonthMode(false);
            }

            sortAllPlayers();
            applySearchFilter(getSearchText());
        });

        btnTabMonth.setOnClickListener(v -> {
            if (monthMode) return;
            monthMode = true;
            setSelectedTab(true);

            if (adapter != null) {
                adapter.setMonthMode(true);
            }

            sortAllPlayers();
            applySearchFilter(getSearchText());
        });
    }

    private void setSelectedTab(boolean monthSelected) {
        if (monthSelected) {
            btnTabMonth.setBackgroundResource(R.drawable.bg_button_primary_round);
            btnTabMonth.setTextColor(Color.WHITE);

            btnTabGeneral.setBackgroundResource(R.drawable.bg_icon_soft);
            btnTabGeneral.setTextColor(Color.parseColor("#60A5FA"));
        } else {
            btnTabGeneral.setBackgroundResource(R.drawable.bg_button_primary_round);
            btnTabGeneral.setTextColor(Color.WHITE);

            btnTabMonth.setBackgroundResource(R.drawable.bg_icon_soft);
            btnTabMonth.setTextColor(Color.parseColor("#60A5FA"));
        }
    }

    private String getSearchText() {
        return etSearchUser.getText() == null ? "" : etSearchUser.getText().toString();
    }

    private void applySearchFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(trLocale);
        playerList.clear();

        for (DuelPlayerModel p : allPlayers) {
            if (p == null) continue;

            long selectedPoint = monthMode ? p.monthlyDuelPoint : p.duelPoint;
            if (monthMode && selectedPoint <= 0) {
                continue;
            }

            String username = p.kullaniciadi == null ? "" : p.kullaniciadi.toLowerCase(trLocale);
            String name = p.fullname == null ? "" : p.fullname.toLowerCase(trLocale);
            String email = p.email == null ? "" : p.email.toLowerCase(Locale.US);

            if (!q.isEmpty()
                    && !username.contains(q)
                    && !name.contains(q)
                    && !email.contains(q)) {
                continue;
            }

            playerList.add(p);
        }

        for (int i = 0; i < playerList.size(); i++) {
            playerList.get(i).rankNo = i + 1;
        }

        if (adapter != null) {
            adapter.setMyName(getVisibleMyName());
            adapter.setMonthMode(monthMode);
            adapter.notifyDataSetChanged();
        }
    }

    private void shareInviteMessage() {
        if (!checkUserAuthentication()) return;
        String appLink = "https://play.google.com/store/apps/details?id=com.hmmi.sto";
        String visibleUsername = getVisibleMyUsername();

        String shareMessage =
                "🔥 Seni Sayı Düellosu'na davet ediyorum!\n\n" +
                        "Benim kullanıcı adım: " + visibleUsername + "\n\n" +
                        "Uygulamayı indir, kullanıcı adımı arat ve bana meydan oku! 🚀\n\n" +
                        "Hemen katıl 👇\n\n" +
                        appLink;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Sayı Düellosu Daveti");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);

        startActivity(Intent.createChooser(shareIntent, "Arkadaşına Davet Gönder"));
    }

    private String getVisibleMyUsername() {
        if (!TextUtils.isEmpty(myUsername)) return cleanUsername(myUsername);
        if (!TextUtils.isEmpty(myName)) return cleanUsername(myName);
        return "oyuncu";
    }

    private String getVisibleMyName() {
        if (!TextUtils.isEmpty(myUsername)) return "@" + cleanUsername(myUsername);
        if (!TextUtils.isEmpty(myName)) return myName;
        return "Oyuncu";
    }

    private String cleanUsername(String value) {
        if (value == null) return "oyuncu";
        String cleaned = value.trim();
        if (cleaned.startsWith("@")) {
            cleaned = cleaned.substring(1).trim();
        }
        if (TextUtils.isEmpty(cleaned)) cleaned = "oyuncu";
        return cleaned;
    }

    private void updateMyFcmToken() {
        if (TextUtils.isEmpty(myUid) || usersRef == null) return;

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (!TextUtils.isEmpty(token)) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("fcmToken", token);
                        map.put("fcmTokenUpdatedAt", ServerValue.TIMESTAMP);
                        if (!TextUtils.isEmpty(myDeviceId)) map.put("deviceId", myDeviceId);
                        usersRef.child(myUid).updateChildren(map);
                    }
                });
    }

    private void loadUsersForDuel() {
        if (usersRef == null) return;

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<DuelPlayerModel> tempList = new ArrayList<>();
                String currentMonthKey = getCurrentMonthKey();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    DuelPlayerModel user = ds.getValue(DuelPlayerModel.class);
                    if (user == null) continue;

                    user.uid = ds.getKey();
                    if (TextUtils.isEmpty(user.uid)) continue;

                    normalizePlayer(user, currentMonthKey);

                    if (isCurrentUser(ds, user)) {
                        if (!TextUtils.isEmpty(user.fullname)) myName = user.fullname.trim();
                        if (!TextUtils.isEmpty(user.kullaniciadi)) myUsername = cleanUsername(user.kullaniciadi);
                        if (!TextUtils.isEmpty(user.lastIp)) myIp = user.lastIp.trim();
                    }

                    tempList.add(user);
                }

                if (tempList.isEmpty()) {
                    allPlayers.clear();
                    playerList.clear();
                    if (adapter != null) adapter.notifyDataSetChanged();
                    return;
                }

                calculateWinPointsAndShowAll(tempList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DuelMainActivity.this, "Kullanıcılar yüklenemedi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void normalizePlayer(DuelPlayerModel user, String currentMonthKey) {
        if (TextUtils.isEmpty(user.fullname)) user.fullname = "Oyuncu";
        else user.fullname = user.fullname.trim();

        if (TextUtils.isEmpty(user.kullaniciadi)) user.kullaniciadi = user.username;
        if (TextUtils.isEmpty(user.kullaniciadi)) user.kullaniciadi = user.fullname;
        user.kullaniciadi = cleanUsername(user.kullaniciadi);

        if (user.duelPoint < 0) user.duelPoint = 0;
        if (user.monthlyDuelPoint < 0) user.monthlyDuelPoint = 0;
        if (user.toplamDuelo < 0) user.toplamDuelo = 0;
        if (user.kazandigiDuelo < 0) user.kazandigiDuelo = 0;

        if (!TextUtils.isEmpty(user.monthlyDuelMonthKey)
                && !currentMonthKey.equals(user.monthlyDuelMonthKey)) {
            user.monthlyDuelPoint = 0;
        }

        user.calculatedWinPoint = 1000;
    }

    private boolean isCurrentUser(DataSnapshot ds, DuelPlayerModel user) {
        if (TextUtils.isEmpty(myUid)) return false;
        if (myUid.equals(ds.getKey())) return true;
        if (!TextUtils.isEmpty(myEmail)) {
            String userEmail = user.email == null ? "" : user.email.trim();
            return myEmail.equalsIgnoreCase(userEmail);
        }
        return false;
    }

    private void calculateWinPointsAndShowAll(List<DuelPlayerModel> tempList) {
        if (tempList == null) return;
        if (statsRef == null || TextUtils.isEmpty(myUid)) {
            setAllPlayers(tempList);
            return;
        }

        statsRef.child(myUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot myStatsSnapshot) {
                for (DuelPlayerModel opponent : tempList) {
                    if (opponent == null || TextUtils.isEmpty(opponent.uid)) continue;

                    DataSnapshot historySnap = myStatsSnapshot.child(opponent.uid);
                    long countValue = readLong(historySnap.child("sameOpponentCount").getValue(), 0L);
                    long lastOpponentMatchAt = readLong(historySnap.child("lastOpponentMatchAt").getValue(), 0L);
                    long lastSameDeviceMatchAt = readLong(historySnap.child("lastSameDeviceMatchAt").getValue(), 0L);
                    long lastSameIpMatchAt = readLong(historySnap.child("lastSameIpMatchAt").getValue(), 0L);

                    boolean firstTimeOpponent = !historySnap.exists() || countValue <= 0 || lastOpponentMatchAt <= 1;
                    int sameOpponentCount = firstTimeOpponent ? 0 : (int) Math.max(1L, countValue);

                    boolean sameDevice = !TextUtils.isEmpty(myDeviceId) && !TextUtils.isEmpty(opponent.deviceId) && myDeviceId.equals(opponent.deviceId);
                    boolean sameIp = !TextUtils.isEmpty(myIp) && !TextUtils.isEmpty(opponent.lastIp) && myIp.equals(opponent.lastIp);

                    opponent.calculatedWinPoint = DuelPointCalculator.calculatePoint(
                            firstTimeOpponent, sameDevice, sameIp, sameOpponentCount,
                            daysSince(lastOpponentMatchAt), daysSince(lastSameDeviceMatchAt), daysSince(lastSameIpMatchAt)
                    );

                    if (opponent.calculatedWinPoint <= 0) {
                        opponent.calculatedWinPoint = 100;
                    }
                }
                setAllPlayers(tempList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setAllPlayers(tempList);
            }
        });
    }

    private void setAllPlayers(List<DuelPlayerModel> tempList) {
        allPlayers.clear();
        if (tempList != null) allPlayers.addAll(tempList);
        sortAllPlayers();
        applySearchFilter(getSearchText());
    }

    private void sortAllPlayers() {
        Collections.sort(allPlayers, (a, b) -> {
            long aPoint = monthMode ? Math.max(0L, a.monthlyDuelPoint) : Math.max(0L, a.duelPoint);
            long bPoint = monthMode ? Math.max(0L, b.monthlyDuelPoint) : Math.max(0L, b.duelPoint);

            int pointCompare = Long.compare(bPoint, aPoint);
            if (pointCompare != 0) return pointCompare;

            int winCompare = Long.compare(b.kazandigiDuelo, a.kazandigiDuelo);
            if (winCompare != 0) return winCompare;

            return Long.compare(b.updatedAt, a.updatedAt);
        });

        for (int i = 0; i < allPlayers.size(); i++) {
            allPlayers.get(i).rankNo = i + 1;
        }
    }

    private String getCurrentMonthKey() {
        return new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());
    }

    private long readLong(Object value, long def) {
        if (value == null) return def;
        try {
            if (value instanceof Long) return (Long) value;
            if (value instanceof Integer) return ((Integer) value).longValue();
            if (value instanceof Double) return ((Double) value).longValue();
            if (value instanceof Float) return ((Float) value).longValue();
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception e) {
            return def;
        }
    }

    private long daysSince(long timestamp) {
        if (timestamp <= 1) return 1;
        long diff = System.currentTimeMillis() - timestamp;
        if (diff <= 0) return 1;
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        return Math.max(1L, days);
    }

    @Override
    protected void onDestroy() {
        if (etSearchUser != null && searchWatcher != null) {
            etSearchUser.removeTextChangedListener(searchWatcher);
        }
        super.onDestroy();
    }
}