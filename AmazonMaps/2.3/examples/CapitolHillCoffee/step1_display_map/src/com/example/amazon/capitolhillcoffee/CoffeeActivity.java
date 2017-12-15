/*
 * CoffeeActivity.java
 *
 * Copyright (c) 2010-2014 Amazon.com, Inc. or its affiliates. All rights reserved.
 *
 * Use is subject to license terms.
 */

package com.example.amazon.capitolhillcoffee;

import android.app.Activity;
import android.os.Bundle;

import com.amazon.geo.mapsv2.MapFragment;
import com.amazon.geo.mapsv2.util.AmazonMapsRuntimeUtil;
import com.amazon.geo.mapsv2.util.ConnectionResult;

/**
 *
 * Activity class for coffee shop app.
 *
 */
public class CoffeeActivity extends Activity {
    @SuppressWarnings("unused")
    private MapFragment mMapFragment;

    /**
     * Upon creation, this demo app:
     *     - creates a stock MapFragment
     */
    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.main);

        // Extract a reference to the map fragment
        mMapFragment = (MapFragment) getFragmentManager().findFragmentById(
                R.id.mapfragment);
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

        if (!mapsRuntimeAvailable) {
            AmazonMapsRuntimeUtil.getErrorDialog(resultCode, this, 0).show();
        }
    }
}
