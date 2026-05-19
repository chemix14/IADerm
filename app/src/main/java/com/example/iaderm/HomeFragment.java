package com.example.iaderm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.iaderm.data.AnalysisRecord;
import com.example.iaderm.viewmodel.HomeViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class HomeFragment extends Fragment {

    private TextView tvGreeting;
    private MaterialCardView cardHistory;
    private MaterialCardView cardTriggers;
    private MaterialCardView cardLastAnalysis;
    private TextView tvLastAnalysisDate;
    private TextView tvScoreLabel;
    private ProgressBar progressScore;
    private View viewSeverityDot;
    private MaterialButton btnViewDetails;
    private TextView tvTipContent;
    private TextView tvTipCategory;
    private HomeViewModel viewModel;
    private AnalysisRecord latestRecord;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Bind views
        tvGreeting = view.findViewById(R.id.tvGreeting);
        cardHistory = view.findViewById(R.id.cardHistory);
        cardTriggers = view.findViewById(R.id.cardTriggers);
        cardLastAnalysis = view.findViewById(R.id.cardLastAnalysis);
        tvLastAnalysisDate = view.findViewById(R.id.tvLastAnalysisDate);
        tvScoreLabel = view.findViewById(R.id.tvScoreLabel);
        progressScore = view.findViewById(R.id.progressScore);
        viewSeverityDot = view.findViewById(R.id.viewSeverityDot);
        btnViewDetails = view.findViewById(R.id.btnViewDetails);
        tvTipContent = view.findViewById(R.id.tvTipContent);
        tvTipCategory = view.findViewById(R.id.tvTipCategory);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setTimeBasedGreeting();
        loadRandomTip();

        // Quick action cards
        cardHistory.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToTab(R.id.nav_history);
            }
        });
        cardTriggers.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToTab(R.id.nav_triggers);
            }
        });

        // Last analysis card → view details
        View.OnClickListener openDetails = v -> {
            if (latestRecord != null) {
                AppNavigator.openResults(getContext(), latestRecord.id, latestRecord.score, latestRecord.diagnosis, latestRecord.heatmapData);
            } else {
                AppNavigator.openResults(getContext(), 42, "Desconocido", "");
            }
        };
        cardLastAnalysis.setOnClickListener(openDetails);
        btnViewDetails.setOnClickListener(openDetails);

        // Tap tip card to load another random tip
        View cardTip = view.findViewById(R.id.cardTip);
        if (cardTip != null) {
            cardTip.setOnClickListener(v -> loadRandomTip());
        }

        observeLatestAnalysis();

        return view;
    }

    private void setTimeBasedGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            tvGreeting.setText(R.string.home_greeting);
        } else if (hour < 18) {
            tvGreeting.setText(R.string.home_greeting_afternoon);
        } else {
            tvGreeting.setText(R.string.home_greeting_evening);
        }
    }

    private void loadRandomTip() {
        if (getContext() == null) return;
        String[] tips = getResources().getStringArray(R.array.rosacea_tips);
        if (tips.length > 0) {
            int index = new Random().nextInt(tips.length);
            String tip = tips[index];
            tvTipContent.setText(tip);

            // Determine category based on index range
            String category;
            if (index < 15) category = "Protección Solar";
            else if (index < 30) category = "Alimentación";
            else if (index < 50) category = "Cuidado de Piel";
            else if (index < 65) category = "Bienestar";
            else if (index < 75) category = "Ejercicio";
            else if (index < 90) category = "Clima y Ambiente";
            else if (index < 100) category = "Maquillaje";
            else category = "Salud General";

            if (tvTipCategory != null) {
                tvTipCategory.setText(category);
            }

            // Subtle animation
            tvTipContent.setAlpha(0f);
            tvTipContent.animate().alpha(1f).setDuration(400).start();
        }
    }

    private void observeLatestAnalysis() {
        viewModel.getLatestAnalysis().observe(getViewLifecycleOwner(), record -> {
            latestRecord = record;
            if (record == null) {
                tvLastAnalysisDate.setText(R.string.home_no_analysis);
                tvScoreLabel.setText(R.string.home_start_first);
                progressScore.setProgress(0);
                viewSeverityDot.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(getContext().getColor(R.color.outline)));
                return;
            }

            int safeScore = Math.max(0, Math.min(100, record.score));
            progressScore.setProgress(safeScore);
            tvScoreLabel.setText(getString(R.string.home_score_label, safeScore));

            long now = System.currentTimeMillis();
            long daysAgo = Math.max(0, TimeUnit.MILLISECONDS.toDays(now - record.timestamp));
            String severityText = getSeverityLabel(record.score);
            tvLastAnalysisDate.setText(getString(R.string.home_last_analysis_summary, daysAgo, severityText));

            viewSeverityDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getSeverityColor(record.score)));
        });
    }

    private int getSeverityColor(int score) {
        if (score <= 35) return getContext().getColor(R.color.success);
        if (score <= 65) return getContext().getColor(R.color.warning);
        return getContext().getColor(R.color.error);
    }

    private String getSeverityLabel(int score) {
        if (score <= 35) return getString(R.string.severity_mild);
        if (score <= 65) return getString(R.string.severity_moderate);
        return getString(R.string.severity_severe);
    }
}
