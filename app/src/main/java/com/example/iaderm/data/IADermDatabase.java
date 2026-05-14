package com.example.iaderm.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * IADermDatabase — Room database singleton.
 *
 * Contains two tables:
 * - analysis_records: Skin analysis results with scores and heatmap data
 * - trigger_records: User-logged rosacea triggers
 *
 * Uses the Singleton pattern to prevent multiple database instances.
 */
@Database(
    entities = {AnalysisRecord.class, TriggerRecord.class},
    version = 2,
    exportSchema = false
)
public abstract class IADermDatabase extends RoomDatabase {

    private static volatile IADermDatabase INSTANCE;

    public abstract AnalysisDao analysisDao();
    public abstract TriggerDao triggerDao();

    /**
     * Get the singleton database instance.
     * Thread-safe via double-checked locking.
     */
    public static IADermDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (IADermDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            IADermDatabase.class,
                            "iaderm_database"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
