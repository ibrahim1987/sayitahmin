package com.hmmi.guessnumber;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.hmmi.sto.R;


public class DuelHowToPlayActivity extends AppCompatActivity {

    private TextView btnBack, btnStartDuel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duel_how_to_play);

        btnBack = findViewById(R.id.btnBack);
        btnStartDuel = findViewById(R.id.btnStartDuel);

        setupRule(
                findViewById(R.id.ruleGoal),
                "🎯",
                "Amaç",
                "Sistem gizli bir sayı üretir. Bu sayıyı rakibinden önce doğru tahmin eden oyuncu düelloyu kazanır."
        );

        setupRule(
                findViewById(R.id.ruleTurn),
                "🔁",
                "Sıra Sistemi",
                "Oyuncular sırayla tahmin yapar. Sıra sende değilse tahmin butonu pasif olur."
        );

        setupRule(
                findViewById(R.id.rulePlus),
                "+",
                "Artı İşareti",
                "+ işareti, doğru rakamı doğru yerde bildiğini gösterir."
        );

        setupRule(
                findViewById(R.id.ruleMinus),
                "−",
                "Eksi İşareti",
                "− işareti, rakamın sayıda olduğunu ama yerinin yanlış olduğunu gösterir."
        );

        setupRule(
                findViewById(R.id.ruleWrong),
                "✖",
                "Eşleşme Yoksa",
                "Tahminde hiçbir rakam eşleşmezse tahmin geçmişinde 3 adet çarpı işareti görünür."
        );

        setupRule(
                findViewById(R.id.rulePoint),
                "🏆",
                "Puan Sistemi",
                "Düelloyu kazanan oyuncuya, davet ekranında gösterilen kazanılacak puan eklenir. Aylık ve genel puan otomatik güncellenir."
        );

        btnBack.setOnClickListener(v -> finish());

        btnStartDuel.setOnClickListener(v -> {
            Intent intent = new Intent(this, DuelMainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void setupRule(LinearLayout item, String icon, String title, String desc) {
        if (item == null) return;

        TextView tvIcon = item.findViewById(R.id.tvRuleIcon);
        TextView tvTitle = item.findViewById(R.id.tvRuleTitle);
        TextView tvDesc = item.findViewById(R.id.tvRuleDesc);

        tvIcon.setText(icon);
        tvTitle.setText(title);
        tvDesc.setText(desc);
    }
}