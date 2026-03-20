package com.example.superpeachsis.domain.service;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScoreManager {

    private static final String PREFS_NAME  = "superpeachsis_scores";
    private static final String KEY_BEST    = "best_score";
    private static final String KEY_SCORE   = "score_";
    private static final int    TOP_SIZE    = 5;

    private final SharedPreferences prefs;

    public ScoreManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveScore(int score) {
        List<Integer> scores = getTopScores();
        scores.add(score);
        Collections.sort(scores, Collections.reverseOrder());
        if (scores.size() > TOP_SIZE) {
            scores = scores.subList(0, TOP_SIZE);
        }

        SharedPreferences.Editor editor = prefs.edit();
        for (int i = 0; i < scores.size(); i++) {
            editor.putInt(KEY_SCORE + i, scores.get(i));
        }
        if (score > getBestScore()) {
            editor.putInt(KEY_BEST, score);
        }
        editor.apply();
    }

    public int getBestScore() {
        return prefs.getInt(KEY_BEST, 0);
    }

    public List<Integer> getTopScores() {
        List<Integer> scores = new ArrayList<>();
        for (int i = 0; i < TOP_SIZE; i++) {
            int s = prefs.getInt(KEY_SCORE + i, -1);
            if (s >= 0) scores.add(s);
        }
        return scores;
    }
}
