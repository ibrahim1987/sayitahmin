package com.hmmi.guessnumber;

import android.app.AlertDialog;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.hmmi.sto.R;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class DuelGameActivity extends AppCompatActivity {

    public static final String DUELS_NODE = "zekaduels";
    public static final String USERS_NODE = "users";

    private TextView btnBack, btnLeaveDuel;
    private TextView tvMyName, tvMyAvatar, tvOpponentName, tvOpponentAvatar;
    private TextView tvTurnStatus, tvGuessCount, btnTahminEt;
    private EditText etGuessInput;
    private RecyclerView rvHistory;
    private AdView adView;

    private FirebaseAuth auth;
    private DatabaseReference rootRef;
    private DatabaseReference duelRef;
    private ValueEventListener duelListener;
    private ValueEventListener guessesListener;

    private String duelId = "";
    private String myUid = "";
    private String creatorUid = "";
    private String opponentUid = "";
    private String creatorName = "Oyuncu";
    private String opponentName = "Rakip";
    private String turnUid = "";
    private String secretNumber = "";
    private String status = "";
    private String myDeviceId = "";
    private String winnerUid = "";
    private String winnerName = "";
    private String leftUid = "";

    private int winPoint = 1000;
    private int digitCount = 3;

    private boolean isCreator = false;
    private boolean isMyTurn = false;
    private boolean isFinished = false;
    private boolean creatingSecret = false;
    private boolean sendingGuess = false;

    private final List<DuelGuessModel> guessList = new ArrayList<>();
    private final List<DuelGuessModel> realGuessList = new ArrayList<>();
    private DuelGuessAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duel_game);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Oturum bulunamadı", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        myUid = auth.getCurrentUser().getUid();

        duelId = safeString(getIntent().getStringExtra("duelId"));
        if (duelId.isEmpty()) {
            Toast.makeText(this, "Düello bilgisi alınamadı", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rootRef = FirebaseDatabase.getInstance().getReference();
        duelRef = rootRef.child(DUELS_NODE).child(duelId);

        myDeviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (myDeviceId == null) myDeviceId = "";

        initViews();
        loadAd();
        setupRecycler();
        setupClicks();
        listenDuel();
        listenGuesses();
        DuelFcmTokenManager.updateTokenForCurrentUser();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnLeaveDuel = findViewById(R.id.btnLeaveDuel);

        tvMyName = findViewById(R.id.tvMyName);
        tvMyAvatar = findViewById(R.id.tvMyAvatar);
        tvOpponentName = findViewById(R.id.tvOpponentName);
        tvOpponentAvatar = findViewById(R.id.tvOpponentAvatar);

        tvTurnStatus = findViewById(R.id.tvTurnStatus);
        tvGuessCount = findViewById(R.id.tvGuessCount);

        etGuessInput = findViewById(R.id.et_guess_input);
        btnTahminEt = findViewById(R.id.btn_tahmin_et);
        rvHistory = findViewById(R.id.rv_history);

        adView = findViewById(R.id.adView);

        etGuessInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        etGuessInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(digitCount)});
    }

    private void loadAd() {
        if (adView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }
    }

    private void setupRecycler() {
        adapter = new DuelGuessAdapter(guessList, myUid);

        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(false);
        manager.setReverseLayout(false);

        rvHistory.setLayoutManager(manager);
        rvHistory.setAdapter(adapter);
        rvHistory.setNestedScrollingEnabled(true);
    }

    private void setupClicks() {
        btnBack.setOnClickListener(v -> finish());
        btnLeaveDuel.setOnClickListener(v -> showLeaveDialog());
        btnTahminEt.setOnClickListener(v -> sendGuess());
    }

    private void listenDuel() {
        duelListener = duelRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(DuelGameActivity.this, "Düello bulunamadı", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                creatorUid = safeString(snapshot.child("creatorUid").getValue());
                opponentUid = safeString(snapshot.child("opponentUid").getValue());
                creatorName = safeString(snapshot.child("creatorName").getValue(), "Oyuncu");
                opponentName = safeString(snapshot.child("opponentName").getValue(), "Rakip");
                turnUid = safeString(snapshot.child("turnUid").getValue());
                secretNumber = safeString(snapshot.child("secretNumber").getValue());
                status = safeString(snapshot.child("status").getValue());

                winnerUid = safeString(snapshot.child("winnerUid").getValue());
                winnerName = safeString(snapshot.child("winnerName").getValue());
                leftUid = safeString(snapshot.child("leftUid").getValue());

                digitCount = readInt(snapshot.child("digitCount").getValue(), 3);
                if (digitCount < 2 || digitCount > 4) digitCount = 3;

                winPoint = readInt(snapshot.child("rewardPoint").getValue(), 0);
                if (winPoint <= 0) {
                    winPoint = readInt(snapshot.child("winPoint").getValue(), 1000);
                }
                if (winPoint <= 0) winPoint = 1000;

                isCreator = myUid.equals(creatorUid);
                isMyTurn = myUid.equals(turnUid);
                isFinished = isTerminalStatus(status);

                updateInputLimit();

                if ("BITTI".equals(status)
                        && !winnerUid.isEmpty()
                        && !myUid.equals(winnerUid)
                        && !Boolean.TRUE.equals(snapshot.child("loserStatsApplied").getValue(Boolean.class))) {
                    DuelLoserStats.applyIfNeeded(duelId, myUid);
                }

                if (secretNumber.isEmpty() && isCreator && !creatingSecret && !isFinished) {
                    createSecretNumberIfNeeded();
                }

                updatePlayerUi();
                updateTurnUi();
                renderHistoryArea();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DuelGameActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenGuesses() {
        guessesListener = duelRef.child("guesses")
                .orderByChild("createdAt")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        realGuessList.clear();

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            DuelGuessModel model = ds.getValue(DuelGuessModel.class);

                            if (model != null) {
                                if (model.guessId == null || model.guessId.trim().isEmpty()) {
                                    model.guessId = ds.getKey();
                                }

                                realGuessList.add(model);
                            }
                        }

                        Collections.sort(realGuessList, (a, b) -> Long.compare(b.createdAt, a.createdAt));

                        renderHistoryArea();

                        if (tvGuessCount != null) {
                            tvGuessCount.setText(String.valueOf(realGuessList.size()));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(DuelGameActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void renderHistoryArea() {
        guessList.clear();

        if (isTerminalStatus(status)) {
            guessList.add(createResultCard());
        } else {
            guessList.addAll(realGuessList);
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private DuelGuessModel createResultCard() {
        int safePoint = winPoint > 0 ? winPoint : 1000;

        if ("BITTI".equals(status)) {
            if (winnerUid.isEmpty() || winnerName.isEmpty()) {
                for (DuelGuessModel g : realGuessList) {
                    if (g != null && g.winner) {
                        if (winnerUid.isEmpty()) winnerUid = safeString(g.uid);
                        if (winnerName.isEmpty()) winnerName = safeString(g.name, "Oyuncu");
                        break;
                    }
                }
            }

            boolean iWon = myUid.equals(winnerUid);

            if (iWon) {
                return DuelGuessModel.result(
                        "🏆",
                        "TEBRİKLER, KAZANDIN!",
                        "+" + safePoint + " puan kazandın.",
                        secretNumber.isEmpty()
                                ? "Düello başarıyla tamamlandı."
                                : "Doğru sayı: " + secretNumber,
                        0xFF22A447
                );
            } else {
                return DuelGuessModel.result(
                        "🎯",
                        "DÜELLO TAMAMLANDI",
                        safeString(winnerName, "Rakip") + " düelloyu kazandı.",
                        secretNumber.isEmpty()
                                ? "Bir sonraki düelloda şansını tekrar dene."
                                : "Doğru sayı: " + secretNumber,
                        0xFFD93025
                );
            }
        }

        if ("AYRILDI".equals(status)) {
            boolean iLeft = myUid.equals(leftUid);
            String leftName = getDisplayNameByUid(leftUid);

            if (iLeft) {
                return DuelGuessModel.result(
                        "🚪",
                        "DÜELLODAN AYRILDIN",
                        "Bu düellodan ayrıldın.",
                        "Düello artık devam ettirilemez.",
                        0xFFD93025
                );
            }

            return DuelGuessModel.result(
                    "🚪",
                    "RAKİP DÜELLODAN AYRILDI",
                    safeString(leftName, "Rakip") + " düellodan ayrıldı.",
                    "Düello artık devam ettirilemez.",
                    0xFFD93025
            );
        }

        if (isRejectedStatus(status)) {
            return DuelGuessModel.result(
                    "❌",
                    "DÜELLO REDDEDİLDİ",
                    "Davet karşı taraf tarafından reddedildi.",
                    "Yeni bir rakip seçerek tekrar düello başlatabilirsin.",
                    0xFFD93025
            );
        }

        return DuelGuessModel.result(
                "🏁",
                "DÜELLO SONUCU",
                "Düello tamamlandı.",
                "",
                0xFF1976F3
        );
    }

    private boolean isTerminalStatus(String s) {
        return "BITTI".equals(s)
                || "AYRILDI".equals(s)
                || isRejectedStatus(s);
    }

    private boolean isRejectedStatus(String s) {
        return "REDDEDILDI".equals(s)
                || "REDDEDİLDİ".equals(s)
                || "RED".equals(s)
                || "REDD".equals(s)
                || "REJECTED".equals(s);
    }

    private String getDisplayNameByUid(String uid) {
        if (uid == null || uid.trim().isEmpty()) return "Oyuncu";

        if (uid.equals(creatorUid)) return creatorName;
        if (uid.equals(opponentUid)) return opponentName;

        return "Oyuncu";
    }

    private void createSecretNumberIfNeeded() {
        creatingSecret = true;

        duelRef.child("secretNumber").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Object value = currentData.getValue();

                if (value == null || String.valueOf(value).trim().isEmpty()) {
                    currentData.setValue(generateSecretNumber(digitCount));
                }

                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                creatingSecret = false;

                if (error != null) {
                    Toast.makeText(DuelGameActivity.this, "Sayı oluşturulamadı", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private String generateSecretNumber(int count) {
        Random random = new Random();
        List<Integer> digits = new ArrayList<>();

        for (int i = 0; i <= 9; i++) digits.add(i);

        Collections.shuffle(digits, random);

        if (digits.get(0) == 0) {
            for (int i = 1; i < digits.size(); i++) {
                if (digits.get(i) != 0) {
                    int temp = digits.get(0);
                    digits.set(0, digits.get(i));
                    digits.set(i, temp);
                    break;
                }
            }
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < count; i++) {
            sb.append(digits.get(i));
        }

        return sb.toString();
    }

    private void updateInputLimit() {
        if (etGuessInput == null) return;

        etGuessInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(digitCount)});
        etGuessInput.setHint(digitCount + " basamaklı tahmin");

        String value = etGuessInput.getText().toString().trim();

        if (value.length() > digitCount) {
            etGuessInput.setText(value.substring(0, digitCount));
            etGuessInput.setSelection(etGuessInput.getText().length());
        }
    }

    private void updatePlayerUi() {

        String myName =
                isCreator ? creatorName : opponentName;

        String rivalName =
                isCreator ? opponentName : creatorName;


        tvMyName.setText(
                shortName(myName)
        );


        tvOpponentName.setText(
                shortName(rivalName)
        );


        tvMyAvatar.setText(
                getFirstLetter(myName)
        );


        tvOpponentAvatar.setText(
                getFirstLetter(rivalName)
        );
    }

    private void updateTurnUi() {
        if ("BITTI".equals(status)) {
            tvTurnStatus.setText("DÜELLO BİTTİ");
            tvTurnStatus.setTextColor(0xFF22A447);
            setInputEnabled(false);
        } else if ("AYRILDI".equals(status)) {
            tvTurnStatus.setText("OYUNCU AYRILDI");
            tvTurnStatus.setTextColor(0xFFD93025);
            setInputEnabled(false);
        } else if (isRejectedStatus(status)) {
            tvTurnStatus.setText("DÜELLO REDDEDİLDİ");
            tvTurnStatus.setTextColor(0xFFD93025);
            setInputEnabled(false);
        } else if (isMyTurn) {
            tvTurnStatus.setText("SENİN SIRAN");
            tvTurnStatus.setTextColor(0xFF1976F3);
            setInputEnabled(true);
        } else {
            tvTurnStatus.setText("RAKİBİN SIRASI");
            tvTurnStatus.setTextColor(0xFFD93025);
            setInputEnabled(false);
        }
    }

    private void setInputEnabled(boolean enabled) {
        boolean active = enabled && !sendingGuess && !isFinished;

        etGuessInput.setEnabled(active);
        btnTahminEt.setEnabled(active);

        etGuessInput.setAlpha(active ? 1f : 0.55f);
        btnTahminEt.setAlpha(active ? 1f : 0.45f);
    }

    private boolean canGuess() {
        if (isFinished) {
            Toast.makeText(this, "Düello bitmiş", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!isMyTurn) {
            Toast.makeText(this, "Şu anda sıra sende değil", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void sendGuess() {
        if (!canGuess()) return;
        if (sendingGuess) return;

        if (secretNumber == null || secretNumber.trim().isEmpty()) {
            Toast.makeText(this, "Sistem sayısı hazırlanıyor", Toast.LENGTH_SHORT).show();
            return;
        }

        String guess = etGuessInput.getText().toString().trim();

        if (guess.length() != digitCount) {
            Toast.makeText(this, digitCount + " basamaklı tahmin gir", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidGuess(guess)) {
            Toast.makeText(this, "Rakamları farklı ve ilk rakamı 0 olmayan sayı gir", Toast.LENGTH_SHORT).show();
            return;
        }

        sendingGuess = true;
        setInputEnabled(false);

        int[] result = calculateResult(secretNumber, guess);
        int correctNumber = result[0];
        int correctPlace = result[1];

        boolean isWin = guess.equals(secretNumber);

        String guessId = duelRef.child("guesses").push().getKey();

        if (guessId == null) {
            sendingGuess = false;
            updateTurnUi();
            Toast.makeText(this, "Tahmin kaydedilemedi", Toast.LENGTH_SHORT).show();
            return;
        }

        String myName = isCreator ? creatorName : opponentName;
        int safeWinPoint = winPoint > 0 ? winPoint : 1000;

        Map<String, Object> guessMap = new HashMap<>();
        guessMap.put("guessId", guessId);
        guessMap.put("uid", myUid);
        guessMap.put("name", myName);
        guessMap.put("guess", guess);
        guessMap.put("correctNumber", correctNumber);
        guessMap.put("correctPlace", correctPlace);
        guessMap.put("winner", isWin);
        guessMap.put("createdAt", ServerValue.TIMESTAMP);

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("guesses/" + guessId, guessMap);
        updateMap.put("updatedAt", ServerValue.TIMESTAMP);

        if (isWin) {
            updateMap.put("status", "BITTI");
            updateMap.put("winnerUid", myUid);
            updateMap.put("winnerName", myName);
            updateMap.put("finishedAt", ServerValue.TIMESTAMP);
            updateMap.put("winPoint", safeWinPoint);
            updateMap.put("rewardPoint", safeWinPoint);
        } else {
            String nextTurnUid = myUid.equals(creatorUid) ? opponentUid : creatorUid;
            updateMap.put("turnUid", nextTurnUid);
            updateMap.put("status", "AKTIF");
        }

        duelRef.updateChildren(updateMap).addOnCompleteListener(task -> {
            sendingGuess = false;
            updateTurnUi();

            if (task.isSuccessful()) {
                etGuessInput.setText("");

                if (isWin) {
                    applyWinRewardOnce(myUid);
                }
            } else {
                String err = task.getException() == null
                        ? "Bilinmeyen hata"
                        : task.getException().getMessage();

                Toast.makeText(this, "Tahmin gönderilemedi: " + err, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void applyWinRewardOnce(String winnerUid) {
        if (TextUtils.isEmpty(winnerUid) || TextUtils.isEmpty(duelId)) return;

        duelRef.child("rewardApplied").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Boolean alreadyApplied = currentData.getValue(Boolean.class);

                if (alreadyApplied != null && alreadyApplied) {
                    return Transaction.abort();
                }

                currentData.setValue(true);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (error != null) {
                    Toast.makeText(
                            DuelGameActivity.this,
                            "Ödül kontrol hatası: " + error.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                if (!committed) return;

                updateWinnerPointAndStats(winnerUid);
            }
        });
    }

    private void updateWinnerPointAndStats(String winnerUid) {
        String loserUid = winnerUid.equals(creatorUid) ? opponentUid : creatorUid;

        if (TextUtils.isEmpty(winnerUid) || TextUtils.isEmpty(loserUid)) return;

        final long safeWinPoint = winPoint > 0 ? winPoint : 1000L;
        final String currentMonthKey = getCurrentMonthKey();

        rootRef.child(USERS_NODE).child(winnerUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot winnerSnap) {
                        rootRef.child(USERS_NODE).child(loserUid)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot loserSnap) {
                                        String winnerDeviceId = safeString(winnerSnap.child("deviceId").getValue());
                                        String loserDeviceId = safeString(loserSnap.child("deviceId").getValue());

                                        String winnerIp = safeString(winnerSnap.child("lastIp").getValue());
                                        String loserIp = safeString(loserSnap.child("lastIp").getValue());

                                        if (winnerUid.equals(myUid) && winnerDeviceId.isEmpty()) {
                                            winnerDeviceId = myDeviceId;
                                        }

                                        boolean sameDevice =
                                                !winnerDeviceId.isEmpty()
                                                        && !loserDeviceId.isEmpty()
                                                        && winnerDeviceId.equals(loserDeviceId);

                                        boolean sameIp =
                                                !winnerIp.isEmpty()
                                                        && !loserIp.isEmpty()
                                                        && winnerIp.equals(loserIp);

                                        Map<String, Object> updates = new HashMap<>();

                                        long oldPuan = readLong(winnerSnap.child("puan").getValue(), 0L);
                                        long oldDuelPoint = readLong(winnerSnap.child("duelPoint").getValue(), 0L);
                                        long oldMonthlyPoint = readLong(winnerSnap.child("monthlyDuelPoint").getValue(), 0L);
                                        String oldMonthKey = safeString(winnerSnap.child("monthlyDuelMonthKey").getValue());

                                        if (!currentMonthKey.equals(oldMonthKey)) {
                                            oldMonthlyPoint = 0L;
                                        }

                                        // Kurallar users/$uid yazmasına sadece sahibine izin veriyor:
                                        // kazanan sadece kendi kaydını günceller, kaybeden kendi
                                        // toplamDuelo'sunu DuelLoserStats ile kendisi artırır.
                                        String winnerBase = USERS_NODE + "/" + winnerUid + "/";

                                        updates.put(winnerBase + "puan", oldPuan + safeWinPoint);
                                        updates.put(winnerBase + "duelPoint", oldDuelPoint + safeWinPoint);
                                        updates.put(winnerBase + "monthlyDuelPoint", oldMonthlyPoint + safeWinPoint);
                                        updates.put(winnerBase + "monthlyDuelMonthKey", currentMonthKey);
                                        updates.put(winnerBase + "kazandigiDuelo", ServerValue.increment(1));
                                        updates.put(winnerBase + "toplamDuelo", ServerValue.increment(1));
                                        updates.put(winnerBase + "lastDuelId", duelId);
                                        updates.put(winnerBase + "updatedAt", ServerValue.TIMESTAMP);

                                        putDuelStatsUpdate(updates, winnerUid, loserUid, sameDevice, sameIp);
                                        putDuelStatsUpdate(updates, loserUid, winnerUid, sameDevice, sameIp);

                                        updates.put(DUELS_NODE + "/" + duelId + "/winPoint", safeWinPoint);
                                        updates.put(DUELS_NODE + "/" + duelId + "/rewardPoint", safeWinPoint);
                                        updates.put(DUELS_NODE + "/" + duelId + "/rewardAppliedAt", ServerValue.TIMESTAMP);
                                        updates.put(DUELS_NODE + "/" + duelId + "/statsUpdated", true);
                                        updates.put(DUELS_NODE + "/" + duelId + "/sameDevice", sameDevice);
                                        updates.put(DUELS_NODE + "/" + duelId + "/sameIp", sameIp);

                                        rootRef.updateChildren(updates).addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(
                                                        DuelGameActivity.this,
                                                        "+" + safeWinPoint + " puan eklendi",
                                                        Toast.LENGTH_SHORT
                                                ).show();
                                            } else {
                                                // Ödül tekrar denenebilsin diye kilidi geri aç
                                                duelRef.child("rewardApplied").setValue(false);

                                                String err = task.getException() == null
                                                        ? "Bilinmeyen hata"
                                                        : task.getException().getMessage();

                                                Toast.makeText(
                                                        DuelGameActivity.this,
                                                        "Puan güncellenemedi: " + err,
                                                        Toast.LENGTH_LONG
                                                ).show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Toast.makeText(
                                                DuelGameActivity.this,
                                                "Rakip bilgisi okunamadı: " + error.getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(
                                DuelGameActivity.this,
                                "Kazanan bilgisi okunamadı: " + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void putDuelStatsUpdate(
            Map<String, Object> updates,
            String userUid,
            String opponentUid,
            boolean sameDevice,
            boolean sameIp
    ) {
        String base = "duelStats/" + userUid + "/" + opponentUid + "/";

        updates.put(base + "sameOpponentCount", ServerValue.increment(1));
        updates.put(base + "lastOpponentMatchAt", ServerValue.TIMESTAMP);
        updates.put(base + "updatedAt", ServerValue.TIMESTAMP);

        if (sameDevice) {
            updates.put(base + "lastSameDeviceMatchAt", ServerValue.TIMESTAMP);
        }

        if (sameIp) {
            updates.put(base + "lastSameIpMatchAt", ServerValue.TIMESTAMP);
        }
    }

    private int[] calculateResult(String secret, String guess) {
        int correctPlace = 0;
        int correctNumber = 0;

        for (int i = 0; i < guess.length(); i++) {
            char g = guess.charAt(i);

            if (i < secret.length() && g == secret.charAt(i)) {
                correctPlace++;
            } else if (secret.indexOf(g) >= 0) {
                correctNumber++;
            }
        }

        return new int[]{correctNumber, correctPlace};
    }

    private boolean isValidGuess(String guess) {
        if (guess == null || guess.length() != digitCount || guess.startsWith("0")) {
            return false;
        }

        Set<Character> set = new HashSet<>();

        for (char c : guess.toCharArray()) {
            if (!Character.isDigit(c) || set.contains(c)) {
                return false;
            }

            set.add(c);
        }

        return true;
    }

    private void showLeaveDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Düellodan ayrıl")
                .setMessage("Düellodan ayrılmak istediğine emin misin?")
                .setPositiveButton("Ayrıl", (dialog, which) -> leaveDuel())
                .setNegativeButton("Vazgeç", null)
                .show();
    }

    private void leaveDuel() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "AYRILDI");
        map.put("leftUid", myUid);
        map.put("leftAt", ServerValue.TIMESTAMP);
        map.put("updatedAt", ServerValue.TIMESTAMP);

        duelRef.updateChildren(map).addOnCompleteListener(task -> {
            Toast.makeText(this, "Düellodan ayrıldın", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private String getFirstLetter(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }

        return name.trim()
                .substring(0, 1)
                .toUpperCase(new Locale("tr", "TR"));
    }
    private String shortName(String name) {

        if (name == null) return "Oyuncu";

        name = name.trim();

        if (name.isEmpty()) return "Oyuncu";


        // Tek kelime uzun kullanıcı adı
        if (!name.contains(" ")) {

            if (name.length() > 14) {
                return name.substring(0, 14) + "...";
            }

            return name;
        }


        // Ad Soyad olanlarda
        String[] parts = name.split(" ");

        String first = parts[0];

        String second = parts.length > 1
                ? parts[1].substring(0,1) + "."
                : "";


        String result = first + " " + second;


        if (result.length() > 14) {
            return result.substring(0,14) + "...";
        }


        return result;
    }
    private int readInt(Object value, int def) {
        if (value == null) return def;

        try {
            if (value instanceof Integer) return (Integer) value;
            if (value instanceof Long) return ((Long) value).intValue();
            if (value instanceof Double) return ((Double) value).intValue();

            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception e) {
            return def;
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
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception e) {
            return def;
        }
    }

    private String safeString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String safeString(Object value, String def) {
        String s = safeString(value);
        return s.isEmpty() ? def : s;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (duelRef != null && duelListener != null) {
            duelRef.removeEventListener(duelListener);
        }

        if (duelRef != null && guessesListener != null) {
            duelRef.child("guesses").removeEventListener(guessesListener);
        }
    }
}