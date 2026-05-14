package com.example.iaderm.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.iaderm.PdfReportGenerator;
import com.example.iaderm.data.AnalysisRecord;
import com.example.iaderm.data.IADermRepository;
import com.example.iaderm.data.TriggerRecord;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryViewModel extends AndroidViewModel {

    private final IADermRepository repository;
    private final MutableLiveData<Long> filterTimestamp = new MutableLiveData<>(0L); // 0 means all time
    private final LiveData<List<AnalysisRecord>> allAnalyses;
    private final MutableLiveData<String> pdfGeneratedPath = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        repository = new IADermRepository(application);
        allAnalyses = Transformations.switchMap(filterTimestamp, timestamp -> {
            if (timestamp <= 0L) {
                return repository.getAllAnalyses();
            } else {
                return repository.getAnalysesSince(timestamp);
            }
        });
    }

    public LiveData<List<AnalysisRecord>> getAllAnalyses() {
        return allAnalyses;
    }

    public void setFilterDays(int days) {
        if (days <= 0) {
            filterTimestamp.setValue(0L);
        } else {
            long millis = days * 24L * 60 * 60 * 1000;
            filterTimestamp.setValue(System.currentTimeMillis() - millis);
        }
    }

    public LiveData<String> getPdfGeneratedPath() {
        return pdfGeneratedPath;
    }

    public void generatePdf() {
        executorService.execute(() -> {
            List<AnalysisRecord> currentRecords = allAnalyses.getValue();
            // In a real scenario we might want to observe triggers, but for this demo
            // we will pass null or fetch synchronously if we added a sync method.
            try {
                String path = PdfReportGenerator.generate(getApplication(), currentRecords, null);
                pdfGeneratedPath.postValue(path);
            } catch (Exception e) {
                e.printStackTrace();
                pdfGeneratedPath.postValue(null);
            }
        });
    }

    public void resetPdfPath() {
        pdfGeneratedPath.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
