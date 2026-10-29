package com.hmmi.guessnumber;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


import com.hmmi.sto.R;

import org.json.JSONArray;
import org.json.JSONObject;

public class NotificationsActivity extends AppCompatActivity {

    private static final String PREF_NAME = "student_notifications";
    private static final String KEY_NOTIFICATIONS = "notifications";

    private ImageView btnBack;
    private TextView btnClearAll, tvEmptyTitle, tvEmptyDesc;
    private LinearLayout notificationContainer, emptyView;

    private SharedPreferences prefs;
    private JSONArray notificationArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_notifications);

        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        initViews();
        setupClicks();
        loadNotifications();
    }
    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnClearAll = findViewById(R.id.btnClearAll);
        notificationContainer = findViewById(R.id.notificationContainer);
        emptyView = findViewById(R.id.emptyView);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptyDesc = findViewById(R.id.tvEmptyDesc);
    }
    private void setupClicks() {
        setSafeClick(btnBack, this::finish);
        setSafeClick(btnClearAll, this::confirmClearAll);
    }
    private void loadNotifications() {
        notificationContainer.removeAllViews();

        try {
           /* String data = prefs.getString(KEY_NOTIFICATIONS, "");

            if (data == null || data.trim().isEmpty()) {
                createSampleNotifications();
                data = prefs.getString(KEY_NOTIFICATIONS, "[]");
            }

            notificationArray = new JSONArray(data);

            if (notificationArray.length() == 0) {
                showEmptyState();
                return;
            }

            showListState();

            for (int i = 0; i < notificationArray.length(); i++) {
                JSONObject obj = notificationArray.getJSONObject(i);
                addNotificationView(i, obj);
            }*/
            showEmptyState();
        } catch (Exception e)
        {
            //notificationArray = new JSONArray();
            //saveNotifications();
            showEmptyState();
        }
    }

    private void addNotificationView(final int index, JSONObject obj) {
        View item = getLayoutInflater().inflate(R.layout.item_notification, notificationContainer, false);

        TextView tvTitle = item.findViewById(R.id.tvNotificationTitle);
        TextView tvMessage = item.findViewById(R.id.tvNotificationMessage);
        TextView tvDate = item.findViewById(R.id.tvNotificationDate);
        ImageView btnDelete = item.findViewById(R.id.btnDeleteNotification);

        String title = obj.optString("title", "Bildirim");
        String message = obj.optString("message", "Yeni bir bildirimin var.");
        String date = obj.optString("date", "Bugün");

        tvTitle.setText(title);
        tvMessage.setText(message);
        tvDate.setText(date);

        setSafeClick(btnDelete, () -> deleteNotification(index));

        notificationContainer.addView(item);
    }

    private void deleteNotification(int index) {
        try {
            JSONArray newArray = new JSONArray();

            for (int i = 0; i < notificationArray.length(); i++) {
                if (i != index) {
                    newArray.put(notificationArray.getJSONObject(i));
                }
            }

            notificationArray = newArray;
            saveNotifications();

            Toast.makeText(this, "Bildirim silindi", Toast.LENGTH_SHORT).show();
            loadNotifications();

        } catch (Exception e) {
            Toast.makeText(this, "Bildirim silinemedi", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmClearAll() {
        if (notificationArray == null || notificationArray.length() == 0) {
            Toast.makeText(this, "Silinecek bildirim yok", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Tüm bildirimler silinsin mi?")
                .setMessage("Bu işlem geri alınamaz.")
                .setPositiveButton("Tümünü Sil", (dialog, which) -> clearAllNotifications())
                .setNegativeButton("Vazgeç", null)
                .show();
    }

    private void clearAllNotifications() {
        notificationArray = new JSONArray();
        saveNotifications();
        loadNotifications();
        Toast.makeText(this, "Tüm bildirimler silindi", Toast.LENGTH_SHORT).show();
    }

    private void saveNotifications() {
        prefs.edit()
                .putString(KEY_NOTIFICATIONS, notificationArray.toString())
                .apply();
    }

    private void showEmptyState() {
        notificationContainer.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        btnClearAll.setVisibility(View.GONE);

        tvEmptyTitle.setText("Bildirim yok");
        tvEmptyDesc.setText("Yeni bildirimler burada görünecek.");
    }

    private void showListState() {
        notificationContainer.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        btnClearAll.setVisibility(View.VISIBLE);
    }

    private void createSampleNotifications() {
        try {
            JSONArray array = new JSONArray();

            JSONObject n1 = new JSONObject();
            n1.put("title", "Yarın Matematik sınavın var");
            n1.put("message", "Sınav hazırlığını kontrol etmeyi unutma.");
            n1.put("date", "Bugün");

            JSONObject n2 = new JSONObject();
            n2.put("title", "Yeni ödev eklendi");
            n2.put("message", "Türkçe dersi için yeni ödev kaydı oluşturuldu.");
            n2.put("date", "Dün");

            JSONObject n3 = new JSONObject();
            n3.put("title", "Haftalık program hazır");
            n3.put("message", "Ders programını kontrol edebilirsin.");
            n3.put("date", "2 gün önce");

            array.put(n1);
            array.put(n2);
            array.put(n3);

            prefs.edit().putString(KEY_NOTIFICATIONS, array.toString()).apply();

        } catch (Exception ignored) {
        }
    }

    private void setSafeClick(View view, ClickAction action) {
        if (view == null || action == null) return;

        view.setClickable(true);
        view.setFocusable(true);

        view.setOnClickListener(v -> {
            animateClick(v);
            action.onClick();
        });
    }

    private void animateClick(View view) {
        AlphaAnimation animation = new AlphaAnimation(0.65f, 1.0f);
        animation.setDuration(130);
        view.startAnimation(animation);
    }

    private interface ClickAction {
        void onClick();
    }
}