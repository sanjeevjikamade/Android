/**
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at http://aws.amazon.com/apache2.0/
 * or in the "license" file accompanying this file.
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazon.sample.framedmodelessinterstitialad;

import com.amazon.device.ads.*;
import com.amazon.sample.framedmodelessinterstitialad.R;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

/**
 * This is a simple app for testing modeless interstitial ad loading capabilities of the Amazon Mobile Ads API.
 */
public class FramedModelessInterstitialAdActivity extends Activity {
    private static final String APP_KEY = "sample-app-v1_pub-2"; // Sample Application Key. Replace this value with your Application Key.
    private static final String LOG_TAG = "FramedModelessInterstitialAdSample"; // Tag used to prefix all log messages.
    
    private ViewGroup adOverallContainer;
    private FrameLayout adContainer;
    private ModelessInterstitialAd modelessInterstitialAd;
    private ViewGroup modelessInterstitialAdFrame;
    private Button loadButton;
    private Button showButton;
    private ImageButton adCloseButton;
    private boolean adShowing;
    private boolean adLoaded;

    /**
     * When the activity starts, set up the click events for the buttons so that they load or show ads when clicked.
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Note: Hiding the action bar is optional for the developer.
        // It is done here to allow the ad to have more screen real estate.
        final ActionBar actionBar = getActionBar();
        actionBar.hide();
        
        // For debugging purposes enable logging, but disable for production builds.
        AdRegistration.enableLogging(true);
        // For debugging purposes flag all ad requests as tests, but set to false for production builds.
        AdRegistration.enableTesting(true);
        
        try {
            AdRegistration.setAppKey(APP_KEY);
        } catch (final IllegalArgumentException e) {
            Log.e(LOG_TAG, "IllegalArgumentException thrown: " + e.toString());
            return;
        }
        
        this.modelessInterstitialAdFrame = new FrameLayout(this);
        final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, 
                FrameLayout.LayoutParams.MATCH_PARENT);
        this.modelessInterstitialAdFrame.setLayoutParams(layoutParams);
        
        this.modelessInterstitialAd = new ModelessInterstitialAd(this.modelessInterstitialAdFrame);
        this.modelessInterstitialAd.setListener(new SampleAdListener());

        initializeUI();
    }
    
    @Override
    protected void onDestroy() {
        this.modelessInterstitialAd.destroy();
        this.modelessInterstitialAd = null;
        
        super.onDestroy();
    }
    
    @Override
    public void onBackPressed() {
        if (this.adShowing) {
            hideAd();
        } else {
            super.onBackPressed();
        }
    }
    
    private void initializeUI() {
        final RelativeLayout parentLayout = (RelativeLayout) findViewById(R.id.parent_layout);
        parentLayout.setBackgroundResource(R.drawable.gradient);
        
        this.adOverallContainer = (ViewGroup) findViewById(R.id.ad_overall_container);
        this.adContainer = (FrameLayout) findViewById(R.id.ad_container);
        this.adContainer.addView(this.modelessInterstitialAdFrame);
        
        // Assign an onClick handler to the button that will load our ad.
        this.loadButton = (Button) findViewById(R.id.load_ad_button);
        this.loadButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View v) {
                loadAd();
            }
        });

        // The show button will be disabled when the ad is unable to be shown.
        this.showButton = (Button) findViewById(R.id.show_ad_button);
        this.showButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View v) {
                showAd();
            }
        });
        
        if (this.adLoaded) {
            this.loadButton.setEnabled(true);
            this.showButton.setEnabled(true);
        }

        this.adCloseButton = (ImageButton) findViewById(R.id.ad_close_button);
        this.adCloseButton.setImageResource(R.drawable.close_normal);
        this.adCloseButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int action = event.getAction();
                final ImageButton showButton = (ImageButton) v;
                if (action == MotionEvent.ACTION_DOWN) {
                    showButton.setImageResource(R.drawable.close_pressed);
                } else if (action == MotionEvent.ACTION_UP || 
                        action == MotionEvent.ACTION_CANCEL) {
                    showButton.setImageResource(R.drawable.close_normal);
                }
                return false;
            }
        });
        this.adCloseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View v) {
                hideAd();
            }
        });
    }
    
    /**
     * Load a new modeless interstitial ad in the background.
     * This action is generally recommended during the activity's start, after a previous ad has been shown, 
     * after a previous ad has failed to show, or thirty seconds after a previous ad has failed to load.
     */
    private void loadAd() {
        this.loadButton.setEnabled(false);
        this.showButton.setEnabled(false);
        this.adLoaded = false;
        
        // Layout on the views must have run in order for to get their width and height.
        // Once it has run (such as after onCreate), set the layout parameters on the frame and add it to the view.
        // Layout will run on the frame and that will need to have happened before the ad is shown.
        this.adOverallContainer.setVisibility(View.INVISIBLE);

        // Load an ad with default ad targeting.
        if (this.modelessInterstitialAd.loadAd()) {
            // Once an ad has started to load, new ads cannot load until the loading ad's lifecycle is complete.
            // We disable the load button here and do not re-enable it until the load action becomes available again. 
            this.loadButton.setEnabled(false);
        } else {
            Log.w(LOG_TAG, "The ad could not be loaded. Check the logcat for more information.");
        }

        // Note: You can choose to provide additional targeting information to modify how your ads
        // are targeted to your users. This is done via an AdTargetingOptions parameter that's passed
        // to the loadAd call. See an example below:
        //
        //    final AdTargetingOptions adOptions = new AdTargetingOptions();
        //    adOptions.enableGeoLocation(true);
        //    if (this.interstitialAd.loadAd(adOptions)) ...
    }

    /**
     * Display the previously-loaded interstitial ad on the screen.
     * This action is generally recommended during a natural transition point in your app.
     */
    private void showAd() {
        if (!this.adShowing) {
            // Once the ad is ready to show to the user, set the visibility of the frame so that it will appear on screen.
            this.adOverallContainer.setVisibility(View.VISIBLE);

            if (this.modelessInterstitialAd.adShown()) {
                this.adShowing = true;
            } else {
                this.adOverallContainer.setVisibility(View.INVISIBLE);
            }
            this.loadButton.setEnabled(true);
        }
    }

    private void hideAd() {
        this.adOverallContainer.setVisibility(View.INVISIBLE);
        this.adShowing = false;
        this.modelessInterstitialAd.adHidden();
    }

    /**
     * This class is for an event listener that tracks ad lifecycle events.
     * It extends DefaultAdListener, so you can override only the methods that you need.
     * In this case, there is no need to override methods specific to expandable ads.
     */
    private class SampleAdListener extends DefaultAdListener {
        /**
         * This event is called once an ad loads successfully.
         */
        @Override
        public void onAdLoaded(final Ad ad, final AdProperties adProperties) {
            Log.i(LOG_TAG, adProperties.getAdType().toString() + " ad loaded successfully.");

            FramedModelessInterstitialAdActivity.this.adLoaded = true;
            
            // Once an interstitial ad has been loaded, it can then be shown.
            FramedModelessInterstitialAdActivity.this.showButton.setEnabled(true);
        }
    
        /**
         * This event is called if an ad fails to load.
         */
        @Override
        public void onAdFailedToLoad(final Ad view, final AdError error) {
            Log.w(LOG_TAG, "Ad failed to load. Code: " + error.getCode() + ", Message: " + error.getMessage());

            // A new load action may be attempted once the previous one has returned a failure callback.
            FramedModelessInterstitialAdActivity.this.loadButton.setEnabled(true);
        }
    }
}
