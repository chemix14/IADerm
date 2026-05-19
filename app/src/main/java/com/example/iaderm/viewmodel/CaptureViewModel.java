package com.example.iaderm.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.iaderm.CaptureUiState;
import com.example.iaderm.StabilityDetector;
import com.example.iaderm.data.AnalysisRecord;
import com.example.iaderm.data.IADermRepository;

public class CaptureViewModel extends AndroidViewModel {

    private final IADermRepository repository;
    private final StabilityDetector stabilityDetector;

    private final MutableLiveData<CaptureUiState> uiState = new MutableLiveData<>(CaptureUiState.SEARCHING);
    private final MutableLiveData<Boolean> isLightOk = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isDistanceOk = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isStabilityOk = new MutableLiveData<>(false);
    
    private boolean isCapturing = false;

    public CaptureViewModel(@NonNull Application application) {
        super(application);
        repository = new IADermRepository(application);

        stabilityDetector = new StabilityDetector(application, isStable -> {
            if (!isCapturing) {
                isStabilityOk.postValue(isStable);
                updateUiState();
            }
        });
    }

    public void startDetectors() {
        stabilityDetector.start();
    }

    public void stopDetectors() {
        stabilityDetector.stop();
    }

    public LiveData<CaptureUiState> getUiState() {
        return uiState;
    }

    public LiveData<Boolean> getIsLightOk() {
        return isLightOk;
    }

    public LiveData<Boolean> getIsDistanceOk() {
        return isDistanceOk;
    }

    public LiveData<Boolean> getIsStabilityOk() {
        return isStabilityOk;
    }

    public void setLightOk(boolean ok) {
        if (isLightOk.getValue() != null && isLightOk.getValue() == ok) return;
        isLightOk.setValue(ok);
        updateUiState();
    }

    public void setDistanceOk(boolean ok) {
        if (isDistanceOk.getValue() != null && isDistanceOk.getValue() == ok) return;
        isDistanceOk.setValue(ok);
        updateUiState();
    }

    private void updateUiState() {
        if (isCapturing) {
            uiState.setValue(CaptureUiState.CAPTURING);
            return;
        }

        boolean light = Boolean.TRUE.equals(isLightOk.getValue());
        boolean distance = Boolean.TRUE.equals(isDistanceOk.getValue());
        boolean stability = Boolean.TRUE.equals(isStabilityOk.getValue());

        if (light && distance && stability) {
            uiState.setValue(CaptureUiState.READY);
        } else if (!light) {
            uiState.setValue(CaptureUiState.ADJUST_LIGHT);
        } else if (!distance) {
            uiState.setValue(CaptureUiState.ADJUST_DISTANCE);
        } else {
            uiState.setValue(CaptureUiState.SEARCHING);
        }
    }

    public void performCapture(int score, String diagnosis, String top3Data, String imagePath, IADermRepository.OnInsertCallback callback) {
        if (isCapturing) return;
        isCapturing = true;
        updateUiState();

        AnalysisRecord record = new AnalysisRecord();
        record.timestamp = System.currentTimeMillis();
        record.score = Math.max(0, Math.min(100, score));
        record.diagnosis = diagnosis;
        record.severity = AnalysisRecord.calculateSeverity(record.score);
        record.imagePath = imagePath != null ? imagePath : "";
        record.heatmapData = top3Data;
        record.notes = "";

        repository.insertAnalysis(record, callback);
    }
}
