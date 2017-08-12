package com.danielcswain.fogofwar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.danielcswain.fogofwar.OpenSourcePackages.PermissionUtils;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

public class MainActivity extends AppCompatActivity
        implements
        OnMyLocationButtonClickListener,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

    // Location permissions.
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean mPermissionDenied = false;

    private GoogleMap mMap;

    /**
     * Create the Activity, initalising the layout and toolbar as well as the map fragment.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the ActionBar.
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        // Show the Google Maps fragment.
        FragmentManager myFragmentManager = getSupportFragmentManager();
        SupportMapFragment mapFragment = (SupportMapFragment) myFragmentManager
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
        enableMyLocation();
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
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
