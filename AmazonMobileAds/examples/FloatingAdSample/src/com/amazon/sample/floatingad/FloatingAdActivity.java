/**
 * Copyright 2013-2014 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at http://aws.amazon.com/apache2.0/
 * or in the "license" file accompanying this file.
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amazon.sample.floatingad;

import com.amazon.device.ads.*;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;
import android.view.animation.*;
import android.view.animation.Animation.AnimationListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import android.widget.FrameLayout.LayoutParams;
import android.util.*;

/**
 * This is a simple app for testing ad loading capabilities of the Amazon Mobile Ads API. 
 * This app demonstrates how to load an ad that floats in from the bottom of the screen.
 */
public class FloatingAdActivity extends Activity {
    private static final String APP_KEY = "sample-app-v1_pub-2"; // Sample Application Key. Replace this value with your Application Key.
    private static final String LOG_TAG = "FloatingAdSample"; // Tag used to prefix all log messages.
    
    private ViewGroup adViewContainer; // View group to which the ad view will be added.
    private AdLayout currentAdView; // The ad that is currently visible to the user.
    private AdLayout nextAdView; // A placeholder for the next ad so we can keep the current ad visible while the next ad loads.
    
    /**
     * When the activity starts, load an ad and set up the button's click event to load another ad when it's clicked.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        this.adViewContainer = (ViewGroup) findViewById(R.id.root_layout);
        
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
                
        // Assign an onClick handler to the button that will load our ad.
        final Button button = (Button) findViewById(R.id.load_ad_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View v) {
                loadAd();
            }
        });
        
        // Load a web view. This web view will appear in the background in order to help illustrate how the
        // floating ad overlays other content on the screen.
        final WebView webView = (WebView) findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
                view.loadUrl(url);
                return true;
            }
        });
        webView.loadUrl("http://www.amazon.com");
        
        // Load the ad when the activity starts.
        loadAd();
    }

    /**
     * Clean up all ad view resources when destroying the activity.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.currentAdView != null)
            this.currentAdView.destroy();
        if (this.nextAdView != null)
            this.nextAdView.destroy();
    }
    
    /**
     * Loads a new ad. Keeps the current ad visible while the next ad loads.
     */
    private void loadAd() {
        if (this.nextAdView == null) { // Create and configure a new ad if the next ad doesn't currently exist.
            this.nextAdView = new AdLayout(this, AdSize.SIZE_AUTO_NO_SCALE);
            final LayoutParams layoutParams = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            
            // Note: The above implementation is for an auto-sized ad in an AdLayout of width MATCH_PARENT and
            // height WRAP_CONTENT. The rendered ad will retain its original device-independent pixel size and
            // will not scale up to fill its container. If you want the ad to fill its container then you can 
            // instead specify your size as AdSize.SIZE_AUTO or, equivalently, leave out the AdSize parameter.
            //
            // Alternatively, if you want to prevent automatic size selection and give the ad the same fixed 
            // size on all devices, you will need to factor in the phone's scale when setting up the AdLayout 
            // dimensions. See the example below for 320x50 dip:
            // 
            //    this.nextAdView = new AdLayout(this, AdSize.SIZE_320x50);
            //    final float scale = this.getApplicationContext().getResources().getDisplayMetrics().density;
            //    final LayoutParams layoutParams = new FrameLayout.LayoutParams((int) (320 * scale),
            //             (int) (50 * scale), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            
            this.nextAdView.setLayoutParams(layoutParams);
            // Register our ad handler that will receive callbacks for state changes during the ad lifecycle.
            this.nextAdView.setListener(new SampleAdListener());
        }
                
        // Load the ad with default ad targeting.
        this.nextAdView.loadAd();
        
        // Note: You can choose to provide additional targeting information to modify how your ads
        // are targeted to your users. This is done via an AdTargetingOptions parameter that's passed
        // to the loadAd call. See an example below:
        //
        //    final AdTargetingOptions adOptions = new AdTargetingOptions();
        //    adOptions.enableGeoLocation(true);
        //    this.nextAdView.loadAd(adOptions);
    }
    
    /**
     * Shows the ad that is in the current ad view to the user.
     */
    private void showCurrentAd() {
        this.adViewContainer.addView(this.currentAdView);
        final Animation slideUp = AnimationUtils.loadAnimation(FloatingAdActivity.this, R.anim.slide_up);
        this.currentAdView.startAnimation(slideUp);
    }
    
    /**
     * Shows the ad that is in the next ad view to the user.
     */
    private void showNextAd() {
        this.adViewContainer.removeView(this.currentAdView);
        final AdLayout tmp = this.currentAdView;
        this.currentAdView = this.nextAdView;
        this.nextAdView = tmp;
        showCurrentAd();
    }
    
    /**
     * Hides the ad that is in the current ad view, and then displays the ad that is in the next ad view.
     */
    private void swapCurrentAd() {
        final Animation slideDown = AnimationUtils.loadAnimation(FloatingAdActivity.this, R.anim.slide_down);
        slideDown.setAnimationListener(new AnimationListener() {
            public void onAnimationEnd(final Animation animation) {
               showNextAd();
            }

            public void onAnimationRepeat(final Animation animation) {
            }
            
            public void onAnimationStart(final Animation animation) {
            }
        });
        this.currentAdView.startAnimation(slideDown);
    }

    /**
     * This class is for an event listener that tracks ad lifecycle events.
     * It extends DefaultAdListener, so you can override only the methods that you need.
     */
    class SampleAdListener extends DefaultAdListener {
        /**
         * This event is called once an ad loads successfully.
         */
        @Override
        public void onAdLoaded(final Ad ad, final AdProperties adProperties) {
            Log.i(LOG_TAG, adProperties.getAdType().toString() + " ad loaded successfully.");
            // If there is an ad currently being displayed, swap the ad that just loaded with current ad.
            // Otherwise simply display the ad that just loaded.
            if (FloatingAdActivity.this.currentAdView != null) {
                swapCurrentAd();
            } else {
                // This is the first time we're loading an ad, so set the
                // current ad view to the ad we just loaded and set the next to null
                // so that we can load a new ad in the background.
                FloatingAdActivity.this.currentAdView = FloatingAdActivity.this.nextAdView;
                FloatingAdActivity.this.nextAdView = null;
                showCurrentAd();
            }
        }
    
        /**
         * This event is called if an ad fails to load.
         */
        @Override
        public void onAdFailedToLoad(final Ad ad, final AdError error) {
            Log.w(LOG_TAG, "Ad failed to load. Code: " + error.getCode() + ", Message: " + error.getMessage());
        }
    
        /**
         * This event is called after a rich media ad expands.
         */
        @Override
        public void onAdExpanded(final Ad ad) {
            Log.i(LOG_TAG, "Ad expanded.");
            // You may want to pause your activity here.
        }
    
        /**
         * This event is called after a rich media ad has collapsed from an expanded state.
         */
        @Override
        public void onAdCollapsed(final Ad ad) {
            Log.i(LOG_TAG, "Ad collapsed.");
            // Resume your activity here, if it was paused in onAdExpanded.
        }
    }
}
