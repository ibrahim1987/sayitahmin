package com.hmmi.guessnumber;

public class SinglePlayerModel {
    private final String guess;
    private final String feedback;

    public SinglePlayerModel(String guess, String feedback) {
        this.guess = guess;
        this.feedback = feedback;
    }

    public String getGuess() {
        return guess;
    }

    public String getFeedback() {
        return feedback;
    }
}