/**
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at http://aws.amazon.com/apache2.0/
 * or in the "license" file accompanying this file.
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazon.sample.mopubadapter;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import com.amazon.device.ads.amazonmopubadaptersampleapp.R;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubView;

/**
 * The main activity for interacting with the MoPub Banner Sample App
 */
public class MoPubBannerActivity extends AppCompatActivity implements MoPubView.BannerAdListener {

    private static final String LOG_TAG = MoPubBannerActivity.class.getSimpleName();
    private MoPubView moPubView;

    /**
     * When the activity starts, set up the mo pub banner ad.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        moPubView = (MoPubView) findViewById(R.id.adview);
        moPubView.setAdUnitId(getString(R.string.ad_unit));
        moPubView.setBannerAdListener(this);
    }

    /**
     * When the activity is destroyed, destroy the mo pub banner ad.
     */
    @Override
    protected void onDestroy() {
        this.moPubView.destroy();
        super.onDestroy();
    }

    /**
     * When the options menu is created. inflate it.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * The actions to perform when the user clicks the load ad button.
     */
    public void loadAdButtonClicked(View view) {
        this.moPubView.loadAd();
    }

    /**
     * The callback from the MoPub SDK when a banner ad loads.
     */
    @Override
    public void onBannerLoaded(MoPubView banner) {
        Log.i(LOG_TAG, "MoPub banner loaded successfully.");
    }

    /**
     * The callback from the MoPub SDK when a banner ad fails to load.
     */
    @Override
    public void onBannerFailed(MoPubView banner, MoPubErrorCode errorCode) {
        Log.i(LOG_TAG, "MoPub banner failed to load. Error: " + errorCode.toString());
    }

    /**
     * The callback from the MoPub SDK when a banner ad is clicked.
     */
    @Override
    public void onBannerClicked(MoPubView banner) {
        Log.i(LOG_TAG, "MoPub banner ad clicked.");
    }

    /**
     * The callback from the MoPub SDK when a banner ad is expanded.
     */
    @Override
    public void onBannerExpanded(MoPubView banner) {
        Log.i(LOG_TAG, "MoPub banner ad expanded.");
    }

    /**
     * The callback from the MoPub SDK when a banner ad is collapsed.
     */
    @Override
    public void onBannerCollapsed(MoPubView banner) {
        Log.i(LOG_TAG, "MoPub banner ad collapsed.");
    }
}
