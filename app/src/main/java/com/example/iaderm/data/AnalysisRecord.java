package com.example.iaderm.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * AnalysisRecord — Room entity representing a single skin analysis result.
 *
 * Stores the analysis score, severity level, image path, and metadata
 * for the history timeline and trend visualization.
 */
@Entity(tableName = "analysis_records")
public class AnalysisRecord {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Unix timestamp (milliseconds) when the analysis was performed */
    public long timestamp;

    /** Redness index score from 0 to 100 */
    public int score;

    /** Predicted diagnosis name */
    public String diagnosis;

    /** Severity level: "mild", "moderate", or "severe" */
    public String severity;

    /** Local file path to the captured face photo */
    public String imagePath;

    /** Comma-separated heatmap zones: "x,y,radius,intensity;x,y,r,i;..." */
    public String heatmapData;

    /** Optional notes from the user */
    public String notes;

    // ── Constructors ──

    public AnalysisRecord() {}

    @Ignore
    public AnalysisRecord(long timestamp, int score, String diagnosis, String severity,
                          String imagePath, String heatmapData) {
        this.timestamp = timestamp;
        this.score = score;
        this.diagnosis = diagnosis;
        this.severity = severity;
        this.imagePath = imagePath;
        this.heatmapData = heatmapData;
    }

    /**
     * Get severity level based on score.
     */
    public static String calculateSeverity(int score) {
        if (score <= 35) return "mild";
        if (score <= 65) return "moderate";
        return "severe";
    }
}
