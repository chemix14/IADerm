package com.example.iaderm;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.iaderm.data.AnalysisRecord;
import com.example.iaderm.viewmodel.ResultsViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.CircularProgressIndicator;

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

    private CircularProgressIndicator progressScore;
    private TextView tvScore;
    private TextView tvDiagnosisLabel;
    private TextView tvTop3List;
    private TextView tvResultDescription;
    private TextView tvRecommendationList;
    private Chip chipSeverity;
    private com.google.android.material.button.MaterialButton btnBack;
    private com.google.android.material.button.MaterialButton btnSave;
    private com.google.android.material.button.MaterialButton btnBookAppointment;
    private android.view.View cardSuggestProfile;
    private com.google.android.material.button.MaterialButton btnGoToProfile;
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
        cardSuggestProfile = findViewById(R.id.cardSuggestProfile);
        btnGoToProfile = findViewById(R.id.btnGoToProfile);
        
        com.google.android.material.button.MaterialButton btnAIChat = findViewById(R.id.btnAIChat);
        
        viewModel = new ViewModelProvider(this).get(ResultsViewModel.class);

        // Back navigation
        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> markAnalysisAsSaved());
        btnGoToProfile.setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, QuestionnaireActivity.class));
        });
        
        btnAIChat.setOnClickListener(v -> {
            // Guardar el score en SharedPreferences para que el Fragmento pueda leerlo
            android.content.SharedPreferences prefs = getSharedPreferences("iaderm_prefs", MODE_PRIVATE);
            prefs.edit().putInt("latest_score", analysisScore).apply();
            
            android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
            intent.putExtra("OPEN_TAB", R.id.nav_ai_chat);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        btnBookAppointment.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse("geo:0,0?q=dermatologos+cercanos"));
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // Fallback to web browser if Maps is not installed
                intent.setData(android.net.Uri.parse("https://www.google.com/maps/search/dermatologos+cercanos"));
                startActivity(intent);
            }
        });

        if (!hasScore) {
            UiFeedback.shortMessage(this, R.string.results_invalid_input);
        }

        // Populate results
        displayResults(analysisScore, diagnosis, top3Data);
        observeAnalysisRecord();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if user has completed the medical profile
        android.content.SharedPreferences prefs = getSharedPreferences("iaderm_prefs", MODE_PRIVATE);
        boolean hasProfile = prefs.getBoolean("has_completed_questionnaire", false);
        if (hasProfile) {
            cardSuggestProfile.setVisibility(android.view.View.GONE);
        } else {
            cardSuggestProfile.setVisibility(android.view.View.VISIBLE);
        }
    }

    private void displayResults(int score, String diag, String top3) {
        String severityText;
        // 1. Animate score progress bar
        animateScoreBar(score);

        // 2. Set score text
        tvScore.setText(getString(R.string.results_score_of, score));
        
        // Update Diagnosis Label
        tvDiagnosisLabel.setText("Nivel de " + diag);

        // Populate Top 3 and look for Alert
        String alertMessage = null;
        if (top3 != null && !top3.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            String[] parts = top3.split(";");
            for (String part : parts) {
                if (part.startsWith("ALERTA:")) {
                    alertMessage = part;
                } else {
                    String[] pair = part.split(":");
                    if (pair.length == 2) {
                        sb.append("• ").append(pair[0]).append(": ").append(pair[1]).append("%\n");
                    }
                }
            }
            tvTop3List.setText("Otros análisis del modelo:\n" + sb.toString().trim());
        } else {
            tvTop3List.setText("No hay datos adicionales disponibles.");
        }

        // Set Dynamic Description based on Diagnosis & Alert
        if (alertMessage != null) {
            tvResultDescription.setText("Compatibilidad con " + diag + ": " + score + "%.\n\n⚠️ " + alertMessage);
            tvRecommendationList.setText("• Agendar cita médica de inmediato\n• Evitar automedicación bajo cualquier circunstancia\n• Usar protector solar dermatológico\n• Limpieza facial suave");
        } else if (score < 30) {
            tvResultDescription.setText("No se detectaron niveles preocupantes de Rosácea. Sigue manteniendo una buena rutina de cuidado de la piel.");
            tvRecommendationList.setText("• Uso diario de protector solar\n• Limpieza suave por la mañana y noche\n• Hidratación constante");
        } else {
            tvResultDescription.setText("El modelo detectó una coincidencia del " + score + "% con características visuales de la Rosácea. Este resultado es sugerente y debe ser confirmado por un especialista.");
            tvRecommendationList.setText("• Evitar exposición solar directa\n• Buscar a un dermatólogo cercano\n• Evitar estrés y cambios bruscos de temperatura\n• No utilizar productos irritantes");
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
        if (analysisId <= 0) return;
        viewModel.getAnalysisById(analysisId).observe(this, record -> {
            if (record != null) {
                currentRecord = record;
                displayResults(record.score, record.diagnosis, record.heatmapData);
                
                // Cargar la imagen local de forma segura
                if (record.imagePath != null && !record.imagePath.isEmpty()) {
                    com.google.android.material.imageview.ShapeableImageView ivPhoto = findViewById(R.id.ivAnalyzedPhoto);
                    if (ivPhoto != null) {
                        try {
                            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(record.imagePath);
                            if (bitmap != null) {
                                ivPhoto.setImageBitmap(bitmap);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
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
                progressScore.setIndicatorColor(getColor(R.color.success));
            } else if (value <= 65) {
                progressScore.setIndicatorColor(getColor(R.color.warning));
            } else {
                progressScore.setIndicatorColor(getColor(R.color.error));
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
