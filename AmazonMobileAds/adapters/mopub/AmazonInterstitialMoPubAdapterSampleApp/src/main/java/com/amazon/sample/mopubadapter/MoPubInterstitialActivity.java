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
import android.widget.Button;

import com.amazon.device.ads.amazonmopubadaptersample.R;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;

/**
 * The main activity which provides the ability to demonstrate the load ad and show ad functionality
 * of an Amazon Interstitial ad through the implementation of MoPub.
 */
public class MoPubInterstitialActivity extends AppCompatActivity implements MoPubInterstitial.InterstitialAdListener {

    private static final String LOG_TAG = MoPubInterstitialActivity.class.getSimpleName();
    /**
     * MoPub's Interstitial object for displaying an interstitial ad
     */
    private MoPubInterstitial moPubInterstitial;
    /**
     * The button for engaging with the show ad functionality
     */
    private Button showAdButton;

    /**
     * When the activity starts, prepare the ad for loading and the user interface for the application.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize the MoPubInterstitial ad
        // the ad unit applied here is specific to this sample application
        this.moPubInterstitial = new MoPubInterstitial(this, getString(R.string.ad_unit));
        this.moPubInterstitial.setInterstitialAdListener(this);
        this.showAdButton = (Button) findViewById(R.id.showAdButton);
    }

    /**
     * Clean up all resources when destroying the acitivity
     */
    @Override
    protected void onDestroy() {
        this.moPubInterstitial.destroy();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Actions to complete when the load ad button is clicked.
     */
    public void loadAdButtonClicked(View view) {
        this.showAdButton.setEnabled(false);
        this.moPubInterstitial.load();
    }

    /**
     * Actions to complete when the show ad button is clicked.
     */
    public void showAdButtonClicked(View view) {
        this.moPubInterstitial.show();
        this.showAdButton.setEnabled(false);
    }

    /**
     * The callback from the MoPub SDK when a interstitial ad loads.
     */
    @Override
    public void onInterstitialLoaded(MoPubInterstitial interstitial) {
        this.showAdButton.setEnabled(true);
        Log.i(LOG_TAG, "MoPub Interstitial loaded successfully.");
    }

    /**
     * The callback from the MoPub SDK when a interstitial ad fails to load.
     */
    @Override
    public void onInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode errorCode) {
        Log.i(LOG_TAG, "MoPub Interstital failed to load. Error: " + errorCode.toString());
    }

    /**
     * The callback from the MoPub SDK when a interstitial ad is shown.
     */
    @Override
    public void onInterstitialShown(MoPubInterstitial interstitial) {
        Log.i(LOG_TAG, "MoPub Interstitial ad shown.");
    }

    /**
     * The callback from the MoPub SDK when a interstitial ad is clicked.
     */
    @Override
    public void onInterstitialClicked(MoPubInterstitial interstitial) {
        Log.i(LOG_TAG, "MoPub Interstitial ad clicked.");
    }

    /**
     * The callback from the MoPub SDK when a interstitial ad is dismissed.
     */
    @Override
    public void onInterstitialDismissed(MoPubInterstitial interstitial) {
        Log.i(LOG_TAG, "MoPub Interstitial ad dismissed.");
    }
}
