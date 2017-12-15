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
package com.amazon.sample.simplead;

import com.amazon.device.ads.*;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * This is a simple app for testing ad loading capabilities of the Amazon Mobile Ads API. 
 * This app demonstrates ad loading, expanding and collapsing along with device rotation.
 */
public class SimpleAdActivity extends Activity {
    private AdLayout adView; // The ad view used to load and display the ad.
    private static final String APP_KEY = "sample-app-v1_pub-2"; // Sample Application Key. Replace this value with your Application Key.
    private static final String LOG_TAG = "SimpleAdSample"; // Tag used to prefix all log messages.
    private Button showAdButton;
    private Button loadAdButton;
    private CheckBox autoShowCheckBox;
    /**
     * When the activity starts, load an ad and set up the button's click event to load another ad when it's clicked.
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // For debugging purposes enable logging, but disable for production builds.
        AdRegistration.enableLogging(true);
        // For debugging purposes flag all ad requests as tests, but set to false for production builds.
        AdRegistration.enableTesting(true);
        
        this.adView = (AdLayout) findViewById(R.id.ad_view);
        this.adView.setListener(new SampleAdListener());

        try {
            AdRegistration.setAppKey(APP_KEY);
        } catch (final IllegalArgumentException e) {
            Log.e(LOG_TAG, "IllegalArgumentException thrown: " + e.toString());
            return;
        }

        this.autoShowCheckBox = ((CheckBox)findViewById(R.id.banner_ads_showAdCheckbox));

        //By default ads automatically show after loading
        //Disable automatic showing of the ad after it is loaded if the option is not checked
        if (!this.autoShowCheckBox.isChecked()) {
            this.adView.disableAutoShow();
        }

        //Assign an onCheckedChange handler to control the autoShow property with the check box
        this.autoShowCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                if (isChecked) {
                    adView.enableAutoShow();
                }
                else {
                    adView.disableAutoShow();
                }
            }
        });

        // Assign an onClick handler to the button that will load our ad.
        this.loadAdButton = (Button) findViewById(R.id.load_ad_button);
        this.loadAdButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View v) {
                loadAd();
            }
        });
        
        // Assign an onClick handler to the load button that will show our ad.
        this.showAdButton = (Button) findViewById(R.id.show_ad_button);
        this.showAdButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View v) {
                showAd();
            }
        });
    }
    
    /**
     * Load a new ad.
     */
    public void loadAd() {
        // Load an ad with default ad targeting.
        this.adView.loadAd();
        
        // Note: You can choose to provide additional targeting information to modify how your ads
        // are targeted to your users. This is done via an AdTargetingOptions parameter that's passed
        // to the loadAd call. See an example below:
        //
        //    final AdTargetingOptions adOptions = new AdTargetingOptions();
        //    adOptions.enableGeoLocation(true);
        //    this.adView.loadAd(adOptions);
    }
    
    public void showAd() {
        if (!this.adView.showAd()) {
            Log.w(LOG_TAG, "The ad was not shown. Check the logcat for more information.");
        }

        // If the show action fails, a new ad must be loaded before another ad can be shown.
        // Once the ad is shown, the Show Ad button is disabled and the Load Ad button is enabled to be able to load more ads.
        this.loadAdButton.setEnabled(true);
        this.showAdButton.setEnabled(false);
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

            //Once a banner ad has been loaded, it can be shown
            if (!SimpleAdActivity.this.autoShowCheckBox.isChecked()) {
                SimpleAdActivity.this.showAdButton.setEnabled(true);
                SimpleAdActivity.this.loadAdButton.setEnabled(false);
            }
        }
        
        /**
         * This event is called if an ad fails to load.
         */
        @Override
        public void onAdFailedToLoad(final Ad ad, final AdError error) {
            Log.w(LOG_TAG, "Ad failed to load. Code: " + error.getCode() + ", Message: " + error.getMessage());
            SimpleAdActivity.this.loadAdButton.setEnabled(true);
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
