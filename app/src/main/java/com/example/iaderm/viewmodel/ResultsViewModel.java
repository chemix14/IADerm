package com.example.iaderm.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.iaderm.data.AnalysisRecord;
import com.example.iaderm.data.IADermRepository;

public class ResultsViewModel extends AndroidViewModel {

    private final IADermRepository repository;

    public ResultsViewModel(@NonNull Application application) {
        super(application);
        repository = new IADermRepository(application);
    }

    public LiveData<AnalysisRecord> getAnalysisById(long id) {
        return repository.getAnalysisById(id);
    }

    public void updateAnalysis(AnalysisRecord record) {
        repository.updateAnalysis(record);
    }
}
