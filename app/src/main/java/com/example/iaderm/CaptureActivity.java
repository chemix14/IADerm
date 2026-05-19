package com.example.iaderm;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.iaderm.viewmodel.CaptureViewModel;
import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CaptureActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private static final String TAG = "CaptureActivity";

    // ── Views ──
    private PreviewView previewView;
    private FaceGuideView faceGuideView;
    private ScanLineView scanLineView;
    private TextView tvInstrucciones;
    private View btnCapturar;
    private ImageButton btnCambiarCamara;
    private ImageButton btnFlash;
    private TextView btnCancel;
    private FaceAnalyzer faceAnalyzer;
    private DermatologyClassifier dermatologyClassifier;
    private View tutorialOverlay;

    // Indicator status text views
    private TextView tvLightStatus;
    private TextView tvDistanceStatus;
    private TextView tvStabilityStatus;

    // ── CameraX ──
    private ProcessCameraProvider cameraProvider;
    private int currentCameraFacing = CameraSelector.LENS_FACING_FRONT;
    private ExecutorService cameraExecutor;

    // ── ViewModel ──
    private CaptureViewModel viewModel;

    // ── Luminosity tracking ──
    private long lastLuminosityCheck = 0;
    private static final long LUMINOSITY_CHECK_INTERVAL = 500; // ms

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        viewModel = new ViewModelProvider(this).get(CaptureViewModel.class);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Bind views
        previewView = findViewById(R.id.previewView);
        faceGuideView = findViewById(R.id.faceGuideView);
        scanLineView = findViewById(R.id.scanLineView);
        tvInstrucciones = findViewById(R.id.tvInstrucciones);
        btnCapturar = findViewById(R.id.btnCapturar);
        btnCambiarCamara = findViewById(R.id.btnCambiarCamara);
        btnFlash = findViewById(R.id.btnFlash);
        btnCancel = findViewById(R.id.btnCancel);
        tutorialOverlay = findViewById(R.id.tutorialOverlay);

        tvLightStatus = findViewById(R.id.tvLightStatus);
        tvDistanceStatus = findViewById(R.id.tvDistanceStatus);
        tvStabilityStatus = findViewById(R.id.tvStabilityStatus);

        // Set up click listeners
        btnCancel.setOnClickListener(v -> finish());
        btnCambiarCamara.setOnClickListener(v -> switchCamera());
        btnFlash.setOnClickListener(v -> UiFeedback.shortMessage(this, R.string.feature_coming_soon));

        btnCapturar.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(viewModel.getIsLightOk().getValue()) &&
                Boolean.TRUE.equals(viewModel.getIsDistanceOk().getValue()) &&
                Boolean.TRUE.equals(viewModel.getIsStabilityOk().getValue())) {
                performCapture();
            }
        });

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
        }

        // Animate capture button entrance
        btnCapturar.setScaleX(0f);
        btnCapturar.setScaleY(0f);
        btnCapturar.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator())
                .setStartDelay(300)
                .start();

        observeViewModel();
        
        // Initialize FaceAnalyzer with guide bounds
        faceGuideView.post(() -> {
            faceAnalyzer = new FaceAnalyzer(faceGuideView.getOvalBounds(), 
                (faceDetected, isCentered, isCorrectDistance) -> {
                    runOnUiThread(() -> {
                        viewModel.setDistanceOk(faceDetected && isCorrectDistance);
                    });
                });
        });

        dermatologyClassifier = new DermatologyClassifier(this);

        // FTU (First Time Use) Tutorial
        android.content.SharedPreferences prefs = getSharedPreferences("iaderm_prefs", MODE_PRIVATE);
        if (!prefs.getBoolean("tutorial_capture_shown", false)) {
            tutorialOverlay.setVisibility(View.VISIBLE);
            findViewById(R.id.btnEntendidoTutorial).setOnClickListener(v -> {
                tutorialOverlay.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(() -> tutorialOverlay.setVisibility(View.GONE))
                        .start();
                prefs.edit().putBoolean("tutorial_capture_shown", true).apply();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.startDetectors();
    }

    @Override
    protected void onPause() {
        super.onPause();
        viewModel.stopDetectors();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (dermatologyClassifier != null) {
            dermatologyClassifier.close();
        }
    }

    // ═══════════════════════════════════════════════════════
    // ViewModel Observers
    // ═══════════════════════════════════════════════════════

    private void observeViewModel() {
        String okStr = getString(R.string.indicator_ok);
        String failStr = getString(R.string.indicator_fail);

        viewModel.getIsLightOk().observe(this, isOk -> {
            tvLightStatus.setText(isOk ? okStr : failStr);
            tvLightStatus.setTextColor(ContextCompat.getColor(this, isOk ? R.color.success : R.color.error));
            tvLightStatus.setContentDescription(getString(isOk ? R.string.cd_light_indicator_ok : R.string.cd_light_indicator_bad));
        });

        viewModel.getIsDistanceOk().observe(this, isOk -> {
            tvDistanceStatus.setText(isOk ? okStr : failStr);
            tvDistanceStatus.setTextColor(ContextCompat.getColor(this, isOk ? R.color.success : R.color.error));
            tvDistanceStatus.setContentDescription(getString(isOk ? R.string.cd_distance_indicator_ok : R.string.cd_distance_indicator_bad));
        });

        viewModel.getIsStabilityOk().observe(this, isOk -> {
            tvStabilityStatus.setText(isOk ? okStr : failStr);
            tvStabilityStatus.setTextColor(ContextCompat.getColor(this, isOk ? R.color.success : R.color.error));
            tvStabilityStatus.setContentDescription(getString(isOk ? R.string.cd_stability_indicator_ok : R.string.cd_stability_indicator_bad));
        });

        viewModel.getUiState().observe(this, this::applyUiState);
    }

    // ═══════════════════════════════════════════════════════
    // CameraX Setup
    // ═══════════════════════════════════════════════════════

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                UiFeedback.longMessage(this, R.string.error_camera_config);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) return;

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(currentCameraFacing)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
            UiFeedback.shortMessage(this, R.string.error_camera_config);
        }
    }

    private void switchCamera() {
        currentCameraFacing = (currentCameraFacing == CameraSelector.LENS_FACING_FRONT)
                ? CameraSelector.LENS_FACING_BACK
                : CameraSelector.LENS_FACING_FRONT;
        bindCameraUseCases();
    }

    // ═══════════════════════════════════════════════════════
    // Quality Indicator Logic
    // ═══════════════════════════════════════════════════════

    private void analyzeImage(ImageProxy image) {
        long now = System.currentTimeMillis();
        
        // 1. Luminosity Check
        if (now - lastLuminosityCheck >= LUMINOSITY_CHECK_INTERVAL) {
            lastLuminosityCheck = now;
            
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            
            long totalLuma = 0;
            for (byte b : data) {
                totalLuma += (b & 0xFF);
            }
            double avgLuma = (double) totalLuma / data.length;
            
            boolean goodLight = avgLuma > 80;
            runOnUiThread(() -> viewModel.setLightOk(goodLight));
        }
        
        // 2. Face Detection Check
        if (faceAnalyzer != null) {
            faceAnalyzer.analyze(image);
        } else {
            image.close();
        }
    }

    // ═══════════════════════════════════════════════════════
    // Capture Action
    // ═══════════════════════════════════════════════════════

    private String saveImageLocally(Bitmap bitmap) {
        if (bitmap == null) return "";
        try {
            java.io.File directory = new java.io.File(getFilesDir(), "iaderm_photos");
            if (!directory.exists()) directory.mkdirs();
            String fileName = "scan_" + System.currentTimeMillis() + ".jpg";
            java.io.File file = new java.io.File(directory, fileName);
            java.io.FileOutputStream out = new java.io.FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void performCapture() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            btnCapturar.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        } else {
            btnCapturar.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }

        // Flash animation
        View flashView = new View(this);
        flashView.setBackgroundColor(0xCCFFFFFF);
        ((android.view.ViewGroup) previewView.getParent()).addView(flashView,
                new android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT));

        flashView.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> {
                    ((android.view.ViewGroup) previewView.getParent()).removeView(flashView);
                })
                .start();

        // 1. Capture bitmap from preview
        Bitmap bitmap = previewView.getBitmap();
        
        // 2. Save image locally
        final String savedImagePath = saveImageLocally(bitmap);
        
        // 3. Run inference in background (simulated delay for UI feedback)
        previewView.postDelayed(() -> {
            DermatologyClassifier.ClassificationResult result = (bitmap != null) 
                    ? dermatologyClassifier.classify(bitmap) 
                    : new DermatologyClassifier.ClassificationResult("Desconocido", 0, "");
                    
            if (result.score < 0) result.score = 0; // Fallback
            
            final int finalScore = result.score;
            final String finalDiagnosis = result.diagnosis;
            final String top3Data = result.top3Data;

            viewModel.performCapture(finalScore, finalDiagnosis, top3Data, savedImagePath, id -> runOnUiThread(() -> {
                if (id <= 0) {
                    UiFeedback.shortMessage(this, R.string.error_analysis_save_failed);
                    AppNavigator.openResults(CaptureActivity.this, finalScore, finalDiagnosis, top3Data);
                } else {
                    AppNavigator.openResults(CaptureActivity.this, id, finalScore, finalDiagnosis, top3Data);
                }
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }));
        }, 1000);
    }

    // ═══════════════════════════════════════════════════════
    // Permissions
    // ═══════════════════════════════════════════════════════

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                UiFeedback.longMessage(this, R.string.permission_camera_denied);
                finish();
            }
        }
    }

    private void applyUiState(CaptureUiState newState) {
        switch (newState) {
            case READY:
                faceGuideView.setState(FaceGuideView.STATE_READY);
                tvInstrucciones.setText(R.string.capture_ready);
                btnCapturar.setEnabled(true);
                btnCapturar.setAlpha(1.0f);
                btnCapturar.animate()
                        .scaleX(1.1f).scaleY(1.1f)
                        .setDuration(200)
                        .withEndAction(() ->
                                btnCapturar.animate()
                                        .scaleX(1.0f).scaleY(1.0f)
                                        .setDuration(150)
                                        .start())
                        .start();
                scanLineView.stopScan();
                break;
            case ADJUST_LIGHT:
                faceGuideView.setState(FaceGuideView.STATE_ADJUSTING);
                tvInstrucciones.setText(R.string.capture_bad_light);
                btnCapturar.setEnabled(false);
                btnCapturar.setAlpha(0.4f);
                scanLineView.stopScan();
                break;
            case ADJUST_DISTANCE:
                faceGuideView.setState(FaceGuideView.STATE_ADJUSTING);
                tvInstrucciones.setText(R.string.capture_too_far);
                btnCapturar.setEnabled(false);
                btnCapturar.setAlpha(0.4f);
                scanLineView.stopScan();
                break;
            case CAPTURING:
                faceGuideView.setState(FaceGuideView.STATE_CAPTURING);
                scanLineView.startScan();
                tvInstrucciones.setText(R.string.capture_processing);
                btnCapturar.setEnabled(false);
                btnCapturar.setAlpha(0.4f);
                break;
            case SEARCHING:
            default:
                faceGuideView.setState(FaceGuideView.STATE_SEARCHING);
                scanLineView.stopScan();
                tvInstrucciones.setText(R.string.capture_searching);
                btnCapturar.setEnabled(false);
                btnCapturar.setAlpha(0.4f);
                break;
        }
    }
}
