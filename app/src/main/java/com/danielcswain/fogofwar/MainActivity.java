package com.danielcswain.fogofwar;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

import com.danielcswain.fogofwar.Data.LocationObject;
import com.danielcswain.fogofwar.Data.SQLDatabaseHelper;
import com.danielcswain.fogofwar.OpenSourcePackages.PermissionUtils;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.List;

import static com.danielcswain.fogofwar.R.id.map;

public class MainActivity extends AppCompatActivity
        implements
        GoogleMap.OnCameraMoveListener,
        OnMyLocationButtonClickListener,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_CHECK_SETTINGS = 2;
    private static final int UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    private final static String KEY_LOCATION = "location";

    private boolean mPermissionDenied = false;

    private FusedLocationProviderClient mFusedLocationClient;
    public static GoogleMap mMap;
    private Location mCurrentLocation;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private SettingsClient mSettingsClient;

    private SQLDatabaseHelper mSqlDatabaseHelper;
    private OverlayView overlayView;

    /**
     * Create the Activity, initalising the layout and toolbar as well as the map fragment.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the database helper and overlay view.
        mSqlDatabaseHelper = new SQLDatabaseHelper(this);
        overlayView = findViewById(R.id.overlay);

        // Set up the ActionBar.
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        // Show the Google Maps fragment.
        FragmentManager myFragmentManager = getSupportFragmentManager();
        SupportMapFragment mapFragment = (SupportMapFragment) myFragmentManager
                .findFragmentById(map);
        mapFragment.getMapAsync(this);

        updateValuesFromBundle(savedInstanceState);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        // Build the callbacks and requests required to get location updates.
        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();
    }

    /**
     * Updates the location based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {

            // Update the value of mCurrentLocation from the Bundle and update the map to show
            // the previous location.
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
                updateLocationUI();
            }
        }
    }

    /**
     * Create a location request to receive location updates.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Creates a callback for receiving location events and updating the position on the map.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                mCurrentLocation = locationResult.getLastLocation();
                updateLocationUI();
            }
        };
    }

    /**
     * Build a LocationSettingsRequest to check if a device has the required location settings.
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    private void startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());

                        updateLocationUI();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }

                        updateLocationUI();
                    }
                });
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    /**
     * Start location updates if the correct location permission is available, otherwise request it.
     */
    @Override
    public void onResume() {
        super.onResume();

        if (checkPermissions()) {
            startLocationUpdates();
        } else {
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        }

        updateLocationUI();
    }

    /**
     * Stop location updates when the activity is exited.
     */
    @Override
    protected void onPause() {
        super.onPause();

        stopLocationUpdates();
    }

    /**
     * Store the location data in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable(KEY_LOCATION, mCurrentLocation);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }


    /**
     * Set the MyLocation button callback on the GoogleMap map object when it is ready.
     *
     * @param map: A GoogleMap map object.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        // Enable the MyLocation Button if the Fine location permission is enabled.
        mMap.setOnMyLocationButtonClickListener(this);

        configureMapSettings();

        // The CameraMoveListener redraws the path when the user pans the camera.
        mMap.setOnCameraMoveListener(this);
    }

    /**
     * Update the map pointer position with the current location and store the location in the
     * database.
     */
    private void updateLocationUI() {
        if (mCurrentLocation != null) {
            double latitude = mCurrentLocation.getLatitude();
            double longitude = mCurrentLocation.getLongitude();
            LatLng latLng = new LatLng(latitude, longitude);
            saveCurrentLocation(latLng);

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17);
            mMap.animateCamera(cameraUpdate, new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    drawPathInMapBounds();
                }

                @Override
                public void onCancel() {
                    drawPathInMapBounds();
                }
            });
        }
    }

    /**
     * Save the current location to the database.
     * @param latLng: A LatLng object representing the current location.
     */
    private void saveCurrentLocation(LatLng latLng) {
        LocationObject locationObject = new LocationObject(latLng);
        mSqlDatabaseHelper.addLocation(locationObject);
    }

    /**
     * Draw the visited locations that are in the map's bounds.
     */
    private void drawPathInMapBounds() {
        // Get the map bounds and the LocationObjects in the bounds.
        Projection mapProjection = mMap.getProjection();
        LatLngBounds mapBounds = mapProjection.getVisibleRegion().latLngBounds;

        List<LocationObject> locationObjects = mSqlDatabaseHelper.getLocationsInWindow(mapBounds);

        overlayView.drawPathInMapBounds(locationObjects);
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void configureMapSettings() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app, so configure the view.
            mMap.setMyLocationEnabled(true);
            // And configure the map's UI settings.
            UiSettings uiSettings = mMap.getUiSettings();
            uiSettings.setZoomControlsEnabled(true);
            uiSettings.setCompassEnabled(true);
        }
    }

    /**
     * When the map camera moves, update the path as new path points may have entered the bounds.
     */
    @Override
    public void onCameraMove() {
        drawPathInMapBounds();
    }

    /**
     * Display a toast to the user before panning to their location.
     * @return false to ensure the overridden behaviour still occurs.
     */
    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, R.string.location_button_pressed, Toast.LENGTH_SHORT).show();
        return false;
    }

    /**
     * Handle a Permission Request result.
     *
     * @param requestCode: int representing the request code for permission that was requested.
     * @param permissions: String array of the permissions requested by the app.
     * @param grantResults: int array of the results for each permission request.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // restart the activity to be able to use the permission.
            MainActivity.this.recreate();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    /**
     * Handle a Generic Activity Request result.
     * @param requestCode: int representing the request code used by the activity.
     * @param resultCode: int representing the result of the activity.
     * @param data: Intent containing any data sent back by the activity when it finished.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        break;
                    case Activity.RESULT_CANCELED:
                        // No changes made to the location settings.
                        updateLocationUI();
                        break;
                }
                break;
        }
    }

    /**
     * On resume of the fragments show the missing permission dialog if the Location permission
     * is denied.
     */
    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }
}
