package com.example.iaderm;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class FaceGuideView extends View {

    private Paint backgroundPaint;
    private Paint transparentPaint;
    private Paint borderPaint;

    public FaceGuideView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // MUY IMPORTANTE: Desactiva la aceleración por hardware en esta vista
        // para que el modo "CLEAR" (borrar píxeles) funcione bien.
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // 1. Pintura para oscurecer el fondo (Negro al 70% de opacidad)
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#B3000000"));

        // 2. Pintura "borradora" para crear el agujero transparente
        transparentPaint = new Paint();
        transparentPaint.setColor(Color.TRANSPARENT);
        transparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        transparentPaint.setAntiAlias(true); // Suaviza los bordes

        // 3. Pintura para dibujar una línea blanca alrededor del óvalo
        borderPaint = new Paint();
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE); // Solo el contorno
        borderPaint.setStrokeWidth(8f); // Grosor de la línea
        borderPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Paso A: Pintar todo el lienzo de oscuro
        canvas.drawRect(0, 0, width, height, backgroundPaint);

        // Paso B: Calcular las proporciones del óvalo según la pantalla del celular
        float ovalWidth = width * 0.75f;  // El óvalo ocupará el 75% del ancho de la pantalla
        float ovalHeight = height * 0.55f; // Y el 55% del alto
        float left = (width - ovalWidth) / 2;
        float top = (height - ovalHeight) / 2;
        float right = left + ovalWidth;
        float bottom = top + ovalHeight;

        RectF ovalRect = new RectF(left, top, right, bottom);

        // Paso C: Recortar el óvalo en la pintura oscura dejando ver la cámara
        canvas.drawOval(ovalRect, transparentPaint);

        // Paso D: Dibujar el contorno blanco para que se vea elegante
        canvas.drawOval(ovalRect, borderPaint);
    }
}