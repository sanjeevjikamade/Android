/*
 * CoffeeActivity.java
 *
 * Copyright (c) 2010-2014 Amazon.com, Inc. or its affiliates. All rights reserved.
 *
 * Use is subject to license terms.
 */

package com.example.amazon.capitolhillcoffee;

import android.app.Activity;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

import com.amazon.geo.mapsv2.AmazonMap;
import com.amazon.geo.mapsv2.CameraUpdate;
import com.amazon.geo.mapsv2.CameraUpdateFactory;
import com.amazon.geo.mapsv2.MapFragment;
import com.amazon.geo.mapsv2.OnMapReadyCallback;
import com.amazon.geo.mapsv2.model.LatLng;
import com.amazon.geo.mapsv2.model.LatLngBounds;
import com.amazon.geo.mapsv2.util.AmazonMapsRuntimeUtil;
import com.amazon.geo.mapsv2.util.ConnectionResult;

/**
 *
 * Activity class for coffee shop app.
 *
 */
public class CoffeeActivity extends Activity {
    // Zoom used when displaying user's location
    private static final int FINDME_ZOOM = 18;

    private MapFragment mMapFragment;
    private AmazonMap mMap;
    private final CoffeeFetcher mCoffeeFetcher = new CoffeeFetcher();

    /**
     * Upon creation, this demo app:
     *     - creates a stock MapFragment
     *     - adds two navigation buttons: one to find coffee,
     *       one to find user's location
     *     - adds coffee shop markers to display coffee shops
     *     - navigates the map to display all the coffee shops
     */
    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.main);

        // Extract a reference to the map fragment
        mMapFragment = (MapFragment) getFragmentManager().findFragmentById(
                R.id.mapfragment);

        // Add 'find coffee' button to top left corner
        final ImageView coffeeImage = (ImageView) findViewById(R.id.find_coffee);
        // When the user clicks, navigate to coffee shops
        coffeeImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (mMap != null) {
                    findCoffee();
                }
            }
        });

        // Add 'find me' button to top right corner
        final ImageView image = (ImageView) findViewById(R.id.find_me);
        // When the user clicks, navigate to my location (if known)
        image.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (mMap != null) {
                    findMe();
                }
            }
        });
    }

    /**
     * Show or hide UI based on Maps API being installed.
     */
    @Override
    public void onResume() {
        super.onResume();

        int resultCode = AmazonMapsRuntimeUtil
                .isAmazonMapsRuntimeAvailable(getApplicationContext());
        boolean mapsRuntimeAvailable = resultCode == ConnectionResult.SUCCESS;

        final ImageView image = (ImageView) findViewById(R.id.find_me);
        final ImageView coffeeImage = (ImageView) findViewById(R.id.find_coffee);
        image.setVisibility(mapsRuntimeAvailable ? View.VISIBLE
                : View.INVISIBLE);
        coffeeImage.setVisibility(mapsRuntimeAvailable ? View.VISIBLE
                : View.INVISIBLE);
        if (mapsRuntimeAvailable) {
            // Turn my location on, so that it displays user's location (if any)
            mMapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(AmazonMap map) {
                    mMap = map;
                    mMap.getUiSettings()
                    .setMyLocationButtonEnabled(true);
                }
            });
        } else {
            AmazonMapsRuntimeUtil.getErrorDialog(resultCode, this, 0).show();
        }
    }

    /**
     * Display coffee shops.
     *
     * For this demo app, the coffee shop data is static and only covers coffee
     * shops in a small area of Capitol Hill, Seattle. Therefore, the 'find
     * coffee' button first adds the coffee shop markers then moves the map to display
     * them.
     *
     * In a production app, this button would instead display coffee shops
     * within the visible region of the map view. (The visible region can be
     * determined by calling map.getProjection().getVisibleRegion().)
     * It would do so by querying a custom coffee shop webservice which would
     * return coffee shop data for that visible region.
     */
    private void findCoffee() {
        // Set center and zoom so that capitol hill is visible
        LatLngBounds coffeeBounds = mCoffeeFetcher.addShops(this, mMap);

        // Ideally this would come from a dimens.xml file.
        int cameraPadding = 100;

        // Animate the camera to the coffee shops.
        final CameraUpdate update = CameraUpdateFactory.newLatLngBounds(
                coffeeBounds, cameraPadding);
        mMap.animateCamera(update);
    }

    /**
     * Set center and zoom to the user's location
     */
    private void findMe() {
        // Obtain the last known location
        final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location bestLocation = null;
        for (String provider : locationManager.getProviders(true)) {
            final Location location = locationManager
                    .getLastKnownLocation(provider);
            if (location != null
                    && (bestLocation == null || location.getAccuracy() < bestLocation
                            .getAccuracy())) {
                // Note: we might want to ignore 'stale' location readings
                bestLocation = location;
            }
        }

        if (bestLocation == null) {
            Toast.makeText(this, "Could not obtain location", Toast.LENGTH_LONG)
                    .show();
        } else {
            final LatLng myLocation = new LatLng(bestLocation.getLatitude(),
                    bestLocation.getLongitude());
            final CameraUpdate update = CameraUpdateFactory.newLatLngZoom(
                    myLocation, FINDME_ZOOM);
            mMap.animateCamera(update);
        }
    }
}
