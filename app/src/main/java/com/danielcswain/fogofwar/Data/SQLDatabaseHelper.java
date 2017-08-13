package com.danielcswain.fogofwar.Data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.List;

/**
 * Database helper to connect to and write to the SQLite database.
 */
public class SQLDatabaseHelper extends SQLiteOpenHelper{

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "LocationDB";
    private static final String TABLE_NAME = "Locations";
    private static final String KEY_ID = "id";
    private static final String KEY_DATETIME = "datetime";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String[] COLUMNS = { KEY_ID, KEY_DATETIME, KEY_LATITUDE, KEY_LONGITUDE };
    private static final String GTE = " >= ";
    private static final String LTE = " <= ";
    private static final String AND = " AND ";

    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME +
            "( " + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            KEY_DATETIME + " TEXT, " + KEY_LATITUDE + " REAL, " + KEY_LONGITUDE + " REAL )";

    /**
     * Constructor for the SQLDatabaseHelper.
     * @param context: The Activity/Application context.
     */
    public SQLDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Create the Table in the SQLite database.
     * @param sqLiteDatabase: The SQLite database.
     */
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TABLE);
    }

    /**
     * Update the SQLite database.
     * @param sqLiteDatabase: The SQLite database.
     * @param i: int representing the old version number.
     * @param i1: int representing the new version number.
     */
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        this.onCreate(sqLiteDatabase);
    }

    /**
     * Add a LocationObject record to the database, storing a location point.
     * @param locationObject: A LocationObject representing a visited LatLng position and the time
     *      it was visited.
     */
    public void addLocation(LocationObject locationObject) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_DATETIME, locationObject.getDatetime());
        contentValues.put(KEY_LATITUDE, locationObject.getLatitude());
        contentValues.put(KEY_LONGITUDE, locationObject.getLongitude());

        sqLiteDatabase.insert(TABLE_NAME, null, contentValues);
        sqLiteDatabase.close();
    }

    /**
     * Get a list of LocationObjects that were visited within the current map view's bounds.
     * @param mapBounds: The LatLngBounds of the map view (Containing the NE and SW LatLng points).
     * @return a List of LocationObjects within the map view's bounds.
     */
    public List<LocationObject> getLocationsInWindow(LatLngBounds mapBounds) {
        LatLng northeast = mapBounds.northeast;
        LatLng southwest = mapBounds.southwest;

        double minLatitude = southwest.latitude;
        double maxLatitude = northeast.latitude;
        double minLongitude = southwest.longitude;
        double maxLongitude = northeast.longitude;

        List<LocationObject> locationObjects = new ArrayList<>();

        String selectionString = KEY_LATITUDE + GTE + "?" + AND + KEY_LATITUDE + LTE + "?" + AND +
                KEY_LONGITUDE + GTE + "?" + AND + KEY_LONGITUDE + LTE + "?";

        String[] selectionArgs = {
                String.valueOf(minLatitude),
                String.valueOf(maxLatitude),
                String.valueOf(minLongitude),
                String.valueOf(maxLongitude),
        };

        String orderBy = KEY_DATETIME + " ASC";

        // Query the database.
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        Cursor cursor = sqLiteDatabase.query(
                TABLE_NAME,
                COLUMNS,
                selectionString,
                selectionArgs,
                null,
                null,
                orderBy,
                null
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    // Create a temporary LocationObject and add to the list.
                    int id = cursor.getInt(cursor.getColumnIndex(KEY_ID));
                    long datetime = cursor.getLong(cursor.getColumnIndex(KEY_DATETIME));
                    double latitude = cursor.getDouble(cursor.getColumnIndex(KEY_LATITUDE));
                    double longitude = cursor.getDouble(cursor.getColumnIndex(KEY_LONGITUDE));

                    LocationObject locationObject = new LocationObject(
                            id, datetime, latitude, longitude);

                    locationObjects.add(locationObject);
                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        return locationObjects;
    }
}
