package com.example.iaderm.data;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * IADermRepository — Single source of truth for all data operations.
 *
 * Mediates between ViewModels and the Room database.
 * All write operations run on a background thread via ExecutorService.
 */
public class IADermRepository {

    private final AnalysisDao analysisDao;
    private final TriggerDao triggerDao;
    private final ExecutorService executor;

    public IADermRepository(Application application) {
        IADermDatabase db = IADermDatabase.getInstance(application);
        analysisDao = db.analysisDao();
        triggerDao = db.triggerDao();
        executor = Executors.newFixedThreadPool(2);
    }

    // ═══════════════════════════════════════════════════════
    // Analysis Records
    // ═══════════════════════════════════════════════════════

    public LiveData<List<AnalysisRecord>> getAllAnalyses() {
        return analysisDao.getAllRecords();
    }

    public LiveData<List<AnalysisRecord>> getRecentAnalyses(int limit) {
        return analysisDao.getRecentRecords(limit);
    }

    public LiveData<AnalysisRecord> getLatestAnalysis() {
        return analysisDao.getLatestRecord();
    }

    public LiveData<AnalysisRecord> getAnalysisById(long id) {
        return analysisDao.getRecordById(id);
    }

    public LiveData<List<AnalysisRecord>> getAnalysesSince(long sinceTimestamp) {
        return analysisDao.getRecordsSince(sinceTimestamp);
    }

    public LiveData<Integer> getTotalAnalysisCount() {
        return analysisDao.getTotalCount();
    }

    public LiveData<Float> getAverageScoreSince(long sinceTimestamp) {
        return analysisDao.getAverageScoreSince(sinceTimestamp);
    }

    public void insertAnalysis(AnalysisRecord record, OnInsertCallback callback) {
        executor.execute(() -> {
            long id = -1L;
            try {
                id = analysisDao.insertSync(record);
            } catch (Exception ignored) {
                // The caller handles fallback UI if insert fails.
            }
            if (callback != null) callback.onInserted(id);
        });
    }

    public void deleteAnalysis(AnalysisRecord record) {
        executor.execute(() -> analysisDao.delete(record));
    }

    public void updateAnalysis(AnalysisRecord record) {
        executor.execute(() -> analysisDao.update(record));
    }

    // ═══════════════════════════════════════════════════════
    // Trigger Records
    // ═══════════════════════════════════════════════════════

    public LiveData<List<TriggerRecord>> getAllTriggers() {
        return triggerDao.getAllTriggers();
    }

    public LiveData<List<TriggerRecord>> getTriggersSince(long sinceTimestamp) {
        return triggerDao.getTriggersSince(sinceTimestamp);
    }

    public LiveData<List<TriggerDao.TriggerCount>> getTriggerCounts() {
        return triggerDao.getTriggerCounts();
    }

    public void insertTrigger(TriggerRecord record) {
        executor.execute(() -> triggerDao.insert(record));
    }

    public void deleteTrigger(TriggerRecord record) {
        executor.execute(() -> triggerDao.delete(record));
    }

    // ═══════════════════════════════════════════════════════
    // Correlation: Find triggers near a flare-up
    // ═══════════════════════════════════════════════════════

    /**
     * Get triggers logged within 48 hours before an analysis timestamp.
     * This enables the "trigger correlation" insight feature.
     */
    public void getTriggersBeforeAnalysis(long analysisTimestamp, OnTriggersCallback callback) {
        executor.execute(() -> {
            long hours48 = 48L * 60 * 60 * 1000;
            List<TriggerRecord> triggers = triggerDao.getTriggersInRangeSync(
                    analysisTimestamp - hours48, analysisTimestamp);
            if (callback != null) callback.onResult(triggers);
        });
    }

    // ── Callbacks ──

    public interface OnInsertCallback {
        void onInserted(long id);
    }

    public interface OnTriggersCallback {
        void onResult(List<TriggerRecord> triggers);
    }
}
