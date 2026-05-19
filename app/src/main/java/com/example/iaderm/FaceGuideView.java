package com.example.iaderm;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * FaceGuideView — Custom overlay for the capture screen.
 *
 * Draws a semi-transparent dark overlay with a transparent oval "window"
 * where the user should position their face. The oval border color changes
 * dynamically based on the current capture state:
 *   - SEARCHING: White (pulsing alpha animation)
 *   - ADJUSTING: Yellow/Amber (user needs to adjust position/light)
 *   - READY: Green (all conditions met, ready to capture)
 *   - CAPTURING: Green (solid, no pulse)
 */
public class FaceGuideView extends View {

    // ── Capture States ──
    public static final int STATE_SEARCHING = 0;
    public static final int STATE_ADJUSTING = 1;
    public static final int STATE_READY = 2;
    public static final int STATE_CAPTURING = 3;

    private int currentState = STATE_SEARCHING;

    // ── Paints ──
    private Paint backgroundPaint;
    private Paint transparentPaint;
    private Paint borderPaint;
    private Paint cornerPaint; // Corner brackets for alignment help

    // ── Colors ──
    private int colorSearching;
    private int colorAdjusting;
    private int colorReady;
    private int currentBorderColor;

    // ── Animation ──
    private ValueAnimator pulseAnimator;
    private ValueAnimator colorAnimator;
    private float currentAlpha = 1.0f;

    // ── Dimensions ──
    private RectF ovalRect;

    public FaceGuideView(Context context) {
        super(context);
        init();
    }

    public FaceGuideView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FaceGuideView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // IMPORTANT: Disable hardware acceleration for PorterDuff CLEAR to work
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // Load state colors from resources
        colorSearching = getContext().getColor(R.color.guide_searching);
        colorAdjusting = getContext().getColor(R.color.guide_adjusting);
        colorReady = getContext().getColor(R.color.guide_ready);
        currentBorderColor = colorSearching;

        // 1. Dark overlay background (70% opacity black)
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#B3000000"));

        // 2. Transparent paint to "erase" the oval window
        transparentPaint = new Paint();
        transparentPaint.setColor(Color.TRANSPARENT);
        transparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        transparentPaint.setAntiAlias(true);

        // 3. Oval border
        borderPaint = new Paint();
        borderPaint.setColor(currentBorderColor);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);
        borderPaint.setAntiAlias(true);

        // 4. Corner bracket guides
        cornerPaint = new Paint();
        cornerPaint.setColor(currentBorderColor);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(6f);
        cornerPaint.setAntiAlias(true);
        cornerPaint.setStrokeCap(Paint.Cap.ROUND);

        ovalRect = new RectF();

        // Start the pulse animation for SEARCHING state
        startPulseAnimation();
    }

    /**
     * Set the capture state and animate the border color transition.
     */
    public void setState(int newState) {
        if (currentState == newState) return;

        int oldColor = currentBorderColor;
        int newColor;

        switch (newState) {
            case STATE_ADJUSTING:
                newColor = colorAdjusting;
                break;
            case STATE_READY:
            case STATE_CAPTURING:
                newColor = colorReady;
                break;
            case STATE_SEARCHING:
            default:
                newColor = colorSearching;
                break;
        }

        currentState = newState;

        // Animate color transition
        if (colorAnimator != null) colorAnimator.cancel();
        colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), oldColor, newColor);
        colorAnimator.setDuration(400);
        colorAnimator.addUpdateListener(animation -> {
            currentBorderColor = (int) animation.getAnimatedValue();
            borderPaint.setColor(currentBorderColor);
            cornerPaint.setColor(currentBorderColor);
            invalidate();
        });
        colorAnimator.start();

        // Manage pulse animation
        if (newState == STATE_SEARCHING) {
            startPulseAnimation();
        } else if (newState == STATE_CAPTURING) {
            stopPulseAnimation();
            currentAlpha = 1.0f;
        } else {
            stopPulseAnimation();
            currentAlpha = 1.0f;
        }
    }

    public int getState() {
        return currentState;
    }

    private void startPulseAnimation() {
        if (pulseAnimator != null && pulseAnimator.isRunning()) return;

        pulseAnimator = ValueAnimator.ofFloat(0.4f, 1.0f);
        pulseAnimator.setDuration(1200);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.addUpdateListener(animation -> {
            currentAlpha = (float) animation.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.start();
    }

    private void stopPulseAnimation() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Step A: Draw dark overlay
        canvas.drawRect(0, 0, width, height, backgroundPaint);

        // Step B: Calculate oval proportions
        float ovalWidth = width * 0.70f;
        float ovalHeight = height * 0.48f;
        float left = (width - ovalWidth) / 2;
        float top = (height - ovalHeight) / 2 - (height * 0.05f); // Slightly above center
        float right = left + ovalWidth;
        float bottom = top + ovalHeight;

        ovalRect.set(left, top, right, bottom);

        // Step C: Cut out the transparent oval window
        canvas.drawOval(ovalRect, transparentPaint);

        // Step D: Draw border with current alpha (for pulse effect)
        borderPaint.setAlpha((int)(currentAlpha * 255));
        canvas.drawOval(ovalRect, borderPaint);

        // Step E: Draw corner bracket guides for alignment
        float bracketLen = 30f;
        cornerPaint.setAlpha((int)(currentAlpha * 255));

        // Top-left corner
        canvas.drawLine(left + 20, top, left + 20 + bracketLen, top, cornerPaint);
        canvas.drawLine(left + 20, top, left + 20, top + bracketLen, cornerPaint);

        // Top-right corner
        canvas.drawLine(right - 20, top, right - 20 - bracketLen, top, cornerPaint);
        canvas.drawLine(right - 20, top, right - 20, top + bracketLen, cornerPaint);

        // Bottom-left corner
        canvas.drawLine(left + 20, bottom, left + 20 + bracketLen, bottom, cornerPaint);
        canvas.drawLine(left + 20, bottom, left + 20, bottom - bracketLen, cornerPaint);

        // Bottom-right corner
        canvas.drawLine(right - 20, bottom, right - 20 - bracketLen, bottom, cornerPaint);
        canvas.drawLine(right - 20, bottom, right - 20, bottom - bracketLen, cornerPaint);
    }

    /**
     * Returns the oval bounds for face detection alignment.
     */
    public RectF getOvalBounds() {
        return new RectF(ovalRect);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopPulseAnimation();
        if (colorAnimator != null) colorAnimator.cancel();
    }
}