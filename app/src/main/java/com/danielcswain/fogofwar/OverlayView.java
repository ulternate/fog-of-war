package com.danielcswain.fogofwar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import com.danielcswain.fogofwar.Data.LocationObject;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;

import java.util.Iterator;
import java.util.List;

import static com.danielcswain.fogofwar.R.color.overlay;

/**
 * OverlayView to draw fog overlay on map using canvas. The users path is erased from the drawing
 * based upon the places they have visited.
 */
public class OverlayView extends View {

    private Paint overlayPaint;
    private Paint pathPaint;
    private Path path;

    public OverlayView(Context context) {
        super(context);
        init();
    }

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OverlayView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Initialise all path and paint objects.
     */
    private void init() {
        // Setting the LayerType of the view enables the path to erase it, removing the fog.
        setLayerType(View.LAYER_TYPE_SOFTWARE, overlayPaint);

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

        // Draw the path.
        canvas.drawPath(path, pathPaint);
    }

    /**
     * Draw the user's path from a list of location objects within the bounds of the view.
     * @param locationObjects: A List of LocationObject objects within the maps boundary.
     */
    public void drawPathInMapBounds(List<LocationObject> locationObjects) {
        // Reset the path.
        path.reset();

        // Iterate between all location objects in the boundary.
        Iterator<LocationObject> locationObjectIterator = locationObjects.iterator();
        if (locationObjectIterator.hasNext()) {
            // Add the first point to the path as the origin.
            LocationObject originalLocation = locationObjectIterator.next();

            Point startingPoint = convertToPointCoordinate(originalLocation);
            path.moveTo(startingPoint.x, startingPoint.y);

            while (locationObjectIterator.hasNext()) {
                // Either add the next location to the path, or start from there if it was not
                // possible to travel between the two points.
                LocationObject nextLocation = locationObjectIterator.next();
                Point nextPoint = convertToPointCoordinate(nextLocation);

                if (nextLocation.getId() - originalLocation.getId() == 1 &&
                        isPossibleToTravelBetweenPoints(originalLocation, nextLocation)) {
                    path.lineTo(nextPoint.x, nextPoint.y);
                } else {
                    path.moveTo(nextPoint.x, nextPoint.y);
                }

                originalLocation = nextLocation;
            }
        }
        // Invalidate the view to get onDraw to be called with the updated path.
        this.invalidate();
    }

    /**
     * Convert the LocationObject's LatLng coordinates to a Point object (a point on the screen).
     * @param locationObject: The LocationObject being converted.
     * @return A Point object.
     */
    private Point convertToPointCoordinate(LocationObject locationObject) {
        // Get the Projection of the map to convert the LatLng object to a Point on the screen.
        Projection mapProjection = MainActivity.mMap.getProjection();

        LatLng latLng = locationObject.getLatLng();

        return mapProjection.toScreenLocation(latLng);
    }

    /**
     * Determine if it is possible to travel between the two points in the time between they were
     * recorded.
     *
     * The limit on travel time is 31 m/s, or roughly 110 km/h.
     *
     * @param originalLocation: A LocationObject, the original or previous location.
     * @param nextLocation: A LocationObject, the current or next location.
     * @return a boolean, True if it is possible, else false.
     */
    private boolean isPossibleToTravelBetweenPoints(
            LocationObject originalLocation, LocationObject nextLocation) {

        long startTime = originalLocation.getDatetime();
        long endTime = nextLocation.getDatetime();

        float distanceBetweenPoints = originalLocation.getLocation().distanceTo(
                nextLocation.getLocation());

        double timeInSeconds = (endTime - startTime) / 1000.0;
        double maxSpeedMpS = 31;

        return distanceBetweenPoints != 0.0 && maxSpeedMpS >= distanceBetweenPoints / timeInSeconds;
    }
}
