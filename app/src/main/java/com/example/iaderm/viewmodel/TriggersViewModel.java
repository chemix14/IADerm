package com.example.iaderm.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.iaderm.data.IADermDatabase;
import com.example.iaderm.data.TriggerDao;
import com.example.iaderm.data.TriggerRecord;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TriggersViewModel extends AndroidViewModel {

    private final TriggerDao triggerDao;
    private final ExecutorService executorService;
    private final MutableLiveData<Boolean> isSaving = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();

    public TriggersViewModel(@NonNull Application application) {
        super(application);
        IADermDatabase db = IADermDatabase.getInstance(application);
        triggerDao = db.triggerDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<TriggerRecord>> getTriggersForAnalysis(long analysisId) {
        return triggerDao.getTriggersForAnalysis(analysisId);
    }

    public LiveData<List<TriggerRecord>> getAllTriggers() {
        return triggerDao.getAllTriggers();
    }

    public LiveData<Boolean> getIsSaving() {
        return isSaving;
    }

    public LiveData<Boolean> getSaveSuccess() {
        return saveSuccess;
    }

    public void saveTrigger(long analysisId, String triggerType, int intensity, String description) {
        isSaving.setValue(true);
        executorService.execute(() -> {
            TriggerRecord record = new TriggerRecord();
            record.analysisId = analysisId > 0 ? analysisId : 0;
            record.timestamp = System.currentTimeMillis();
            record.triggerType = triggerType;
            record.intensity = intensity;
            record.description = description;

            triggerDao.insert(record);

            isSaving.postValue(false);
            saveSuccess.postValue(true);
        });
    }

    public void resetSaveSuccess() {
        saveSuccess.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
