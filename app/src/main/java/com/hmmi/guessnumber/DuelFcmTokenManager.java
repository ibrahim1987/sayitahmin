package com.hmmi.guessnumber;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public final class DuelFcmTokenManager {

    private DuelFcmTokenManager() {
    }

    public static void updateTokenForCurrentUser() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (TextUtils.isEmpty(uid)) return;

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> saveToken(uid, token));
    }

    public static void saveToken(@NonNull String uid, String token) {
        if (TextUtils.isEmpty(uid) || TextUtils.isEmpty(token)) return;

        Map<String, Object> map = new HashMap<>();
        map.put("fcmToken", token);
        map.put("fcmTokenUpdatedAt", com.google.firebase.database.ServerValue.TIMESTAMP);

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .updateChildren(map);
    }
}
