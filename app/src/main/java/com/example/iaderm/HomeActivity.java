package com.example.iaderm;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.iaderm.data.AnalysisRecord;
import com.example.iaderm.viewmodel.HomeViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * HomeActivity — The main dashboard of IADerm.
 *
 * Features:
 * - Time-based personalized greeting
 * - Last analysis summary card
 * - Quick action cards (History, Triggers)
 * - Tip of the day
 * - Extended FAB to start analysis
 * - Bottom navigation
 */
public class HomeActivity extends AppCompatActivity {

    private TextView tvGreeting;
    private ExtendedFloatingActionButton fabAnalyze;
    private BottomNavigationView bottomNav;
    private MaterialCardView cardHistory;
    private MaterialCardView cardTriggers;
    private MaterialCardView cardLastAnalysis;
    private TextView tvLastAnalysisDate;
    private TextView tvScoreLabel;
    private ProgressBar progressScore;
    private View viewSeverityDot;
    private MaterialButton btnViewDetails;
    private HomeViewModel viewModel;
    private AnalysisRecord latestRecord;
    private View tutorialOverlayHome;
    private View tvTutorialArrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Bind views
        tvGreeting = findViewById(R.id.tvGreeting);
        fabAnalyze = findViewById(R.id.fabAnalyze);
        bottomNav = findViewById(R.id.bottomNav);
        cardHistory = findViewById(R.id.cardHistory);
        cardTriggers = findViewById(R.id.cardTriggers);
        cardLastAnalysis = findViewById(R.id.cardLastAnalysis);
        tvLastAnalysisDate = findViewById(R.id.tvLastAnalysisDate);
        tvScoreLabel = findViewById(R.id.tvScoreLabel);
        progressScore = findViewById(R.id.progressScore);
        viewSeverityDot = findViewById(R.id.viewSeverityDot);
        btnViewDetails = findViewById(R.id.btnViewDetails);
        tutorialOverlayHome = findViewById(R.id.tutorialOverlayHome);
        tvTutorialArrow = findViewById(R.id.tvTutorialArrow);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // Set greeting based on time of day
        setTimeBasedGreeting();

        // FAB click → open CaptureActivity
        fabAnalyze.setOnClickListener(v -> {
            AppNavigator.openCapture(this);
        });

        // Quick action cards
        cardHistory.setOnClickListener(v -> AppNavigator.openHistory(this));
        cardTriggers.setOnClickListener(v -> AppNavigator.openTriggers(this));

        // Last analysis card → view details
        cardLastAnalysis.setOnClickListener(v -> {
            if (latestRecord != null) {
                AppNavigator.openResults(this, latestRecord.id, latestRecord.score, latestRecord.diagnosis, latestRecord.heatmapData);
            } else {
                AppNavigator.openResults(this, 42, "Desconocido", "");
            }
        });
        btnViewDetails.setOnClickListener(v -> {
            if (latestRecord != null) {
                AppNavigator.openResults(this, latestRecord.id, latestRecord.score, latestRecord.diagnosis, latestRecord.heatmapData);
            } else {
                AppNavigator.openResults(this, 42, "Desconocido", "");
            }
        });

        // Bottom Navigation
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_history) {
                AppNavigator.openHistory(this);
                return true;
            } else if (itemId == R.id.nav_capture) {
                AppNavigator.openCapture(this);
                return true;
            } else if (itemId == R.id.nav_triggers) {
                AppNavigator.openTriggers(this);
                return true;
            } else if (itemId == R.id.nav_ai_chat) {
                UiFeedback.shortMessage(this, R.string.feature_coming_soon);
                return true;
            }
            return false;
        });

        // Entrance animations for cards
        animateCardsEntrance();
        observeLatestAnalysis();

        // FTU (First Time Use) Tutorial for Home
        android.content.SharedPreferences prefs = getSharedPreferences("iaderm_prefs", MODE_PRIVATE);
        if (!prefs.getBoolean("tutorial_home_shown", false)) {
            tutorialOverlayHome.setVisibility(View.VISIBLE);
            
            // Arrow bounce animation
            android.animation.ObjectAnimator bounceAnim = android.animation.ObjectAnimator.ofFloat(tvTutorialArrow, "translationY", 0f, 40f);
            bounceAnim.setDuration(600);
            bounceAnim.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            bounceAnim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            bounceAnim.start();

            findViewById(R.id.btnEntendidoHome).setOnClickListener(v -> {
                tutorialOverlayHome.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            tutorialOverlayHome.setVisibility(View.GONE);
                            bounceAnim.cancel();
                        })
                        .start();
                prefs.edit().putBoolean("tutorial_home_shown", true).apply();
            });
        }
    }

    /**
     * Set greeting text based on the current hour.
     */
    private void setTimeBasedGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour < 12) {
            tvGreeting.setText(R.string.home_greeting); // "Buenos días"
        } else if (hour < 18) {
            tvGreeting.setText(R.string.home_greeting_afternoon); // "Buenas tardes"
        } else {
            tvGreeting.setText(R.string.home_greeting_evening); // "Buenas noches"
        }
    }

    /**
     * Animate cards sliding up on first load.
     */
    private void animateCardsEntrance() {
        float offset = 80f;
        long baseDuration = 400;

        // Stagger animations
        cardLastAnalysis.setAlpha(0f);
        cardLastAnalysis.setTranslationY(offset);
        cardLastAnalysis.animate()
                .alpha(1f).translationY(0f)
                .setDuration(baseDuration)
                .setStartDelay(100)
                .start();

        cardHistory.setAlpha(0f);
        cardHistory.setTranslationY(offset);
        cardHistory.animate()
                .alpha(1f).translationY(0f)
                .setDuration(baseDuration)
                .setStartDelay(200)
                .start();

        cardTriggers.setAlpha(0f);
        cardTriggers.setTranslationY(offset);
        cardTriggers.animate()
                .alpha(1f).translationY(0f)
                .setDuration(baseDuration)
                .setStartDelay(300)
                .start();

        fabAnalyze.setAlpha(0f);
        fabAnalyze.setTranslationY(40f);
        fabAnalyze.animate()
                .alpha(1f).translationY(0f)
                .setDuration(baseDuration)
                .setStartDelay(500)
                .start();
    }

    private void observeLatestAnalysis() {
        viewModel.getLatestAnalysis().observe(this, record -> {
            latestRecord = record;
            if (record == null) {
                tvLastAnalysisDate.setText(R.string.home_no_analysis);
                tvScoreLabel.setText(R.string.home_start_first);
                progressScore.setProgress(0);
                viewSeverityDot.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(getColor(R.color.outline)));
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
        if (score <= 35) return getColor(R.color.success);
        if (score <= 65) return getColor(R.color.warning);
        return getColor(R.color.error);
    }

    private String getSeverityLabel(int score) {
        if (score <= 35) return getString(R.string.severity_mild);
        if (score <= 65) return getString(R.string.severity_moderate);
        return getString(R.string.severity_severe);
    }
}
