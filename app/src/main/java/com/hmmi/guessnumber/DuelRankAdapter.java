package com.hmmi.guessnumber;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hmmi.login.AuthLoginActivity;
import com.hmmi.sto.R;

import java.util.List;

public class DuelRankAdapter extends RecyclerView.Adapter<DuelRankAdapter.VH> {

    private final List<DuelPlayerModel> list;
    private final String myUid;

    private String myName;
    private boolean monthMode = false;

    public DuelRankAdapter(List<DuelPlayerModel> list, String myUid, String myName) {
        this.list = list;
        this.myUid = myUid == null ? "" : myUid.trim();
        this.myName = cleanName(myName, "Oyuncu");
    }

    public void setMyName(String myName) {
        this.myName = cleanName(myName, "Oyuncu");
    }

    public void setMonthMode(boolean monthMode) {
        this.monthMode = monthMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_duel_rank, parent, false);

        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DuelPlayerModel p = list.get(position);
        if (p == null) return;

        boolean isMe = !isEmpty(myUid) && myUid.equals(p.uid);

        int rankNo = p.rankNo > 0 ? p.rankNo : position + 1;
        h.tvRankNumber.setText(String.valueOf(rankNo));

        String username = cleanUsername(p.kullaniciadi);
        String fullname = cleanName(p.fullname, "Oyuncu");

        String displayName;

        if (!isEmpty(username)) {
            displayName = username;
        } else {
            displayName = fullname;
        }

        if (isMe) {
            displayName = displayName + " (Sen)";
        }

        long totalPoint = Math.max(
                0L,
                monthMode ? p.monthlyDuelPoint : p.duelPoint
        );

        long safeWinPoint = p.calculatedWinPoint <= 0
                ? 1000L
                : p.calculatedWinPoint;

        h.tvUsername.setText(displayName);

        h.tvTotalPoint.setText(
                (monthMode ? "Bu ay 🏆 " : "🏆 ") +
                        totalPoint +
                        " Puan"
        );

        h.tvWinPoint.setText("+" + safeWinPoint);

        if (isMe) {
            h.btnDuelInvite.setVisibility(View.GONE);
            h.btnDuelInvite.setOnClickListener(null);
            h.layoutWinPoint.setVisibility(View.GONE);
        } else {
            h.layoutWinPoint.setVisibility(View.VISIBLE);

            h.btnDuelInvite.setVisibility(View.VISIBLE);
            h.btnDuelInvite.setText("Davet Et");
            h.btnDuelInvite.setEnabled(true);

            h.btnDuelInvite.setOnClickListener(v -> {
                Context context = v.getContext();
                if (context == null) return;

                // Giriş kontrolü (Kullanıcı giriş yapmamışsa)
                if (isEmpty(myUid)) {
                    Toast.makeText(context, "Giriş yapmalısınız", Toast.LENGTH_SHORT).show();
                    Intent loginIntent = new Intent(context, AuthLoginActivity.class);
                    context.startActivity(loginIntent);
                    return;
                }

                openCreateDuelScreen(context, p);
            });
        }
    }

    private void openCreateDuelScreen(Context context, DuelPlayerModel opponent) {
        if (context == null) return;

        if (opponent == null || isEmpty(opponent.uid)) {
            Toast.makeText(context, "Rakip bilgisi eksik", Toast.LENGTH_SHORT).show();
            return;
        }

        if (myUid.equals(opponent.uid)) {
            Toast.makeText(context, "Kendine düello daveti gönderemezsin", Toast.LENGTH_SHORT).show();
            return;
        }

        long safeWinPoint = opponent.calculatedWinPoint <= 0
                ? 1000L
                : opponent.calculatedWinPoint;

        String opponentUsername = cleanUsername(opponent.kullaniciadi);
        String opponentName = cleanName(opponent.fullname, "Oyuncu");

        String visibleOpponentName = !isEmpty(opponentUsername)
                ? opponentUsername
                : opponentName;

        Intent i = new Intent(context, CreateDuelInviteActivity.class);

        i.putExtra("myUid", myUid);
        i.putExtra("myName", cleanName(myName, "Oyuncu"));

        i.putExtra("opponentUid", opponent.uid);
        i.putExtra("opponentName", visibleOpponentName);
        i.putExtra("opponentUsername", opponentUsername);

        i.putExtra("opponentTotalPoint", Math.max(0L, opponent.duelPoint));
        i.putExtra("opponentMonthlyPoint", Math.max(0L, opponent.monthlyDuelPoint));
        i.putExtra("winPoint", safeWinPoint);
        i.putExtra("rankNo", opponent.rankNo);

        context.startActivity(i);
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String cleanName(String value, String def) {
        if (value == null || value.trim().isEmpty()) {
            return def;
        }

        return value.trim();
    }

    private static String cleanUsername(String value) {
        if (value == null) return "";

        String cleaned = value.trim();

        if (cleaned.startsWith("@")) {
            cleaned = cleaned.substring(1).trim();
        }

        return cleaned;
    }

    static class VH extends RecyclerView.ViewHolder {

        TextView tvRankNumber;
        TextView tvUsername;
        TextView tvTotalPoint;
        TextView tvWinPoint;
        TextView btnDuelInvite;

        LinearLayout layoutWinPoint;

        VH(@NonNull View itemView) {
            super(itemView);

            tvRankNumber = itemView.findViewById(R.id.tvRankNumber);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvTotalPoint = itemView.findViewById(R.id.tvTotalPoint);
            tvWinPoint = itemView.findViewById(R.id.tvWinPoint);
            btnDuelInvite = itemView.findViewById(R.id.btnDuelInvite);
            layoutWinPoint = itemView.findViewById(R.id.layoutWinPoint);
        }
    }
}