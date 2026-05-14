package com.example.iaderm;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.iaderm.data.AnalysisRecord;
import com.example.iaderm.viewmodel.ResultsViewModel;
import com.google.android.material.chip.Chip;

/**
 * ResultsActivity — Displays the analysis results.
 *
 * Features:
 * - Animated score reveal (progress bar fills up)
 * - Heatmap overlay visualization (demo zones)
 * - Contextual education text based on severity
 * - Severity chip with appropriate color coding
 * - Medical disclaimer footer
 */
public class ResultsActivity extends AppCompatActivity {

    private ProgressBar progressScore;
    private TextView tvScore;
    private TextView tvDiagnosisLabel;
    private TextView tvTop3List;
    private TextView tvResultDescription;
    private TextView tvRecommendationList;
    private Chip chipSeverity;
    private ImageButton btnBack;
    private ImageButton btnSave;
    private com.google.android.material.button.MaterialButton btnBookAppointment;
    private ResultsViewModel viewModel;

    private int analysisScore = 42; // Default demo score
    private String diagnosis = "Desconocido";
    private String top3Data = "";
    private long analysisId = -1L;
    private AnalysisRecord currentRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        // Get score and diagnosis from intent
        boolean hasScore = getIntent().hasExtra(AppNavigator.EXTRA_SCORE);
        analysisId = getIntent().getLongExtra(AppNavigator.EXTRA_ANALYSIS_ID, -1L);
        analysisScore = getIntent().getIntExtra(AppNavigator.EXTRA_SCORE, 42);
        analysisScore = Math.max(0, Math.min(100, analysisScore));
        
        if (getIntent().hasExtra(AppNavigator.EXTRA_DIAGNOSIS)) {
            diagnosis = getIntent().getStringExtra(AppNavigator.EXTRA_DIAGNOSIS);
        }
        if (getIntent().hasExtra(AppNavigator.EXTRA_TOP3)) {
            top3Data = getIntent().getStringExtra(AppNavigator.EXTRA_TOP3);
        }

        // Bind views
        progressScore = findViewById(R.id.progressScore);
        tvScore = findViewById(R.id.tvScore);
        tvDiagnosisLabel = findViewById(R.id.tvDiagnosisLabel);
        tvTop3List = findViewById(R.id.tvTop3List);
        tvResultDescription = findViewById(R.id.tvResultDescription);
        tvRecommendationList = findViewById(R.id.tvRecommendationList);
        chipSeverity = findViewById(R.id.chipSeverity);
        btnBack = findViewById(R.id.btnBack);
        btnSave = findViewById(R.id.btnSave);
        btnBookAppointment = findViewById(R.id.btnBookAppointment);
        viewModel = new ViewModelProvider(this).get(ResultsViewModel.class);

