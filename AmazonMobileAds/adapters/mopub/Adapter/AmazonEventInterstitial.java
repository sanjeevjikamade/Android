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
package com.mopub.mobileads;

import java.util.Iterator;
import java.util.Map;

import com.amazon.device.ads.Ad;
import com.amazon.device.ads.AdError;
import com.amazon.device.ads.AdListener;
import com.amazon.device.ads.AdProperties;
import com.amazon.device.ads.AdRegistration;
import com.amazon.device.ads.AdTargetingOptions;
import com.amazon.device.ads.InterstitialAd;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * AmazonEventInterstitial extends MoPub's CustomEventInterstitial to allow developers to
 * easily display Amazon Banner Ads through the MoPub SDK.
 */
public class AmazonEventInterstitial extends CustomEventInterstitial {

    private static final String LOG_TAG = AmazonEventInterstitial.class.getSimpleName();
    private static final String SLOT_KEY = "slot";
    private static final String SLOT_VALUE = "MoPubAMZN";
    private static final String PK_KEY = "pk";
    private static final String PK_VALUE = "[AndroidMoPubAdapter-1.1]";

    // keys for Amazon configuration in serviceExtras map from MoPub server
    private static final String APP_KEY = "appKey";
    private static final String LOGGING_ENABLED_KEY = "loggingEnabled";
    private static final String TESTING_ENABLED_KEY = "testingEnabled";
    private static final String GEOLOCATION_ENABLED_KEY = "geolocationEnabled";
    private static final String ADVANCED_OPTIONS_KEY = "advOptions";

    private CustomEventInterstitialListener mopubInterstitialListener;
    private InterstitialAd amazonInterstitial;

    /**
     * Executed by MoPub when this CustomEventBanner is utilized for loading banner ads.
     */
    @Override
    protected void loadInterstitial(Context context, CustomEventInterstitialListener customEventInterstitialListener, Map<String, Object> localExtras, Map<String, String> serviceExtras) {
        // save the listener so its callbacks can be executed later
        mopubInterstitialListener = customEventInterstitialListener;

        // configure AdRegistration with mopub server parameters
        AdRegistration.setAppKey(serviceExtras.get(APP_KEY));
        AdRegistration.enableLogging(Boolean.parseBoolean(serviceExtras.get(LOGGING_ENABLED_KEY)));
        AdRegistration.enableTesting(Boolean.parseBoolean(serviceExtras.get(TESTING_ENABLED_KEY)));

        // Initialize the interstitial ad
        amazonInterstitial = new InterstitialAd(context);
        amazonInterstitial.setListener(new InterstitialAdListener());

        // retrieve any advanced options set through the mopub server values
        final AdTargetingOptions adTargetingOptions = new AdTargetingOptions();
        final String advOptions = serviceExtras.get(ADVANCED_OPTIONS_KEY);
        if (advOptions != null && !advOptions.isEmpty()) {
            try {
                final JSONObject advOptionsJson = new JSONObject(advOptions);
                final Iterator<String> keysIt = advOptionsJson.keys();
                while(keysIt.hasNext()) {
                    final String key = keysIt.next();
                    final String value = advOptionsJson.getString(key);
                    adTargetingOptions.setAdvancedOption(key, value);
                }
            } catch (JSONException ex) {
                Log.d(LOG_TAG, "Error converting advOptions JSON.");
            }
        }

        // enable geolocation based on mopub server value
        final boolean geolocationEnabled = Boolean.parseBoolean(serviceExtras.get(GEOLOCATION_ENABLED_KEY));
        adTargetingOptions.enableGeoLocation(geolocationEnabled);


        // Loading ads with AdTargetingOptions populated with slot and pk values will
        // help to identify and troubleshoot with application developers using this adapter
        adTargetingOptions.setAdvancedOption(SLOT_KEY, SLOT_VALUE);
        adTargetingOptions.setAdvancedOption(PK_KEY, PK_VALUE);
        amazonInterstitial.loadAd(adTargetingOptions);
    }

    /**
     * Executed by MoPub when a developer shows a MoPub Interstitial ad.
     */
    @Override
    protected void showInterstitial() {
        amazonInterstitial.showAd();
    }

    /**
     * Cleanup any resources
     */
    @Override
    protected void onInvalidate() {
    }

    /**
     * Implements AdListener for receiving callbacks from the Amazon Ads SDK.
     */
    private class InterstitialAdListener implements AdListener {

        /**
         * This callback is executed when an Amazon Interstitial Ad loads successfully.
         */
        @Override
        public void onAdLoaded(Ad ad, AdProperties adProperties) {
            mopubInterstitialListener.onInterstitialLoaded();
        }

        /**
         * This callback is executed when an Amazon Interstitial Ad fails to load.
         */
        @Override
        public void onAdFailedToLoad(Ad ad, AdError adError) {
            final MoPubErrorCode moPubErrorCode = convertToMoPubErrorCode(adError);
            mopubInterstitialListener.onInterstitialFailed(moPubErrorCode);
        }

        /**
         * This callback is executed when an Amazon Interstitial Ad is expanded.
         */
        @Override
        public void onAdExpanded(Ad ad) {
            Log.i(LOG_TAG, "Amazon Interstitial Ad Expanded.");
        }

        /**
         * This callback is executed when an Amazon Interstitial Ad is collapsed.
         */
        @Override
        public void onAdCollapsed(Ad ad) {
            Log.i(LOG_TAG, "Amazon Interstitial Ad Collapsed.");
        }

        /**
         * This callback is executed when an Amazon Interstitial Ad is dismissed.
         */
        @Override
        public void onAdDismissed(Ad ad) {
            mopubInterstitialListener.onInterstitialDismissed();
        }

        /**
         * Converts the Amazon AdError to a MoPubErrorCode
         * @param adError the Amazon AdError
         * @return MoPubErrorCode mapped to Amazon AdError, defaults to MoPubErrorCode.UNSPECIFIED
         */
        private MoPubErrorCode convertToMoPubErrorCode(final AdError adError) {
            final AdError.ErrorCode errorCode = adError.getCode();
            if (errorCode.equals(AdError.ErrorCode.NO_FILL)) {
                return MoPubErrorCode.NETWORK_NO_FILL;
            }
            if (errorCode.equals(AdError.ErrorCode.NETWORK_ERROR)) {
                return MoPubErrorCode.NETWORK_INVALID_STATE;
            }
            if (errorCode.equals(AdError.ErrorCode.NETWORK_TIMEOUT)) {
                return MoPubErrorCode.NETWORK_TIMEOUT;
            }
            if (errorCode.equals(AdError.ErrorCode.INTERNAL_ERROR)) {
                return MoPubErrorCode.INTERNAL_ERROR;
            }
            return MoPubErrorCode.UNSPECIFIED;
        }
    }
}


