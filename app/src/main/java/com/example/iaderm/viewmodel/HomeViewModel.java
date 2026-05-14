package com.example.iaderm.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.iaderm.data.AnalysisRecord;
import com.example.iaderm.data.IADermRepository;

public class HomeViewModel extends AndroidViewModel {

    private final IADermRepository repository;
    private final LiveData<AnalysisRecord> latestAnalysis;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = new IADermRepository(application);
        latestAnalysis = repository.getLatestAnalysis();
    }

    public LiveData<AnalysisRecord> getLatestAnalysis() {
        return latestAnalysis;
    }
}
