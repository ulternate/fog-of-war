package com.danielcswain.fogofwar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import static com.danielcswain.fogofwar.R.color.overlay;

/**
 * OverlayView to draw fog overlay on map using canvas. The users path is erased from the drawing
 * based upon the places they have visited.
 */
public class OverlayView extends View {

    Paint overlayPaint;
    Paint pathPaint;
    Path path;

    public OverlayView(Context context) {
        super(context);
        init();
        // Setting the LayerType of the view enables the path to erase it, removing the fog.
        setLayerType(View.LAYER_TYPE_SOFTWARE, overlayPaint);
    }

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        setLayerType(View.LAYER_TYPE_SOFTWARE, overlayPaint);
    }

    public OverlayView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
        setLayerType(View.LAYER_TYPE_SOFTWARE, overlayPaint);
    }

    /**
     * Initialise all path and paint objects.
     */
    private void init() {
        // Set the path to fill inside the lines.
        path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);

        // Set up the path paintbrush to erase from the overlay.
        pathPaint = new Paint();
        pathPaint.setColor(Color.TRANSPARENT);
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        pathPaint.setStrokeWidth(50);

        // Set up the overlay.
        overlayPaint = new Paint();
        overlayPaint.setColor(ContextCompat.getColor(getContext(), overlay));
        overlayPaint.setStyle(Paint.Style.FILL);
    }

    /**
     * Draw the users path on the canvas when the View is drawn.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Fill canvas with the overlay.
        float canvasWidth = getWidth();
        float canvasHeight = getHeight();
        canvas.drawRect(0, 0, canvasWidth, canvasHeight, overlayPaint);

        // Draw some fake paths. TODO use location data to draw paths.
        path.moveTo(500, 500);
        path.lineTo(1200, 700);
        path.lineTo(1200, 1900);
        path.lineTo(1240, 1900);

        path.moveTo(canvasWidth / 8, canvasHeight / 8);
        path.lineTo(canvasWidth - canvasWidth / 4, canvasHeight - canvasHeight / 4);

        canvas.drawPath(path, pathPaint);
    }
}
