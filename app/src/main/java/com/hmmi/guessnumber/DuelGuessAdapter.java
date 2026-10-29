package com.hmmi.guessnumber;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.hmmi.sto.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DuelGuessAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_GUESS = 1;
    private static final int TYPE_RESULT = 2;

    private final List<DuelGuessModel> list;
    private final String myUid;

    public DuelGuessAdapter(List<DuelGuessModel> list, String myUid) {
        this.list = list;
        this.myUid = myUid == null ? "" : myUid.trim();
    }

    @Override
    public int getItemViewType(int position) {
        DuelGuessModel m = list.get(position);
        return m != null && m.resultCard ? TYPE_RESULT : TYPE_GUESS;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_RESULT) {
            View v = inflater.inflate(R.layout.item_duel_result_row, parent, false);
            return new ResultVH(v);
        }

        View v = inflater.inflate(R.layout.item_duel_guess_row, parent, false);
        return new GuessVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DuelGuessModel m = list.get(position);
        if (m == null) return;

        if (holder instanceof ResultVH) {
            bindResultCard((ResultVH) holder, m);
        } else if (holder instanceof GuessVH) {
            bindGuessRow((GuessVH) holder, m);
        }
    }

    private void bindResultCard(@NonNull ResultVH h, @NonNull DuelGuessModel m) {
        h.tvResultEmoji.setText(safe(m.resultEmoji, "🏁"));
        h.tvResultTitle.setText(safe(m.resultTitle, "Düello Sonucu"));
        h.tvResultMessage.setText(safe(m.resultMessage, ""));
        h.tvResultSubMessage.setText(safe(m.resultSubMessage, ""));

        int color = m.resultColor == 0 ? 0xFF1976F3 : m.resultColor;

        h.tvResultTitle.setTextColor(color);
        h.tvResultEmoji.setTextColor(color);

        h.tvResultSubMessage.setVisibility(
                safe(m.resultSubMessage, "").isEmpty() ? View.GONE : View.VISIBLE
        );
    }

    private void bindGuessRow(@NonNull GuessVH h, @NonNull DuelGuessModel m) {
        boolean mine = myUid.equals(safe(m.uid, ""));
        String name = mine ? "Sen" : safe(m.name, "Rakip");
        String guess = safe(m.guess, "");

        h.tvHistoryName.setText(name);
        h.tvHistoryTime.setText(formatTime(m.createdAt));

        h.tvHistoryName.setTextColor(mine ? 0xFF1976F3 : 0xFFD93025);

        bindGuessDigits(h, guess);
        bindResultSigns(h, m.correctPlace, m.correctNumber);
    }

    private void bindGuessDigits(@NonNull GuessVH h, String guess) {
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

    private void bindResultSigns(@NonNull GuessVH h, int correctPlace, int correctNumber) {
        TextView[] views = {h.tvResult1, h.tvResult2, h.tvResult3};

        int totalMatch = correctPlace + correctNumber;

        if (totalMatch <= 0) {
            for (TextView v : views) {
                v.setVisibility(View.VISIBLE);
                v.setText("×");
                v.setTextColor(0xFFD93025);
                v.setBackgroundResource(R.drawable.bg_result_red);
            }
            return;
        }

        int index = 0;

        for (int i = 0; i < correctPlace && index < views.length; i++, index++) {
            views[index].setVisibility(View.VISIBLE);
            views[index].setText("+");
            views[index].setTextColor(0xFF1B8E24);
            views[index].setBackgroundResource(R.drawable.bg_result_green);
        }

        for (int i = 0; i < correctNumber && index < views.length; i++, index++) {
            views[index].setVisibility(View.VISIBLE);
            views[index].setText("-");
            views[index].setTextColor(0xFFF59B00);
            views[index].setBackgroundResource(R.drawable.bg_result_orange);
        }

        for (; index < views.length; index++) {
            views[index].setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class GuessVH extends RecyclerView.ViewHolder {

        TextView tvHistoryName, tvHistoryTime;
        TextView tvGuess1, tvGuess2, tvGuess3;
        TextView tvResult1, tvResult2, tvResult3;

        GuessVH(@NonNull View itemView) {
            super(itemView);

            tvHistoryName = itemView.findViewById(R.id.tvHistoryName);
            tvHistoryTime = itemView.findViewById(R.id.tvHistoryTime);

            tvGuess1 = itemView.findViewById(R.id.tvGuess1);
            tvGuess2 = itemView.findViewById(R.id.tvGuess2);
            tvGuess3 = itemView.findViewById(R.id.tvGuess3);

            tvResult1 = itemView.findViewById(R.id.tvResult1);
            tvResult2 = itemView.findViewById(R.id.tvResult2);
            tvResult3 = itemView.findViewById(R.id.tvResult3);
        }
    }

    static class ResultVH extends RecyclerView.ViewHolder {

        TextView tvResultEmoji, tvResultTitle, tvResultMessage, tvResultSubMessage;

        ResultVH(@NonNull View itemView) {
            super(itemView);

            tvResultEmoji = itemView.findViewById(R.id.tvResultEmoji);
            tvResultTitle = itemView.findViewById(R.id.tvResultTitle);
            tvResultMessage = itemView.findViewById(R.id.tvResultMessage);
            tvResultSubMessage = itemView.findViewById(R.id.tvResultSubMessage);
        }
    }

    private String safe(String s, String def) {
        return s == null || s.trim().isEmpty() ? def : s.trim();
    }

    private String formatTime(long time) {
        if (time <= 0) return "--:--";

        try {
            return new SimpleDateFormat("HH:mm", new Locale("tr", "TR"))
                    .format(new Date(time));
        } catch (Exception e) {
            return "--:--";
        }
    }
}