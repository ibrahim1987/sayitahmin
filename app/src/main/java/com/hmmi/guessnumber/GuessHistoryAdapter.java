package com.hmmi.guessnumber;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hmmi.sto.R;

import java.util.List;

public class GuessHistoryAdapter extends RecyclerView.Adapter<GuessHistoryAdapter.ViewHolder> {

    private final List<SinglePlayerModel> historyList;

    public GuessHistoryAdapter(List<SinglePlayerModel> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_guess, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SinglePlayerModel item = historyList.get(position);
        if (item == null) return;

        // 1. Tahmin Edilen Sayıları Yerleştir (Örn: "158" -> '1', '5', '8')
        String guess = item.getGuess();
        bindGuessDigits(holder, guess != null ? guess : "");

        // 2. Geri Bildirim / Sonuç İşaretlerini Soldan Sağa Düzenli Yerleştir
        String feedback = item.getFeedback();
        bindResultSigns(holder, feedback != null ? feedback : "");
    }

    private void bindGuessDigits(@NonNull ViewHolder h, String guess) {
        TextView[] views = {h.tvGuess1, h.tvGuess2, h.tvGuess3};

        for (int i = 0; i < views.length; i++) {
            if (i < guess.length()) {
                views[i].setVisibility(View.VISIBLE);
                views[i].setText(String.valueOf(guess.charAt(i)));
            } else {
                views[i].setVisibility(View.GONE);
            }
        }
    }

    private void bindResultSigns(@NonNull ViewHolder h, String feedback) {
        TextView[] views = {h.tvResult1, h.tvResult2, h.tvResult3};
        String[] results = feedback.split(",");

        // Eğer feedback doğru bildiyseniz veya özel bir mesajsa
        if (feedback.contains("Doğru") || feedback.contains("Tebrikler")) {
            for (TextView v : views) {
                v.setVisibility(View.VISIBLE);
                v.setText("+");
                v.setTextColor(Color.parseColor("#1B8E24"));
                v.setBackgroundResource(R.drawable.bg_result_green);
            }
            return;
        }

        // Artı (+) ve Eksi (-) adetlerini sayalım
        int plusCount = 0;
        int minusCount = 0;

        for (String sign : results) {
            String trimmed = sign.trim();
            if ("+".equals(trimmed)) {
                plusCount++;
            } else if ("-".equals(trimmed)) {
                minusCount++;
            }
        }

        int index = 0;

        // 1. Önce tüm artı (+) işaretlerini soldan sağa diz
        for (int i = 0; i < plusCount && index < views.length; i++, index++) {
            views[index].setVisibility(View.VISIBLE);
            views[index].setText("+");
            views[index].setTextColor(Color.parseColor("#1B8E24"));
            views[index].setBackgroundResource(R.drawable.bg_result_green);
        }

        // 2. Sonra eksi (-) işaretlerini soldan sağa diz
        for (int i = 0; i < minusCount && index < views.length; i++, index++) {
            views[index].setVisibility(View.VISIBLE);
            views[index].setText("-");
            views[index].setTextColor(Color.parseColor("#F59B00"));
            views[index].setBackgroundResource(R.drawable.bg_result_orange);
        }

        // 3. Kalan kutucukları ise '×' (eşleşmeyen) olarak doldur
        while (index < views.length) {
            views[index].setVisibility(View.VISIBLE);
            views[index].setText("×");
            views[index].setTextColor(Color.RED);
            views[index].setBackgroundResource(R.drawable.bg_result_red);
            index++;
        }
    }

    @Override
    public int getItemCount() {
        return historyList == null ? 0 : historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvGuess1, tvGuess2, tvGuess3;
        TextView tvResult1, tvResult2, tvResult3;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGuess1 = itemView.findViewById(R.id.tvGuess1);
            tvGuess2 = itemView.findViewById(R.id.tvGuess2);
            tvGuess3 = itemView.findViewById(R.id.tvGuess3);

            tvResult1 = itemView.findViewById(R.id.tvResult1);
            tvResult2 = itemView.findViewById(R.id.tvResult2);
            tvResult3 = itemView.findViewById(R.id.tvResult3);
        }
    }
}