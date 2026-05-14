package com.example.iaderm.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * TriggerDao — Data Access Object for trigger records.
 */
@Dao
public interface TriggerDao {

    @Insert
    long insert(TriggerRecord record);

    @Delete
    void delete(TriggerRecord record);

    /** Get all triggers, newest first */
    @Query("SELECT * FROM trigger_records ORDER BY timestamp DESC")
    LiveData<List<TriggerRecord>> getAllTriggers();

    /** Get recent triggers (last N days) */
    @Query("SELECT * FROM trigger_records WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    LiveData<List<TriggerRecord>> getTriggersSince(long sinceTimestamp);

    /** Get triggers associated with an analysis */
    @Query("SELECT * FROM trigger_records WHERE analysisId = :analysisId")
    LiveData<List<TriggerRecord>> getTriggersForAnalysis(long analysisId);

    /** Count triggers by type (for correlation insights) */
    @Query("SELECT triggerType, COUNT(*) as count FROM trigger_records GROUP BY triggerType ORDER BY count DESC")
    LiveData<List<TriggerCount>> getTriggerCounts();

    /** Get triggers in the 48h before a high-score analysis (for correlation) */
    @Query("SELECT * FROM trigger_records WHERE timestamp BETWEEN :start AND :end")
    List<TriggerRecord> getTriggersInRangeSync(long start, long end);

    /**
     * Helper class for trigger count aggregation.
     */
    class TriggerCount {
        public String triggerType;
        public int count;
    }
}
