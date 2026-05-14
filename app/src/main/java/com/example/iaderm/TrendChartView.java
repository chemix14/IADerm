package com.example.iaderm;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Lightweight trend chart for history screen.
 */
public class TrendChartView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int[] scores = new int[]{42};

    public TrendChartView(Context context) {
        super(context);
        init();
    }

    public TrendChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TrendChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint.setColor(getContext().getColor(R.color.primary));
        linePaint.setStrokeWidth(5f);
        linePaint.setStyle(Paint.Style.STROKE);

        pointPaint.setColor(getContext().getColor(R.color.primary));
        pointPaint.setStyle(Paint.Style.FILL);

        gridPaint.setColor(getContext().getColor(R.color.outline));
        gridPaint.setStrokeWidth(1f);
    }

    public void setScores(int[] newScores) {
        if (newScores == null || newScores.length == 0) {
            scores = new int[]{42};
        } else {
            scores = newScores;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int padding = 24;

        float left = padding;
        float top = padding;
        float right = width - padding;
        float bottom = height - padding;

        canvas.drawLine(left, top, left, bottom, gridPaint);
        canvas.drawLine(left, bottom, right, bottom, gridPaint);

        if (scores.length < 2) {
            float y = bottom - ((scores[0] / 100f) * (bottom - top));
            canvas.drawCircle((left + right) / 2f, y, 6f, pointPaint);
            return;
        }

        float stepX = (right - left) / (scores.length - 1);
        float prevX = left;
        float prevY = bottom - ((scores[0] / 100f) * (bottom - top));

        for (int i = 1; i < scores.length; i++) {
            float x = left + (i * stepX);
            float y = bottom - ((scores[i] / 100f) * (bottom - top));
            canvas.drawLine(prevX, prevY, x, y, linePaint);
            canvas.drawCircle(prevX, prevY, 6f, pointPaint);
            prevX = x;
            prevY = y;
        }
        canvas.drawCircle(prevX, prevY, 6f, pointPaint);
    }
}
