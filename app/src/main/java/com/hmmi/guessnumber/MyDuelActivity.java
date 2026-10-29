package com.hmmi.guessnumber;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.hmmi.sto.R;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
public class MyDuelActivity extends AppCompatActivity {

    public static final String DUELS_NODE = "zekaduels";

    // Geçmiş kayıtları 12 saat sonra Firebase'den tamamen silinir.
    private static final long HISTORY_AUTO_DELETE_MS = 12L * 60L * 60L * 1000L;

    private TextView btnBack, tabPending, tabActive, tabHistory, tvEmpty, tvTitle;
    private RecyclerView recyclerView;

    private AdView adView;
    private DatabaseReference duelsRef;
    private ValueEventListener duelsListener;

    private String myUid = "";
    private String myDeviceId = "";
    private String myIp = "";

    private final ArrayList<DuelModel> pendingList = new ArrayList<>();
    private final ArrayList<DuelModel> activeList = new ArrayList<>();
    private final ArrayList<DuelModel> historyList = new ArrayList<>();

    private MyDuelAdapter adapter;
    private String currentTab = "PENDING";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_duels);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || uid.trim().isEmpty()) {
            Toast.makeText(this, "Oturum bulunamadı", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        myUid = uid;
        myDeviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (myDeviceId == null) myDeviceId = "";

        duelsRef = FirebaseDatabase.getInstance().getReference(DUELS_NODE);

        initViews();
        setupAds();
        setupRecycler();
        setupClicks();
        String openTab = safe(getIntent().getStringExtra("openTab"), "");
        if ("ACTIVE".equals(openTab) || "HISTORY".equals(openTab) || "PENDING".equals(openTab)) {
            currentTab = openTab;
        }
        loadMyInfo();
        loadMyDuels();
        DuelFcmTokenManager.updateTokenForCurrentUser();
    }

    private void initViews() {


        btnBack =
                findViewById(R.id.btnBack);


        tabPending =
                findViewById(R.id.tabPending);

        tabActive =
                findViewById(R.id.tabActive);

        tabHistory =
                findViewById(R.id.tabHistory);


        tvEmpty =
                findViewById(R.id.tvEmpty);


        tvTitle =
                findViewById(R.id.tvTitle);


        recyclerView =
                findViewById(R.id.recyclerView);



        // ADMOB
        adView =
                findViewById(R.id.adView);

    }
    private void setupAds() {


        MobileAds.initialize(
                this,
                initializationStatus -> {

                }
        );


        if (adView != null) {


            AdRequest request =
                    new AdRequest
                            .Builder()
                            .build();


            adView.loadAd(request);

        }

    }
    private void setupRecycler() {
        adapter = new MyDuelAdapter(new ArrayList<>(), myUid, new MyDuelAdapter.DuelClickListener() {
            @Override
            public void onPrimaryClick(DuelModel duel) {
                handlePrimaryClick(duel);
            }

            @Override
            public void onSecondaryClick(DuelModel duel) {
                handleSecondaryClick(duel);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupClicks() {
        btnBack.setOnClickListener(v -> finish());

        tabPending.setOnClickListener(v -> {
            currentTab = "PENDING";
            refreshTabs();
        });

        tabActive.setOnClickListener(v -> {
            currentTab = "ACTIVE";
            refreshTabs();
        });

        tabHistory.setOnClickListener(v -> {
            currentTab = "HISTORY";
            refreshTabs();
        });
    }

    private void loadMyInfo() {
        FirebaseDatabase.getInstance().getReference("users").child(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String ip = snapshot.child("lastIp").getValue(String.class);
                        myIp = ip == null ? "" : ip.trim();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void loadMyDuels() {
        duelsListener = duelsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pendingList.clear();
                activeList.clear();
                historyList.clear();

                long now = System.currentTimeMillis();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    DuelModel duel = ds.getValue(DuelModel.class);
                    if (duel == null) continue;

                    duel.duelId = safe(duel.duelId, ds.getKey());

                    if (!isMyDuel(duel)) continue;

                    String status = safe(duel.status, "");

                    if (isHistoryStatus(status)) {
                        long historyTime = getHistoryTime(duel);

                        if (historyTime > 0 && now - historyTime >= HISTORY_AUTO_DELETE_MS) {
                            duelsRef.child(duel.duelId).removeValue();
                            continue;
                        }

                        boolean deletedForMe =
                                duel.deletedBy != null
                                        && Boolean.TRUE.equals(duel.deletedBy.get(myUid));

                        if (!deletedForMe) {
                            historyList.add(duel);
                        }

                        if ("BITTI".equals(status)
                                && !safe(duel.winnerUid, "").isEmpty()
                                && !myUid.equals(duel.winnerUid)
                                && !duel.loserStatsApplied) {
                            DuelLoserStats.applyIfNeeded(duel.duelId, myUid);
                        }

                    } else if ("DAVET".equals(status)) {
                        pendingList.add(duel);

                    } else if ("AKTIF".equals(status)) {
                        activeList.add(duel);
                    }
                }

                sortLists();
                refreshTabs();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MyDuelActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isMyDuel(DuelModel duel) {
        return duel != null && (myUid.equals(duel.creatorUid) || myUid.equals(duel.opponentUid));
    }

    private boolean isHistoryStatus(String status) {
        return "BITTI".equals(status)
                || "AYRILDI".equals(status)
                || "SURE_DOLDU".equals(status)
                || "RED".equals(status)
                || "IPTAL".equals(status);
    }

    private long getHistoryTime(DuelModel duel) {
        if (duel == null) return 0L;

        if (duel.finishedAt > 0) return duel.finishedAt;
        if (duel.expiredAt > 0) return duel.expiredAt;
        if (duel.updatedAt > 0) return duel.updatedAt;
        return duel.createdAt;
    }

    private void sortLists() {
        Comparator<DuelModel> comparator = (a, b) -> Long.compare(b.updatedAt, a.updatedAt);
        Collections.sort(pendingList, comparator);
        Collections.sort(activeList, comparator);
        Collections.sort(historyList, comparator);
    }

    private void refreshTabs() {
        selectTab(tabPending, "PENDING".equals(currentTab));
        selectTab(tabActive, "ACTIVE".equals(currentTab));
        selectTab(tabHistory, "HISTORY".equals(currentTab));

        ArrayList<DuelModel> showList;

        if ("PENDING".equals(currentTab)) {
            tvTitle.setText("Bekleyen Davetler");
            showList = pendingList;
        } else if ("ACTIVE".equals(currentTab)) {
            tvTitle.setText("Aktif Düellolar");
            showList = activeList;
        } else {
            tvTitle.setText("Geçmiş Düellolar");
            showList = historyList;
        }

        adapter.setList(showList);
        tvEmpty.setVisibility(showList.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(showList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void selectTab(TextView tab, boolean selected) {
        if (tab == null) return;

        tab.setBackgroundResource(selected ? R.drawable.bg_duel_tab_selected : R.drawable.bg_duel_tab_normal);
        tab.setTextColor(selected ? Color.WHITE : Color.parseColor("#52708F"));
    }

    private void handlePrimaryClick(DuelModel duel) {
        if (duel == null) return;

        String status = safe(duel.status, "");

        if ("DAVET".equals(status)) {
            if (myUid.equals(duel.opponentUid)) {
                acceptDuel(duel);
            } else {
                Toast.makeText(this, "Rakibin kabul etmesi bekleniyor", Toast.LENGTH_SHORT).show();
            }
        } else if ("AKTIF".equals(status)) {
            Intent i = new Intent(this, DuelGameActivity.class);
            i.putExtra("duelId", duel.duelId);
            startActivity(i);
        } else if (isHistoryStatus(status)) {
            openRematchInvitePage(duel);
        }
    }

    private void handleSecondaryClick(DuelModel duel) {
        if (duel == null) return;

        String status = safe(duel.status, "");

        if ("DAVET".equals(status)) {
            if (myUid.equals(duel.opponentUid)) {
                rejectDuel(duel);
            } else if (myUid.equals(duel.creatorUid)) {
                cancelDuel(duel);
            }
            return;
        }

        if (isHistoryStatus(status)) {
            deleteDuelNow(duel);
        }
    }

    private void deleteDuelNow(DuelModel duel) {
        if (duel == null || safe(duel.duelId, "").isEmpty()) return;

        String status = safe(duel.status, "");

        if (!isHistoryStatus(status)) {
            Toast.makeText(this, "Sadece geçmiş kayıtları silinebilir", Toast.LENGTH_SHORT).show();
            return;
        }

        long historyTime = getHistoryTime(duel);
        long now = System.currentTimeMillis();

        if (historyTime > 0 && now - historyTime >= HISTORY_AUTO_DELETE_MS) {
            duelsRef.child(duel.duelId).removeValue()
                    .addOnSuccessListener(unused ->
                            Toast.makeText(MyDuelActivity.this, "Düello kaydı tamamen silindi", Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(
                                    MyDuelActivity.this,
                                    "Silinemedi: " + (e == null ? "Bilinmeyen hata" : e.getMessage()),
                                    Toast.LENGTH_LONG
                            ).show()
                    );
            return;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("deletedBy/" + myUid, true);
        map.put("deletedAtBy/" + myUid, ServerValue.TIMESTAMP);
        map.put("updatedAt", ServerValue.TIMESTAMP);

        duelsRef.child(duel.duelId).updateChildren(map)
                .addOnSuccessListener(unused ->
                        Toast.makeText(MyDuelActivity.this, "Geçmişten kaldırıldı", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(
                                MyDuelActivity.this,
                                "Silinemedi: " + (e == null ? "Bilinmeyen hata" : e.getMessage()),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void acceptDuel(DuelModel duel) {
        if (duel == null || safe(duel.duelId, "").isEmpty()) {
            Toast.makeText(this, "Düello bilgisi bulunamadı", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!myUid.equals(duel.opponentUid)) {
            Toast.makeText(this, "Bu davet sana ait değil", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("status", "AKTIF");
        map.put("opponentReady", true);
        map.put("creatorReady", true);
        map.put("turnUid", myUid);
        map.put("startedAt", ServerValue.TIMESTAMP);
        map.put("updatedAt", ServerValue.TIMESTAMP);

        duelsRef.child(duel.duelId).updateChildren(map)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Davet kabul edildi", Toast.LENGTH_SHORT).show();

                    Intent i = new Intent(MyDuelActivity.this, DuelGameActivity.class);
                    i.putExtra("duelId", duel.duelId);
                    startActivity(i);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                MyDuelActivity.this,
                                "Davet kabul edilemedi: " + (e == null ? "Bilinmeyen hata" : e.getMessage()),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void rejectDuel(DuelModel duel) {
        updateStatus(duel, "RED");
    }

    private void cancelDuel(DuelModel duel) {
        updateStatus(duel, "IPTAL");
    }

    private void updateStatus(DuelModel duel, String status) {
        if (duel == null || safe(duel.duelId, "").isEmpty()) return;

        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        map.put("updatedAt", ServerValue.TIMESTAMP);
        map.put("expiredAt", ServerValue.TIMESTAMP);

        duelsRef.child(duel.duelId).updateChildren(map)
                .addOnFailureListener(e ->
                        Toast.makeText(
                                MyDuelActivity.this,
                                "İşlem başarısız: " + (e == null ? "Bilinmeyen hata" : e.getMessage()),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void openRematchInvitePage(DuelModel oldDuel) {
        if (oldDuel == null) return;

        boolean iAmCreator = myUid.equals(oldDuel.creatorUid);

        String rivalUid = iAmCreator ? oldDuel.opponentUid : oldDuel.creatorUid;
        String rivalName = iAmCreator ? oldDuel.opponentName : oldDuel.creatorName;
        String myNameForInvite = iAmCreator ? oldDuel.creatorName : oldDuel.opponentName;

        if (safe(rivalUid, "").isEmpty()) {
            Toast.makeText(this, "Rakip bilgisi bulunamadı", Toast.LENGTH_SHORT).show();
            return;
        }

        int digit = oldDuel.digitCount >= 2 && oldDuel.digitCount <= 4 ? oldDuel.digitCount : 3;

        loadCurrentWinPointAndOpenInvite(rivalUid, rivalName, myNameForInvite, digit);
    }

    private void loadCurrentWinPointAndOpenInvite(
            String rivalUid,
            String rivalName,
            String myNameForInvite,
            int digit
    ) {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference();

        root.child("users").child(rivalUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot rivalSnap) {
                String rivalDeviceId = safe(rivalSnap.child("deviceId").getValue(String.class), "");
                String rivalIp = safe(rivalSnap.child("lastIp").getValue(String.class), "");

                root.child("duelStats").child(myUid).child(rivalUid)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot statsSnap) {
                                long count = readLong(statsSnap.child("sameOpponentCount").getValue(), 0L);
                                long lastOpponent = readLong(statsSnap.child("lastOpponentMatchAt").getValue(), 0L);
                                long lastDevice = readLong(statsSnap.child("lastSameDeviceMatchAt").getValue(), 0L);
                                long lastIp = readLong(statsSnap.child("lastSameIpMatchAt").getValue(), 0L);

                                boolean firstTime = !statsSnap.exists() || count <= 0 || lastOpponent <= 1;
                                int sameOpponentCount = firstTime ? 0 : (int) Math.max(1L, count);

                                boolean sameDevice =
                                        !TextUtils.isEmpty(myDeviceId)
                                                && !TextUtils.isEmpty(rivalDeviceId)
                                                && myDeviceId.equals(rivalDeviceId);

                                boolean sameIp =
                                        !TextUtils.isEmpty(myIp)
                                                && !TextUtils.isEmpty(rivalIp)
                                                && myIp.equals(rivalIp);

                                long currentPoint = DuelPointCalculator.calculatePoint(
                                        firstTime,
                                        sameDevice,
                                        sameIp,
                                        sameOpponentCount,
                                        daysSince(lastOpponent),
                                        daysSince(lastDevice),
                                        daysSince(lastIp)
                                );

                                if (currentPoint <= 0) currentPoint = 100L;

                                Intent i = new Intent(MyDuelActivity.this, CreateDuelInviteActivity.class);
                                i.putExtra("opponentUid", rivalUid);
                                i.putExtra("opponentName", safe(rivalName, "Rakip Oyuncu"));
                                i.putExtra("myName", safe(myNameForInvite, "Oyuncu"));
                                i.putExtra("winPoint", currentPoint);
                                i.putExtra("digitCount", digit);
                                startActivity(i);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(MyDuelActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MyDuelActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();


        if (
                duelsRef != null
                        &&
                        duelsListener != null
        ) {

            duelsRef.removeEventListener(
                    duelsListener
            );
        }



        // ADMOB TEMİZLEME

        if (adView != null) {

            adView.destroy();

        }
    }

    private static String safe(String s, String def) {
        return s == null || s.trim().isEmpty() ? def : s.trim();
    }

    private static long readLong(Object value, long def) {
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

    private static long daysSince(long timestamp) {
        if (timestamp <= 1) return 1L;

        long diff = System.currentTimeMillis() - timestamp;
        if (diff <= 0) return 1L;

        long days = TimeUnit.MILLISECONDS.toDays(diff);
        return Math.max(1L, days);
    }

    public static class DuelModel {
        public String duelId;
        public String creatorUid;
        public String creatorName;
        public String opponentUid;
        public String opponentName;
        public String status;

        public String winnerUid;
        public String winnerName;
        public String leftUid;

        public int digitCount;
        public long winPoint;
        public long rewardPoint;
        public boolean loserStatsApplied;

        public long createdAt;
        public long updatedAt;
        public long expiredAt;
        public long startedAt;
        public long finishedAt;

        public Map<String, Boolean> deletedBy;
        public Map<String, Long> deletedAtBy;

        public DuelModel() {
        }
    }

    public static class MyDuelAdapter extends RecyclerView.Adapter<MyDuelAdapter.VH> {

        private final ArrayList<DuelModel> list;
        private final String myUid;
        private final DuelClickListener listener;

        public interface DuelClickListener {
            void onPrimaryClick(DuelModel duel);

            void onSecondaryClick(DuelModel duel);
        }

        public MyDuelAdapter(ArrayList<DuelModel> list, String myUid, DuelClickListener listener) {
            this.list = list == null ? new ArrayList<>() : list;
            this.myUid = myUid == null ? "" : myUid;
            this.listener = listener;
        }

        public void setList(ArrayList<DuelModel> newList) {
            list.clear();

            if (newList != null) {
                list.addAll(newList);
            }

            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_duel, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            DuelModel d = list.get(position);

            boolean iAmCreator = myUid.equals(d.creatorUid);
            String rivalName = iAmCreator ? d.opponentName : d.creatorName;
            String status = safe(d.status, "");

            long point = d.rewardPoint > 0 ? d.rewardPoint : d.winPoint;
            if (point <= 0) point = 1000L;

            h.tvName.setText(safe(rivalName, "Rakip Oyuncu"));
            h.tvInfo.setText((d.digitCount > 0 ? d.digitCount : 3) + " haneli sayı tahmin düellosu");
            h.tvDate.setText(formatDate(d.updatedAt));

            bindPointText(h, d, status, point);

            h.tvPrimary.setVisibility(View.VISIBLE);
            h.tvSecondary.setVisibility(View.GONE);

            if ("DAVET".equals(status)) {
                h.tvStatus.setText(iAmCreator ? "Cevap bekleniyor" : "Sana davet geldi");

                h.tvPrimary.setText(iAmCreator ? "Bekleniyor" : "Kabul Et");
                h.tvPrimary.setEnabled(!iAmCreator);
                h.tvPrimary.setAlpha(iAmCreator ? 0.55f : 1f);

                h.tvSecondary.setText(iAmCreator ? "İptal Et" : "Reddet");
                h.tvSecondary.setVisibility(View.VISIBLE);

            } else {
                h.tvPrimary.setEnabled(true);
                h.tvPrimary.setAlpha(1f);

                if ("AKTIF".equals(status)) {
                    h.tvStatus.setText("Devam ediyor");
                    h.tvPrimary.setText("Devam Et");

                } else if ("BITTI".equals(status)) {
                    h.tvStatus.setText("Tamamlandı");
                    h.tvPrimary.setText("Rövanş Yap");
                    h.tvSecondary.setText("Sil");
                    h.tvSecondary.setVisibility(View.VISIBLE);

                } else if ("AYRILDI".equals(status)) {
                    h.tvStatus.setText("Oyuncu ayrıldı");
                    h.tvPrimary.setText("Tekrar Davet Et");
                    h.tvSecondary.setText("Sil");
                    h.tvSecondary.setVisibility(View.VISIBLE);

                } else if ("SURE_DOLDU".equals(status)) {
                    h.tvStatus.setText("Süresi doldu");
                    h.tvPrimary.setText("Tekrar Davet Et");
                    h.tvSecondary.setText("Sil");
                    h.tvSecondary.setVisibility(View.VISIBLE);

                } else if ("RED".equals(status)) {
                    h.tvStatus.setText("Reddedildi");
                    h.tvPrimary.setText("Tekrar Davet Et");
                    h.tvSecondary.setText("Sil");
                    h.tvSecondary.setVisibility(View.VISIBLE);

                } else if ("IPTAL".equals(status)) {
                    h.tvStatus.setText("İptal edildi");
                    h.tvPrimary.setText("Tekrar Davet Et");
                    h.tvSecondary.setText("Sil");
                    h.tvSecondary.setVisibility(View.VISIBLE);

                } else {
                    h.tvStatus.setText("Bilinmeyen durum");
                    h.tvPrimary.setText("Detay");
                    h.tvPrimary.setVisibility(View.INVISIBLE);
                }
            }

            h.tvPrimary.setOnClickListener(v -> {
                if (listener != null) listener.onPrimaryClick(d);
            });

            h.tvSecondary.setOnClickListener(v -> {
                if (listener != null) listener.onSecondaryClick(d);
            });
        }

        private void bindPointText(@NonNull VH h, DuelModel d, String status, long point) {
            if ("BITTI".equals(status)) {
                if (myUid.equals(d.winnerUid)) {
                    h.tvPoint.setText("🏆 +" + point + " puan kazandın");
                    h.tvPoint.setTextColor(Color.parseColor("#1FA463"));
                } else {
                    h.tvPoint.setText("❌ Puan kazanamadın");
                    h.tvPoint.setTextColor(Color.parseColor("#D9534F"));
                }
            } else if ("AYRILDI".equals(status)) {
                h.tvPoint.setText("Düello tamamlanmadı");
                h.tvPoint.setTextColor(Color.parseColor("#6F8396"));
            } else if ("RED".equals(status) || "IPTAL".equals(status) || "SURE_DOLDU".equals(status)) {
                h.tvPoint.setText("Puan verilmedi");
                h.tvPoint.setTextColor(Color.parseColor("#6F8396"));
            } else {
                h.tvPoint.setText("Kazanırsan +" + point + " puan");
                h.tvPoint.setTextColor(Color.parseColor("#183B59"));
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvInfo, tvPoint, tvStatus, tvDate, tvPrimary, tvSecondary;

            VH(@NonNull View itemView) {
                super(itemView);

                tvName = itemView.findViewById(R.id.tvName);
                tvInfo = itemView.findViewById(R.id.tvInfo);
                tvPoint = itemView.findViewById(R.id.tvPoint);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvPrimary = itemView.findViewById(R.id.tvPrimary);
                tvSecondary = itemView.findViewById(R.id.tvSecondary);
            }
        }

        private static String formatDate(long time) {
            if (time <= 0) return "";

            try {
                return new SimpleDateFormat("dd.MM.yyyy HH:mm", new Locale("tr", "TR")).format(new Date(time));
            } catch (Exception e) {
                return "";
            }
        }
    }
}