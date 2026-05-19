package com.example.iaderm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

import com.example.iaderm.data.TriggerRecord;
import com.example.iaderm.viewmodel.TriggersViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TriggersFragment extends Fragment {

    private TriggersViewModel viewModel;
    private ChipGroup cgTriggerType;
    private Slider sliderIntensity;
    private TextInputEditText etNotes;
    private MaterialButton btnSaveTrigger;
    private LinearLayout historyContainer;
    private TextView tvEmptyHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_triggers, container, false);

        viewModel = new ViewModelProvider(this).get(TriggersViewModel.class);

        cgTriggerType = view.findViewById(R.id.cgTriggerType);
        sliderIntensity = view.findViewById(R.id.sliderIntensity);
        etNotes = view.findViewById(R.id.etNotes);
        btnSaveTrigger = view.findViewById(R.id.btnSaveTrigger);
        historyContainer = view.findViewById(R.id.triggerHistoryContainer);
        tvEmptyHistory = view.findViewById(R.id.tvEmptyHistory);

        btnSaveTrigger.setOnClickListener(v -> saveTrigger());

        // Observe saved triggers and display them
        viewModel.getAllTriggers().observe(getViewLifecycleOwner(), triggers -> {
            historyContainer.removeAllViews();
            if (triggers == null || triggers.isEmpty()) {
                tvEmptyHistory.setVisibility(View.VISIBLE);
            } else {
                tvEmptyHistory.setVisibility(View.GONE);
                for (TriggerRecord record : triggers) {
                    historyContainer.addView(createTriggerCard(record));
                }
            }
        });

        // Observe save success
        viewModel.getSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                UiFeedback.shortMessage(getContext(), R.string.trigger_saved);
                resetForm();
                viewModel.resetSaveSuccess();
            }
        });

        return view;
    }

    private void saveTrigger() {
        // Get selected trigger type
        int checkedId = cgTriggerType.getCheckedChipId();
        if (checkedId == View.NO_ID) {
            UiFeedback.shortMessage(getContext(), R.string.error_invalid_input);
            return;
        }

        Chip selectedChip = cgTriggerType.findViewById(checkedId);
        String triggerType = selectedChip.getText().toString();

        int intensity = (int) sliderIntensity.getValue();
        String notes = "";
        if (etNotes != null && etNotes.getText() != null) {
            notes = etNotes.getText().toString().trim();
        }

        viewModel.saveTrigger(0, triggerType, intensity, notes);
    }

    private void resetForm() {
        cgTriggerType.clearCheck();
        sliderIntensity.setValue(0);
        if (etNotes != null) {
            etNotes.setText("");
        }
    }

    private View createTriggerCard(TriggerRecord record) {
        if (getContext() == null) return new View(getContext());

        // Main card
        MaterialCardView card = new MaterialCardView(getContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(12));
        card.setLayoutParams(cardParams);
        card.setCardElevation(dpToPx(2));
        card.setRadius(dpToPx(16));
        card.setCardBackgroundColor(getResources().getColor(R.color.surface_variant, null));
        card.setContentPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Content layout
        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // Row 1: Emoji + type + date
        LinearLayout row1 = new LinearLayout(getContext());
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        row1.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Type label with emoji
        String emoji = TriggerRecord.getEmojiForType(record.triggerType);
        TextView tvType = new TextView(getContext());
        tvType.setText(emoji + " " + record.triggerType);
        tvType.setTextSize(16f);
        tvType.setTypeface(null, android.graphics.Typeface.BOLD);
        tvType.setTextColor(getResources().getColor(R.color.on_surface, null));
        LinearLayout.LayoutParams typeParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        tvType.setLayoutParams(typeParams);
        row1.addView(tvType);

        // Date
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault());
        TextView tvDate = new TextView(getContext());
        tvDate.setText(sdf.format(new Date(record.timestamp)));
        tvDate.setTextSize(12f);
        tvDate.setTextColor(getResources().getColor(R.color.on_surface_variant, null));
        row1.addView(tvDate);

        content.addView(row1);

        // Row 2: Intensity bar
        LinearLayout row2 = new LinearLayout(getContext());
        row2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams row2Params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        row2Params.setMargins(0, dpToPx(8), 0, 0);
        row2.setLayoutParams(row2Params);
        row2.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView tvIntLabel = new TextView(getContext());
        tvIntLabel.setText("Intensidad: ");
        tvIntLabel.setTextSize(13f);
        tvIntLabel.setTextColor(getResources().getColor(R.color.on_surface_variant, null));
        row2.addView(tvIntLabel);

        // Visual intensity dots
        StringBuilder intensityDots = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            intensityDots.append(i <= record.intensity ? "●" : "○");
            if (i < 5) intensityDots.append(" ");
        }
        TextView tvIntDots = new TextView(getContext());
        tvIntDots.setText(intensityDots.toString());
        tvIntDots.setTextSize(14f);
        int dotColor;
        if (record.intensity <= 2) {
            dotColor = getResources().getColor(R.color.severity_mild, null);
        } else if (record.intensity <= 3) {
            dotColor = getResources().getColor(R.color.severity_moderate, null);
        } else {
            dotColor = getResources().getColor(R.color.severity_severe, null);
        }
        tvIntDots.setTextColor(dotColor);
        row2.addView(tvIntDots);

        content.addView(row2);

        // Row 3: Notes (if any)
        if (record.description != null && !record.description.isEmpty()) {
            TextView tvNotes = new TextView(getContext());
            tvNotes.setText("📝 " + record.description);
            tvNotes.setTextSize(13f);
            tvNotes.setTextColor(getResources().getColor(R.color.on_surface_variant, null));
            LinearLayout.LayoutParams notesParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            notesParams.setMargins(0, dpToPx(6), 0, 0);
            tvNotes.setLayoutParams(notesParams);
            content.addView(tvNotes);
        }

        // Delete button
        TextView tvDelete = new TextView(getContext());
        tvDelete.setText("Eliminar");
        tvDelete.setTextSize(12f);
        tvDelete.setTextColor(getResources().getColor(R.color.severity_severe, null));
        tvDelete.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        deleteParams.setMargins(0, dpToPx(8), 0, 0);
        deleteParams.gravity = android.view.Gravity.END;
        tvDelete.setLayoutParams(deleteParams);
        tvDelete.setOnClickListener(v -> {
            viewModel.deleteTrigger(record);
        });
        content.addView(tvDelete);

        card.addView(content);
        return card;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
