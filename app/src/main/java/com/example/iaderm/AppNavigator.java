package com.example.iaderm;

import android.content.Context;
import android.content.Intent;

/**
 * Centralized app navigation to keep flow consistent across screens.
 */
public final class AppNavigator {

    public static final String EXTRA_SCORE = "score";
    public static final String EXTRA_DIAGNOSIS = "diagnosis";
    public static final String EXTRA_TOP3 = "top3";
    public static final String EXTRA_ANALYSIS_ID = "analysis_id";

    private AppNavigator() {
        // Utility class
    }

    public static void openHome(Context context) {
        context.startActivity(new Intent(context, MainActivity.class));
    }

    public static void openCapture(Context context) {
        context.startActivity(new Intent(context, CaptureActivity.class));
    }

    public static void openHistory(Context context) {
        context.startActivity(new Intent(context, HistoryActivity.class));
    }

    public static void openResults(Context context, int score, String diagnosis, String top3Data) {
        Intent intent = new Intent(context, ResultsActivity.class);
        intent.putExtra(EXTRA_SCORE, score);
        intent.putExtra(EXTRA_DIAGNOSIS, diagnosis);
        intent.putExtra(EXTRA_TOP3, top3Data);
        context.startActivity(intent);
    }

    public static void openResults(Context context, long analysisId, int score, String diagnosis, String top3Data) {
        Intent intent = new Intent(context, ResultsActivity.class);
        intent.putExtra(EXTRA_ANALYSIS_ID, analysisId);
        intent.putExtra(EXTRA_SCORE, score);
        intent.putExtra(EXTRA_DIAGNOSIS, diagnosis);
        intent.putExtra(EXTRA_TOP3, top3Data);
        context.startActivity(intent);
    }

    public static void openTriggers(Context context) {
        context.startActivity(new Intent(context, TriggersActivity.class));
    }

    public static void openTriggers(Context context, long analysisId) {
        Intent intent = new Intent(context, TriggersActivity.class);
        intent.putExtra(EXTRA_ANALYSIS_ID, analysisId);
        context.startActivity(intent);
    }
}
