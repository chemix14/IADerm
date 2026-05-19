package com.example.iaderm;

import android.content.Context;
import android.widget.Toast;

/**
 * Shared user feedback helper to keep error/status messages consistent.
 */
public final class UiFeedback {

    private UiFeedback() {
        // Utility class
    }

    public static void shortMessage(Context context, int messageRes) {
        Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show();
    }

    public static void longMessage(Context context, int messageRes) {
        Toast.makeText(context, messageRes, Toast.LENGTH_LONG).show();
    }
}
