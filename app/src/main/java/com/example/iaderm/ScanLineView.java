package com.example.iaderm;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * ScanLineView — Animated horizontal scan line that sweeps vertically
 * across the face during the analysis processing phase.
 *
 * Creates a "medical scanner" visual effect with a gradient line
 * that moves from top to bottom repeatedly.
 *
 * Usage:
 *   scanLineView.startScan();   // Begin animation
 *   scanLineView.stopScan();    // Stop animation
 */
public class ScanLineView extends View {

    private Paint scanPaint;
    private float currentY = 0f;
    private ValueAnimator scanAnimator;

    private int scanColor;
    private int scanColorTransparent;

    // Line height (the "thickness" of the scan beam)
    private static final float BEAM_HEIGHT = 60f;

    public ScanLineView(Context context) {
        super(context);
        init();
    }

    public ScanLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScanLineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        scanColor = getContext().getColor(R.color.primary);
        scanColorTransparent = 0x000D7377; // Transparent version of primary

        scanPaint = new Paint();
        scanPaint.setAntiAlias(true);
        scanPaint.setStyle(Paint.Style.FILL);

        // Initially invisible
        setAlpha(0f);
    }

    /**
     * Start the scanning animation.
     * The line sweeps from top to bottom in 2 seconds, then repeats.
     */
    public void startScan() {
        setAlpha(1f);

        if (scanAnimator != null && scanAnimator.isRunning()) {
            scanAnimator.cancel();
        }

        scanAnimator = ValueAnimator.ofFloat(0f, 1f);
        scanAnimator.setDuration(2000);
        scanAnimator.setRepeatMode(ValueAnimator.RESTART);
        scanAnimator.setRepeatCount(ValueAnimator.INFINITE);
        scanAnimator.setInterpolator(new LinearInterpolator());
        scanAnimator.addUpdateListener(animation -> {
            currentY = (float) animation.getAnimatedValue();
            invalidate();
        });
        scanAnimator.start();
    }

    /**
     * Stop the scanning animation with a fade-out.
     */
    public void stopScan() {
        animate().alpha(0f).setDuration(300).withEndAction(() -> {
            if (scanAnimator != null) {
                scanAnimator.cancel();
                scanAnimator = null;
            }
        }).start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (scanAnimator == null || !scanAnimator.isRunning()) return;

        int width = getWidth();
        int height = getHeight();

        float yPos = currentY * height;

        // Create a gradient for the scan beam:
        // transparent → primary (50% alpha) → transparent
        LinearGradient gradient = new LinearGradient(
                0, yPos - BEAM_HEIGHT,
                0, yPos + BEAM_HEIGHT,
                new int[]{
                        scanColorTransparent,
                        (scanColor & 0x00FFFFFF) | 0x80000000, // 50% alpha
                        scanColorTransparent
                },
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
        );

        scanPaint.setShader(gradient);

        // Draw the horizontal beam
        canvas.drawRect(0, yPos - BEAM_HEIGHT, width, yPos + BEAM_HEIGHT, scanPaint);

        // Draw a thin bright center line for crisp effect
        Paint linePaint = new Paint();
        linePaint.setColor((scanColor & 0x00FFFFFF) | 0xCC000000); // 80% alpha
        linePaint.setStrokeWidth(2f);
        linePaint.setAntiAlias(true);
        canvas.drawLine(0, yPos, width, yPos, linePaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (scanAnimator != null) {
            scanAnimator.cancel();
        }
    }
}
