package com.example.iaderm;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * HeatmapOverlayView — Draws a transparent heatmap overlay on the captured
 * face photo in the Results screen.
 *
 * Each "zone" represents a region of detected redness/inflammation.
 * The zones are rendered as radial gradients with colors from the
 * heatmap palette (yellow → orange → red) based on severity.
 *
 * Usage:
 *   heatmapView.clearZones();
 *   heatmapView.addZone(0.45f, 0.35f, 0.15f, 0.7f); // x, y, radius, intensity
 *   heatmapView.invalidate();
 */
public class HeatmapOverlayView extends View {

    /**
     * Represents a single heatmap zone on the face.
     */
    public static class HeatmapZone {
        /** Normalized X position (0.0 = left, 1.0 = right) */
        public float normalizedX;
        /** Normalized Y position (0.0 = top, 1.0 = bottom) */
        public float normalizedY;
        /** Normalized radius relative to view width */
        public float normalizedRadius;
        /** Intensity from 0.0 (none) to 1.0 (severe) */
        public float intensity;

        public HeatmapZone(float x, float y, float radius, float intensity) {
            this.normalizedX = x;
            this.normalizedY = y;
            this.normalizedRadius = radius;
            this.intensity = Math.max(0f, Math.min(1f, intensity));
        }
    }

    private final List<HeatmapZone> zones = new ArrayList<>();
    private Paint zonePaint;

    // Heatmap color palette
    private int colorLow;
    private int colorMedium;
    private int colorHigh;

    public HeatmapOverlayView(Context context) {
        super(context);
        init();
    }

    public HeatmapOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HeatmapOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        zonePaint = new Paint();
        zonePaint.setAntiAlias(true);
        zonePaint.setDither(true);

        colorLow = getContext().getColor(R.color.heatmap_low);
        colorMedium = getContext().getColor(R.color.heatmap_medium);
        colorHigh = getContext().getColor(R.color.heatmap_high);
    }

    /**
     * Add a detection zone to the heatmap.
     *
     * @param normalizedX Center X (0.0 - 1.0)
     * @param normalizedY Center Y (0.0 - 1.0)
     * @param normalizedRadius Radius relative to view width (0.0 - 0.5)
     * @param intensity Severity level (0.0 - 1.0)
     */
    public void addZone(float normalizedX, float normalizedY, float normalizedRadius, float intensity) {
        zones.add(new HeatmapZone(normalizedX, normalizedY, normalizedRadius, intensity));
    }

    /**
     * Clear all zones.
     */
    public void clearZones() {
        zones.clear();
    }

    /**
     * Set zones from an analysis result (replaces all existing zones).
     */
    public void setZones(List<HeatmapZone> newZones) {
        zones.clear();
        if (newZones != null) {
            zones.addAll(newZones);
        }
        invalidate();
    }

    /**
     * Get the heatmap color for a given intensity.
     * 0.0-0.33: Low (yellow)
     * 0.33-0.66: Medium (orange)
     * 0.66-1.0: High (red/pink)
     */
    private int getColorForIntensity(float intensity) {
        if (intensity < 0.33f) {
            return colorLow;
        } else if (intensity < 0.66f) {
            return colorMedium;
        } else {
            return colorHigh;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (zones.isEmpty()) return;

        int width = getWidth();
        int height = getHeight();

        for (HeatmapZone zone : zones) {
            float cx = zone.normalizedX * width;
            float cy = zone.normalizedY * height;
            float radius = zone.normalizedRadius * width;

            int baseColor = getColorForIntensity(zone.intensity);

            // Create a semi-transparent version of the color
            int alpha = (int)(zone.intensity * 120); // Max 120 out of 255 for subtlety
            int transparentColor = Color.argb(0, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));
            int semiTransparent = Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));

            // Radial gradient: solid center → transparent edges
            RadialGradient gradient = new RadialGradient(
                    cx, cy, radius,
                    semiTransparent,
                    transparentColor,
                    Shader.TileMode.CLAMP
            );

            zonePaint.setShader(gradient);
            canvas.drawCircle(cx, cy, radius, zonePaint);
        }
    }

    /**
     * Load demo zones for testing the heatmap visualization.
     * Simulates a typical rosacea distribution:
     * - Cheeks (primary area)
     * - Nose bridge
     * - Forehead (mild)
     */
    public void loadDemoZones() {
        clearZones();
        // Left cheek — moderate
        addZone(0.30f, 0.55f, 0.18f, 0.55f);
        // Right cheek — moderate
        addZone(0.70f, 0.55f, 0.18f, 0.60f);
        // Nose — higher intensity
        addZone(0.50f, 0.50f, 0.10f, 0.75f);
        // Forehead — mild
        addZone(0.50f, 0.30f, 0.15f, 0.25f);
        // Chin — mild
        addZone(0.50f, 0.72f, 0.10f, 0.20f);
        invalidate();
    }
}