        // Back navigation
        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> markAnalysisAsSaved());
        btnBookAppointment.setOnClickListener(v -> UiFeedback.shortMessage(this, R.string.feature_coming_soon));

        if (!hasScore) {
            UiFeedback.shortMessage(this, R.string.results_invalid_input);
        }

        // Populate results
        displayResults(analysisScore, diagnosis, top3Data);
        observeAnalysisRecord();
    }

    private void displayResults(int score, String diag, String top3) {
        String severityText;
        // 1. Animate score progress bar
        animateScoreBar(score);

        // 2. Set score text
        tvScore.setText(getString(R.string.results_score_of, score));
        
        // Update Diagnosis Label
        tvDiagnosisLabel.setText("Diagnóstico: " + diag);

        // Populate Top 3
        if (top3 != null && !top3.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            String[] parts = top3.split(";");
            for (int i = 0; i < parts.length; i++) {
                String[] pair = parts[i].split(":");
                if (pair.length == 2) {
                    sb.append(i + 1).append(". ").append(pair[0]).append(": ").append(pair[1]).append("%");
                    if (i < parts.length - 1) sb.append("\n");
                }
            }
            tvTop3List.setText(sb.toString());
        } else {
            tvTop3List.setText("No hay datos disponibles.");
        }

        // Set Dynamic Description based on Diagnosis
        if (diag.equals("Piel sana")) {
            tvResultDescription.setText("El modelo no detectó signos importantes de afecciones dermatológicas. Sigue manteniendo una buena rutina de cuidado de la piel.");
            tvRecommendationList.setText("• Uso diario de protector solar\n• Limpieza suave por la mañana y noche\n• Hidratación constante");
        } else if (diag.equals("Desconocido")) {
            tvResultDescription.setText("No se pudo clasificar con precisión. Por favor, toma otra fotografía con mejor iluminación.");
            tvRecommendationList.setText("• Asegúrate de estar en un lugar bien iluminado\n• Evita sombras fuertes en el rostro\n• Mantén el teléfono estable");
        } else {
            tvResultDescription.setText("El modelo ha detectado características visuales compatibles con " + diag + " con un grado de confianza de " + score + "%. Este resultado es sugerente y debe ser confirmado por un especialista.");
            tvRecommendationList.setText("• Agendar cita con un dermatólogo\n• Evitar rascar o irritar la zona afectada\n• Proteger la piel de la exposición solar directa\n• Evitar automedicación");
        }

        // 3. Determine severity and set chip
        if (score <= 35) {
            chipSeverity.setText("Confianza Baja");
            severityText = "Confianza Baja";
            chipSeverity.setChipBackgroundColorResource(R.color.severity_mild_bg);
            chipSeverity.setTextColor(getColor(R.color.severity_mild_text));
        } else if (score <= 65) {
            chipSeverity.setText("Confianza Media");
            severityText = "Confianza Media";
            chipSeverity.setChipBackgroundColorResource(R.color.severity_moderate_bg);
            chipSeverity.setTextColor(getColor(R.color.severity_moderate_text));
        } else {
            chipSeverity.setText("Confianza Alta");
            severityText = "Confianza Alta";
            chipSeverity.setChipBackgroundColorResource(R.color.severity_severe_bg);
            chipSeverity.setTextColor(getColor(R.color.severity_severe_text));
        }
        chipSeverity.setContentDescription(getString(R.string.cd_severity_indicator, severityText));
    }

    private void observeAnalysisRecord() {
        if (analysisId <= 0L) {
            return;
        }
        viewModel.getAnalysisById(analysisId).observe(this, record -> {
            if (record == null) {
                return;
            }
            currentRecord = record;
            int dbScore = Math.max(0, Math.min(100, record.score));
            String dbDiagnosis = (record.diagnosis != null) ? record.diagnosis : diagnosis;
            String dbTop3 = (record.heatmapData != null && !record.heatmapData.isEmpty()) ? record.heatmapData : top3Data;
            
            if (dbScore != analysisScore || !dbDiagnosis.equals(diagnosis) || !dbTop3.equals(top3Data)) {
                analysisScore = dbScore;
                diagnosis = dbDiagnosis;
                top3Data = dbTop3;
                displayResults(analysisScore, diagnosis, top3Data);
            }
        });
    }

    /**
     * Animate the progress bar filling up from 0 to the target score.
     * Creates a satisfying "reveal" effect.
     */
    private void animateScoreBar(int targetScore) {
        progressScore.setProgress(0);

        ValueAnimator animator = ValueAnimator.ofInt(0, targetScore);
        animator.setDuration(1200);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.setStartDelay(400); // Wait for screen to settle

        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            progressScore.setProgress(value);
            tvScore.setText(getString(R.string.results_score_of, value));

            // Update progress bar tint based on current value
            if (value <= 35) {
                progressScore.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(getColor(R.color.success)));
            } else if (value <= 65) {
                progressScore.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(getColor(R.color.warning)));
            } else {
                progressScore.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(getColor(R.color.error)));
            }
        });

        animator.start();
    }

    private void markAnalysisAsSaved() {
        if (currentRecord == null) {
            UiFeedback.shortMessage(this, R.string.results_saved_message);
            return;
        }
        currentRecord.notes = "saved_by_user";
        viewModel.updateAnalysis(currentRecord);
        UiFeedback.shortMessage(this, R.string.results_updated_message);
    }
}
