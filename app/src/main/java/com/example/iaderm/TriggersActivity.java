package com.example.iaderm;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.example.iaderm.viewmodel.TriggersViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

public class TriggersActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ChipGroup cgTriggerTypes;
    private Slider sliderSeverity;
    private TextView tvSeverityValue;
    private TextInputEditText etNotes;
    private MaterialButton btnSaveTrigger;

    private TriggersViewModel viewModel;
    private long analysisId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_triggers);

        analysisId = getIntent().getLongExtra(AppNavigator.EXTRA_ANALYSIS_ID, -1L);

        viewModel = new ViewModelProvider(this).get(TriggersViewModel.class);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        cgTriggerTypes = findViewById(R.id.cgTriggerTypes);
        sliderSeverity = findViewById(R.id.sliderSeverity);
        tvSeverityValue = findViewById(R.id.tvSeverityValue);
        etNotes = findViewById(R.id.etNotes);
        btnSaveTrigger = findViewById(R.id.btnSaveTrigger);

        sliderSeverity.addOnChangeListener((slider, value, fromUser) -> {
            tvSeverityValue.setText(String.valueOf((int) value));
        });

        btnSaveTrigger.setOnClickListener(v -> saveTrigger());

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getIsSaving().observe(this, isSaving -> {
            btnSaveTrigger.setEnabled(!isSaving);
            if (isSaving) {
                btnSaveTrigger.setText(R.string.capture_processing);
            } else {
                btnSaveTrigger.setText(R.string.action_save);
            }
        });

        viewModel.getSaveSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                UiFeedback.shortMessage(this, R.string.results_saved_message);
                viewModel.resetSaveSuccess();
                finish();
            }
        });
    }

    private void saveTrigger() {
        int checkedChipId = cgTriggerTypes.getCheckedChipId();
        if (checkedChipId == View.NO_ID) {
            UiFeedback.shortMessage(this, R.string.error_invalid_input);
            return;
        }

        Chip selectedChip = findViewById(checkedChipId);
        String type = selectedChip.getText().toString();
        int severity = (int) sliderSeverity.getValue();
        String notes = etNotes.getText() != null ? etNotes.getText().toString() : "";

        viewModel.saveTrigger(analysisId, type, severity, notes);
    }
}
