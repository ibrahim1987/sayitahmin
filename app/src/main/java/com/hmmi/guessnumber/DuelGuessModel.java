package com.hmmi.guessnumber;

public class DuelGuessModel {

    public String guessId = "";
    public String uid = "";
    public String name = "";
    public String guess = "";

    public int correctNumber = 0;
    public int correctPlace = 0;

    public boolean winner = false;
    public long createdAt = 0;

    // ÖZEL DURUM KARTI İÇİN
    public boolean resultCard = false;
    public String resultEmoji = "";
    public String resultTitle = "";
    public String resultMessage = "";
    public String resultSubMessage = "";
    public int resultColor = 0;

    public DuelGuessModel() {
    }

    public static DuelGuessModel result(
            String emoji,
            String title,
            String message,
            String subMessage,
            int color
    ) {
        DuelGuessModel m = new DuelGuessModel();
        m.resultCard = true;
        m.resultEmoji = emoji;
        m.resultTitle = title;
        m.resultMessage = message;
        m.resultSubMessage = subMessage;
        m.resultColor = color;
        return m;
    }
}