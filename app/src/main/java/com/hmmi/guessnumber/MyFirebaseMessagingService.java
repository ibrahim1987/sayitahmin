package com.hmmi.guessnumber;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.hmmi.sto.R;


import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "duel_notifications";
    private static final int BASE_NOTIFICATION_ID = 240600;

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        String uid = FirebaseAuth.getInstance().getUid();
        if (!TextUtils.isEmpty(uid)) {
            DuelFcmTokenManager.saveToken(uid, token);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        Map<String, String> data = message.getData();
        if (data == null || data.isEmpty()) return;

        String type = safe(data.get("type"));
        String duelId = safe(data.get("duelId"));

        if (!type.startsWith("DUEL_")) return;

        String title = safe(data.get("title"));
        String body = safe(data.get("body"));

        if (title.isEmpty()) title = "Sayı düellosu";
        if (body.isEmpty()) body = "Yeni bir düello bildirimin var.";

        showDuelNotification(type, duelId, title, body);
    }

    private void showDuelNotification(String type, String duelId, String title, String body) {
        createChannel();

        Intent intent;

        if ("DUEL_TURN".equals(type)
                || "DUEL_ACCEPTED".equals(type)
                || "DUEL_FINISHED".equals(type)
                || "DUEL_LEFT".equals(type)) {
            intent = new Intent(this, DuelGameActivity.class);
            intent.putExtra("duelId", duelId);
        } else {
            intent = new Intent(this, MyDuelActivity.class);
            intent.putExtra("openTab", "PENDING");
            intent.putExtra("duelId", duelId);
            intent.putExtra("fromNotification", true);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int requestCode = Math.abs((type + duelId).hashCode());

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                requestCode,
                intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(BASE_NOTIFICATION_ID + requestCode % 100000, builder.build());
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;

        NotificationChannel old = manager.getNotificationChannel(CHANNEL_ID);
        if (old != null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Düello Bildirimleri",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Sayı düellosu davetleri, sıra bildirimleri ve sonuçları");

        manager.createNotificationChannel(channel);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
