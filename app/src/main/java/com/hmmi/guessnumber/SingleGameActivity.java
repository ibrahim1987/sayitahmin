package com.hmmi.guessnumber;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.hmmi.sto.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class SingleGameActivity extends AppCompatActivity {

    private AdView adView;
    private NumberPicker numberPicker1, numberPicker2, numberPicker3;
    private Button buttonTahmin;
    private RecyclerView recyclerView;
    private LinearLayout navHome, tvExitDuel;

    private GuessHistoryAdapter adapter;
    private final List<SinglePlayerModel> historyList = new ArrayList<>();

    private String targetNumber = "";
    private int attemptCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_game);

        initViews();
        setupAdMob();
        setupNumberPickers();
        generateTargetNumber();
        setupRecyclerView();
        setupListeners();
    }

    private void initViews() {
        adView = findViewById(R.id.adView);
        numberPicker1 = findViewById(R.id.numberPicker1);
        numberPicker2 = findViewById(R.id.numberPicker2);
        numberPicker3 = findViewById(R.id.numberPicker3);
        buttonTahmin = findViewById(R.id.buttontahmin);
        recyclerView = findViewById(R.id.recycler_view);
        navHome = findViewById(R.id.navHome);
        tvExitDuel = findViewById(R.id.tv_exit_duel);
    }

    private void setupAdMob() {
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    private void setupNumberPickers() {
        configurePicker(numberPicker1);
        configurePicker(numberPicker2);
        configurePicker(numberPicker3);
    }

    private void configurePicker(NumberPicker picker) {
        if (picker != null) {
            picker.setMinValue(0);
            picker.setMaxValue(9);
            picker.setWrapSelectorWheel(true);
        }
    }

    private void generateTargetNumber() {
        Random random = new Random();
        int d1, d2, d3;

        // 3 rakam da benzersiz olana ve ilk rakam 0 olana kadar döner
        do {
            d1 = random.nextInt(9) + 1; // 1 ile 9 arası (0 olamaz)
            d2 = random.nextInt(10);    // 0 ile 9 arası
            d3 = random.nextInt(10);    // 0 ile 9 arası
        } while (d1 == d2 || d1 == d3 || d2 == d3);

        targetNumber = String.valueOf(d1) + d2 + d3;
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GuessHistoryAdapter(historyList);
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        buttonTahmin.setOnClickListener(v -> evaluateGuess());

        navHome.setOnClickListener(v -> finish());

        tvExitDuel.setOnClickListener(v -> {
            Toast.makeText(this, "Oyun sonlandırıldı. Yeni sayı belirleniyor.", Toast.LENGTH_SHORT).show();
            generateTargetNumber();
            historyList.clear();
            if (adapter != null) adapter.notifyDataSetChanged();
            attemptCount = 0;
        });
    }

    private void evaluateGuess() {
        int val1 = numberPicker1.getValue();
        int val2 = numberPicker2.getValue();
        int val3 = numberPicker3.getValue();

        // Kural 1: İlk rakam 0 olamaz
        if (val1 == 0) {
            Toast.makeText(this, "İlk rakam 0 olamaz!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kural 2: Üç rakam da birbirinden farklı olmalı
        if (val1 == val2 || val1 == val3 || val2 == val3) {
            Toast.makeText(this, "Tüm rakamlar birbirinden farklı olmalıdır!", Toast.LENGTH_SHORT).show();
            return;
        }

        String guess = String.valueOf(val1) + val2 + val3;
        attemptCount++;

        if (guess.equals(targetNumber)) {
            Toast.makeText(this, "Tebrikler! " + attemptCount + ". denemede bildiniz! 🎉", Toast.LENGTH_LONG).show();
            historyList.add(0, new SinglePlayerModel(guess, "+,+,+"));
            if (adapter != null) adapter.notifyDataSetChanged();
            generateTargetNumber();
            attemptCount = 0;
            return;
        }

        String feedback = calculateFeedback(guess, targetNumber);
        historyList.add(0, new SinglePlayerModel(guess, feedback));
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private String calculateFeedback(String guess, String target) {
        char[] targetChars = target.toCharArray();
        char[] guessChars = guess.toCharArray();

        String[] results = new String[]{"x", "x", "x"};
        boolean[] targetMatched = new boolean[3];
        boolean[] guessMatched = new boolean[3];

        // 1. Aşama: Doğru yerdeki rakamlar (+)
        for (int i = 0; i < 3; i++) {
            if (guessChars[i] == targetChars[i]) {
                results[i] = "+";
                targetMatched[i] = true;
                guessMatched[i] = true;
            }
        }

        // 2. Aşama: Yeri yanlış olan rakamlar (-)
        for (int i = 0; i < 3; i++) {
            if (guessMatched[i]) continue;
            for (int j = 0; j < 3; j++) {
                if (!targetMatched[j] && guessChars[i] == targetChars[j]) {
                    results[i] = "-";
                    targetMatched[j] = true;
                    break;
                }
            }
        }

        return results[0] + "," + results[1] + "," + results[2];
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) adView.resume();
    }

    @Override
    protected void onPause() {
        if (adView != null) adView.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (adView != null) adView.destroy();
        super.onDestroy();
    }
}