package com.example.iaderm;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * StabilityDetector — Uses the device accelerometer to detect
 * whether the phone is being held steadily enough for photo capture.
 *
 * Measures the magnitude of acceleration changes over a rolling window.
 * If the variation stays below a threshold for a sustained period,
 * the device is considered "stable".
 *
 * Usage:
 *   StabilityDetector detector = new StabilityDetector(context, isStable -> {
 *       // Update UI indicator
 *   });
 *   detector.start();   // onResume
 *   detector.stop();    // onPause
 */
public class StabilityDetector implements SensorEventListener {

    public interface StabilityCallback {
        void onStabilityChanged(boolean isStable);
    }

    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final StabilityCallback callback;

    // Rolling window of acceleration magnitudes
    private static final int WINDOW_SIZE = 10;
    private final float[] magnitudes = new float[WINDOW_SIZE];
    private int magnitudeIndex = 0;
    private boolean windowFull = false;

    // Threshold: max allowed standard deviation of acceleration
    private static final float STABILITY_THRESHOLD = 0.35f;

    // Debounce: require stability for N consecutive samples
    private int stableCount = 0;
    private static final int STABLE_REQUIRED = 5;

    private boolean lastReportedStable = false;
    private boolean isRunning = false;

    // Previous acceleration values for high-pass filter
    private float lastX = 0, lastY = 0, lastZ = 0;
    private boolean hasBaseline = false;

    public StabilityDetector(Context context, StabilityCallback callback) {
        this.callback = callback;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    /**
     * Start listening to accelerometer events.
     */
    public void start() {
        if (accelerometer != null && !isRunning) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_UI);
            isRunning = true;
            hasBaseline = false;
            stableCount = 0;
            windowFull = false;
            magnitudeIndex = 0;
        }
    }

    /**
     * Stop listening to accelerometer events.
     */
    public void stop() {
        if (isRunning) {
            sensorManager.unregisterListener(this);
            isRunning = false;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        if (!hasBaseline) {
            lastX = x;
            lastY = y;
            lastZ = z;
            hasBaseline = true;
            return;
        }

        // High-pass filter: calculate change from last reading
        float dx = x - lastX;
        float dy = y - lastY;
        float dz = z - lastZ;
        lastX = x;
        lastY = y;
        lastZ = z;

        // Magnitude of the change vector (ignoring gravity by looking at deltas)
        float magnitude = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Store in rolling window
        magnitudes[magnitudeIndex] = magnitude;
        magnitudeIndex = (magnitudeIndex + 1) % WINDOW_SIZE;
        if (magnitudeIndex == 0) windowFull = true;

        if (!windowFull) return;

        // Calculate standard deviation of the window
        float mean = 0;
        for (float m : magnitudes) mean += m;
        mean /= WINDOW_SIZE;

        float variance = 0;
        for (float m : magnitudes) {
            float diff = m - mean;
            variance += diff * diff;
        }
        variance /= WINDOW_SIZE;
        float stdDev = (float) Math.sqrt(variance);

        // Determine stability with debounce
        boolean currentlyStable = stdDev < STABILITY_THRESHOLD;

        if (currentlyStable) {
            stableCount = Math.min(stableCount + 1, STABLE_REQUIRED + 1);
        } else {
            stableCount = Math.max(stableCount - 2, 0); // Faster to become unstable
        }

        boolean isStable = stableCount >= STABLE_REQUIRED;

        // Only report changes
        if (isStable != lastReportedStable) {
            lastReportedStable = isStable;
            if (callback != null) {
                callback.onStabilityChanged(isStable);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }

    /**
     * Check if device is currently considered stable.
     */
    public boolean isStable() {
        return lastReportedStable;
    }
}
