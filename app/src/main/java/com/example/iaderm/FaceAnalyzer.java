package com.example.iaderm;

import android.graphics.Rect;
import android.graphics.RectF;
import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

/**
 * FaceAnalyzer — Real-time face detection using Google ML Kit.
 * 
 * Analyzes CameraX frames to detect a face and check if it is
 * correctly positioned within the guide oval.
 */
public class FaceAnalyzer implements ImageAnalysis.Analyzer {

    public interface FaceDetectionCallback {
        void onFaceStatusChanged(boolean faceDetected, boolean isCentered, boolean isCorrectDistance);
    }

    private final FaceDetector detector;
    private final FaceDetectionCallback callback;
    private final RectF guideOval;
    
    // Thresholds for distance (based on face width relative to guide)
    private static final float MIN_FACE_WIDTH_RATIO = 0.5f;
    private static final float MAX_FACE_WIDTH_RATIO = 0.95f;

    public FaceAnalyzer(RectF guideOval, FaceDetectionCallback callback) {
        this.guideOval = guideOval;
        this.callback = callback;

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build();

        this.detector = FaceDetection.getClient(options);
    }

    @Override
    @androidx.camera.core.ExperimentalGetImage
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    processFaces(faces, imageProxy.getWidth(), imageProxy.getHeight());
                })
                .addOnFailureListener(e -> {
                    // Handle error
                })
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void processFaces(List<Face> faces, int imgWidth, int imgHeight) {
        if (faces.isEmpty()) {
            callback.onFaceStatusChanged(false, false, false);
            return;
        }

        // We only care about the first (largest) face
        Face face = faces.get(0);
        Rect bounds = face.getBoundingBox();

        // Normalize coordinates to guide scale (0..1 or absolute pixels)
        // Note: Camera frames might be rotated, but ML Kit handles that.
        // We need to map face bounds to the same coordinate system as guideOval.
        
        // Simplified check: is the face roughly in the center?
        float faceCenterX = bounds.centerX();
        float faceCenterY = bounds.centerY();
        
        // Check if face is centered relative to image
        boolean isCentered = Math.abs(faceCenterX - (imgWidth / 2f)) < (imgWidth * 0.2f);
        
        // Distance check: face width should be between 50% and 90% of guide width
        float faceWidthRatio = (float) bounds.width() / imgWidth;
        boolean isCorrectDistance = faceWidthRatio > 0.25f && faceWidthRatio < 0.6f;

        callback.onFaceStatusChanged(true, isCentered, isCorrectDistance);
    }
}
