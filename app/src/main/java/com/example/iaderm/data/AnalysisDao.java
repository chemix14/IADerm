package com.example.iaderm.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * AnalysisDao — Data Access Object for analysis records.
 *
 * Provides reactive LiveData queries for automatic UI updates
 * when the database changes.
 */
@Dao
public interface AnalysisDao {

    @Insert
    long insert(AnalysisRecord record);

    @Update
    void update(AnalysisRecord record);

    @Delete
    void delete(AnalysisRecord record);

    /** Get all records, newest first */
    @Query("SELECT * FROM analysis_records ORDER BY timestamp DESC")
    LiveData<List<AnalysisRecord>> getAllRecords();

    /** Get the N most recent records */
    @Query("SELECT * FROM analysis_records ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<AnalysisRecord>> getRecentRecords(int limit);

    /** Get a single record by ID */
    @Query("SELECT * FROM analysis_records WHERE id = :id")
    LiveData<AnalysisRecord> getRecordById(long id);

    /** Get the most recent single record (for Home dashboard) */
    @Query("SELECT * FROM analysis_records ORDER BY timestamp DESC LIMIT 1")
    LiveData<AnalysisRecord> getLatestRecord();

    /** Get records in the last N days (for trend chart) */
    @Query("SELECT * FROM analysis_records WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    LiveData<List<AnalysisRecord>> getRecordsSince(long sinceTimestamp);

    /** Count total analyses */
    @Query("SELECT COUNT(*) FROM analysis_records")
    LiveData<Integer> getTotalCount();

    /** Get average score over the last N days */
    @Query("SELECT AVG(score) FROM analysis_records WHERE timestamp >= :sinceTimestamp")
    LiveData<Float> getAverageScoreSince(long sinceTimestamp);

    /** Synchronous insert for background threads */
    @Insert
    long insertSync(AnalysisRecord record);

    /** Synchronous query for two records to compare */
    @Query("SELECT * FROM analysis_records WHERE id IN (:id1, :id2) ORDER BY timestamp ASC")
    List<AnalysisRecord> getTwoRecordsSync(long id1, long id2);
}
