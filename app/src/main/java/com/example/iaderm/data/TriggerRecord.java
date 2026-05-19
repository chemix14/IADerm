package com.example.iaderm.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * TriggerRecord — Room entity for tracking rosacea triggers (desencadenantes).
 *
 * Each record represents a user-logged trigger event with a timestamp
 * and an optional association to an analysis record.
 */
@Entity(tableName = "trigger_records")
public class TriggerRecord {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Unix timestamp (milliseconds) */
    public long timestamp;

    /** Trigger type: "sun", "stress", "alcohol", "spicy", "cosmetics",
     *  "exercise", "temperature", "other" */
    public String triggerType;

    /** Intensity: 1 (low) to 5 (high) */
    public int intensity;

    /** Optional custom description */
    public String description;

    /** Optional FK to associated analysis (0 if none) */
    public long analysisId;

    public TriggerRecord() {}

    @Ignore
    public TriggerRecord(long timestamp, String triggerType, int intensity, String description) {
        this.timestamp = timestamp;
        this.triggerType = triggerType;
        this.intensity = intensity;
        this.description = description;
        this.analysisId = 0;
    }

    /**
     * Get emoji icon for a trigger type.
     * Handles both internal keys and localized strings from UI.
     */
    public static String getEmojiForType(String type) {
        if (type == null) return "📝";
        String lower = type.toLowerCase();

        if (lower.contains("alimentación") || lower.contains("food")) return "🍎";
        if (lower.contains("estrés") || lower.contains("stress")) return "🧠";
        if (lower.contains("sueño") || lower.contains("sleep")) return "💤";
        if (lower.contains("clima") || lower.contains("weather")) return "☀️";
        if (lower.contains("producto") || lower.contains("skincare") || lower.contains("cosméticos")) return "🧴";
        if (lower.contains("sol") || lower.contains("sun")) return "☀️";
        if (lower.contains("alcohol")) return "🍷";
        if (lower.contains("picante") || lower.contains("spicy")) return "🌶️";
        if (lower.contains("ejercicio") || lower.contains("exercise")) return "🏃";
        if (lower.contains("temperatura") || lower.contains("temperature")) return "🌡️";

        return "📝";
    }
}
