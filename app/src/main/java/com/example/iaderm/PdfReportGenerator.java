package com.example.iaderm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;

import com.example.iaderm.data.AnalysisRecord;
import com.example.iaderm.data.TriggerRecord;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * PdfReportGenerator — Generates a professional-grade PDF report
 * of the patient's analysis history for their dermatologist.
 *
 * The report includes:
 * - Patient summary header
 * - Analysis history table
 * - Score trend mini-chart
 * - Logged triggers summary
 * - Medical disclaimer
 *
 * Usage:
 *   String path = PdfReportGenerator.generate(context, analyses, triggers);
 */
public class PdfReportGenerator {

    private static final int PAGE_WIDTH = 595;  // A4 in points
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 40;
    private static final int CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("es", "MX"));
    private static final SimpleDateFormat FILE_DATE_FORMAT =
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

    /**
     * Generate a PDF report and return the file path.
     *
     * @param context     Application context
     * @param analyses    List of analysis records (newest first)
     * @param triggers    List of trigger records
     * @return Absolute file path to the generated PDF
     */
    public static String generate(Context context,
                                  List<AnalysisRecord> analyses,
                                  List<TriggerRecord> triggers) throws IOException {

        PdfDocument document = new PdfDocument();

        // ── Page 1: Summary + History ──
        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        float y = MARGIN;

        // Paints
        Paint titlePaint = createPaint(20, Color.parseColor("#0D7377"), true);
        Paint headingPaint = createPaint(14, Color.parseColor("#1A1A2E"), true);
        Paint bodyPaint = createPaint(10, Color.parseColor("#3D3D4E"), false);
        Paint captionPaint = createPaint(8, Color.parseColor("#6B7B8D"), false);
        Paint accentPaint = createPaint(10, Color.parseColor("#0D7377"), true);
        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#E0EBE6"));
        linePaint.setStrokeWidth(1f);

        // ── Header ──
        canvas.drawText("IADerm — Reporte de Análisis Dermatológico", MARGIN, y, titlePaint);
        y += 28;

        canvas.drawText("Generado: " + DATE_FORMAT.format(new Date()), MARGIN, y, captionPaint);
        y += 14;

        canvas.drawText("Herramienta de seguimiento de bienestar — NO es diagnóstico médico",
                MARGIN, y, captionPaint);
        y += 24;

        // Horizontal rule
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint);
        y += 20;

        // ── Summary Stats ──
        canvas.drawText("Resumen del Paciente", MARGIN, y, headingPaint);
        y += 20;

        int totalAnalyses = analyses != null ? analyses.size() : 0;
        canvas.drawText("Total de análisis: " + totalAnalyses, MARGIN, y, bodyPaint);
        y += 16;

        if (totalAnalyses > 0) {
            AnalysisRecord latest = analyses.get(0);
            canvas.drawText("Último análisis: " + DATE_FORMAT.format(new Date(latest.timestamp)),
                    MARGIN, y, bodyPaint);
            y += 16;

            canvas.drawText("Último índice de rojez: " + latest.score + "/100 (" +
                    getSeverityLabel(latest.severity) + ")", MARGIN, y, bodyPaint);
            y += 16;

            // Average score
            int sum = 0;
            for (AnalysisRecord a : analyses) sum += a.score;
            float avg = (float) sum / totalAnalyses;
            canvas.drawText("Promedio de rojez: " + String.format(Locale.getDefault(), "%.1f", avg) +
                    "/100", MARGIN, y, bodyPaint);
            y += 24;
        }

        // ── Score Trend Chart ──
        if (totalAnalyses >= 2) {
            canvas.drawText("Tendencia de Índice de Rojez", MARGIN, y, headingPaint);
            y += 16;

            float chartLeft = MARGIN;
            float chartTop = y;
            float chartWidth = CONTENT_WIDTH;
            float chartHeight = 100;

            // Draw chart background
            Paint chartBgPaint = new Paint();
            chartBgPaint.setColor(Color.parseColor("#F0EBE6"));
            canvas.drawRect(chartLeft, chartTop, chartLeft + chartWidth,
                    chartTop + chartHeight, chartBgPaint);

            // Draw data points (most recent last → reverse order)
            int pointCount = Math.min(totalAnalyses, 20);
            Paint chartLinePaint = new Paint();
            chartLinePaint.setColor(Color.parseColor("#0D7377"));
            chartLinePaint.setStrokeWidth(2f);
            chartLinePaint.setAntiAlias(true);

            Paint dotPaint = new Paint();
            dotPaint.setColor(Color.parseColor("#0D7377"));
            dotPaint.setAntiAlias(true);

            float spacing = chartWidth / (pointCount - 1);

            for (int i = 0; i < pointCount; i++) {
                // Reverse index (oldest first in chart)
                AnalysisRecord rec = analyses.get(pointCount - 1 - i);
                float px = chartLeft + i * spacing;
                float py = chartTop + chartHeight - (rec.score / 100f * chartHeight);

                canvas.drawCircle(px, py, 3f, dotPaint);

                if (i > 0) {
                    AnalysisRecord prevRec = analyses.get(pointCount - i);
                    float prevX = chartLeft + (i - 1) * spacing;
                    float prevY = chartTop + chartHeight - (prevRec.score / 100f * chartHeight);
                    canvas.drawLine(prevX, prevY, px, py, chartLinePaint);
                }
            }

            y += chartHeight + 24;
        }

        // ── Analysis History Table ──
        canvas.drawText("Historial de Análisis", MARGIN, y, headingPaint);
        y += 18;

        // Table headers
        canvas.drawText("Fecha", MARGIN, y, accentPaint);
        canvas.drawText("Score", MARGIN + 180, y, accentPaint);
        canvas.drawText("Nivel", MARGIN + 250, y, accentPaint);
        y += 4;
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint);
        y += 14;

        // Table rows (limit to fit page)
        int maxRows = Math.min(totalAnalyses, 15);
        for (int i = 0; i < maxRows; i++) {
            AnalysisRecord rec = analyses.get(i);

            canvas.drawText(DATE_FORMAT.format(new Date(rec.timestamp)), MARGIN, y, bodyPaint);
            canvas.drawText(rec.score + "/100", MARGIN + 180, y, bodyPaint);
            canvas.drawText(getSeverityLabel(rec.severity), MARGIN + 250, y, bodyPaint);
            y += 16;

            if (y > PAGE_HEIGHT - 120) break; // Leave room for footer
        }

        y += 12;

        // ── Triggers Summary ──
        if (triggers != null && !triggers.isEmpty()) {
            if (y > PAGE_HEIGHT - 200) {
                // Need a new page
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = MARGIN;
            }

            canvas.drawText("Desencadenantes Registrados", MARGIN, y, headingPaint);
            y += 18;

            // Count triggers by type
            java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
            for (TriggerRecord t : triggers) {
                counts.merge(t.triggerType, 1, Integer::sum);
            }

            for (java.util.Map.Entry<String, Integer> entry : counts.entrySet()) {
                String emoji = TriggerRecord.getEmojiForType(entry.getKey());
                canvas.drawText(emoji + " " + capitalize(entry.getKey()) + ": " +
                        entry.getValue() + " registros", MARGIN + 10, y, bodyPaint);
                y += 16;
            }

            y += 12;
        }

        // ── Footer: Disclaimer ──
        y = PAGE_HEIGHT - 60;
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint);
        y += 14;

        Paint disclaimerPaint = createPaint(7, Color.parseColor("#9E9E9E"), false);
        canvas.drawText("AVISO: IADerm es una herramienta educativa de seguimiento de bienestar.",
                MARGIN, y, disclaimerPaint);
        y += 10;
        canvas.drawText("No es un dispositivo médico. Los resultados NO sustituyen la evaluación " +
                "de un profesional.", MARGIN, y, disclaimerPaint);
        y += 10;
        canvas.drawText("Consulte siempre a un dermatólogo certificado.",
                MARGIN, y, disclaimerPaint);

        document.finishPage(page);

        // ── Save to file ──
        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "IADerm_Reports");
        if (!dir.exists()) dir.mkdirs();

        String filename = "IADerm_Reporte_" + FILE_DATE_FORMAT.format(new Date()) + ".pdf";
        File file = new File(dir, filename);

        FileOutputStream fos = new FileOutputStream(file);
        document.writeTo(fos);
        fos.close();
        document.close();

        return file.getAbsolutePath();
    }

    // ── Helpers ──

    private static Paint createPaint(float size, int color, boolean bold) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setAntiAlias(true);
        if (bold) paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        return paint;
    }

    private static String getSeverityLabel(String severity) {
        if (severity == null) return "—";
        switch (severity) {
            case "mild": return "Leve";
            case "moderate": return "Moderado";
            case "severe": return "Elevado";
            default: return severity;
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
