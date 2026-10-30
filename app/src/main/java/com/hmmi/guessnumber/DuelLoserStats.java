package com.hmmi.guessnumber;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;

import java.util.HashMap;
import java.util.Map;

/**
 * Kaybeden oyuncunun toplamDuelo sayacını kendi hesabından bir kez artırır.
 * users/$uid kuralı sadece sahibine yazma izni verdiği için kazanan bunu yapamaz.
 */
public final class DuelLoserStats {

    private DuelLoserStats() {
    }

    public static void applyIfNeeded(String duelId, String loserUid) {
        if (TextUtils.isEmpty(duelId) || TextUtils.isEmpty(loserUid)) return;

        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        DatabaseReference flagRef = root.child(DuelGameActivity.DUELS_NODE)
                .child(duelId)
                .child("loserStatsApplied");

        flagRef.runTransaction(new Transaction.Handler() {
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
                if (error != null || !committed) return;

                Map<String, Object> updates = new HashMap<>();
                updates.put("toplamDuelo", ServerValue.increment(1));
                updates.put("lastDuelId", duelId);
                updates.put("updatedAt", ServerValue.TIMESTAMP);

                root.child(DuelGameActivity.USERS_NODE)
                        .child(loserUid)
                        .updateChildren(updates)
                        .addOnFailureListener(e -> flagRef.setValue(false));
            }
        });
    }
}
