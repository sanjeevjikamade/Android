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

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

import com.amazon.device.ads.Ad;
import com.amazon.device.ads.AdError;
import com.amazon.device.ads.AdLayout;
import com.amazon.device.ads.AdListener;
import com.amazon.device.ads.AdProperties;
import com.amazon.device.ads.AdRegistration;
import com.amazon.device.ads.AdSize;
import com.amazon.device.ads.AdTargetingOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

/**
 * AmazonEventBanner extends MoPub's CustomEventBanner to allow developers to
 * easily display Amazon Banner Ads through the MoPub SDK.
 */
public class AmazonEventBanner extends CustomEventBanner {

    private static final String LOG_TAG = AmazonEventBanner.class.getSimpleName();
    private static final String SLOT_KEY = "slot";
    private static final String SLOT_VALUE = "MoPubAMZN";
    private static final String PK_KEY = "pk";
    private static final String PK_VALUE = "[AndroidMoPubAdapter-1.1]";

    // keys for ad size in localExtras map from MoPub server
    private static final String MOPUB_AD_HEIGHT_KEY = "com_mopub_ad_height";
    private static final String MOPUB_AD_WIDTH_KEY = "com_mopub_ad_width";

    // keys for Amazon configuration in serviceExtras map from MoPub server
    private static final String APP_KEY = "appKey";
    private static final String LOGGING_ENABLED_KEY = "loggingEnabled";
    private static final String TESTING_ENABLED_KEY = "testingEnabled";
    private static final String SCALING_ENABLED_KEY = "scalingEnabled";
    private static final String GEOLOCATION_ENABLED_KEY = "geolocationEnabled";
    private static final String ADVANCED_OPTIONS_KEY = "advOptions";

    private CustomEventBannerListener mopubBannerListener;
    private AdLayout amazonBanner;

    /**
     * Executed by MoPub when this CustomEventBanner is utilized for loading banner ads.
     */
    @Override
    protected void loadBanner(Context context, CustomEventBannerListener customEventBannerListener, Map<String, Object> localExtras, Map<String, String> serviceExtras) {
        // save the listener so it's callbacks can be executed later
        mopubBannerListener = customEventBannerListener;

        // configure AdRegistration with mopub server parameters
        AdRegistration.setAppKey(serviceExtras.get(APP_KEY));
        AdRegistration.enableLogging(Boolean.parseBoolean(serviceExtras.get(LOGGING_ENABLED_KEY)));
        AdRegistration.enableTesting(Boolean.parseBoolean(serviceExtras.get(TESTING_ENABLED_KEY)));

        // determine the Amazon AdSize to request
        final int adWidth = (Integer)localExtras.get(MOPUB_AD_WIDTH_KEY);
        final int adHeight = (Integer)localExtras.get(MOPUB_AD_HEIGHT_KEY);
        AdSize amazonAdSize = convertToAmazonAdSize(adWidth, adHeight);

        // disable scaling of ads based on mopub server values
        final boolean scalingEnabled = Boolean.parseBoolean(serviceExtras.get(SCALING_ENABLED_KEY));
        if (!scalingEnabled)
            amazonAdSize = amazonAdSize.disableScaling();

        // initialize AdLayout
        amazonBanner = new AdLayout(context, amazonAdSize);
        amazonBanner.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        amazonBanner.setListener(new AmazonAdBannerListener());

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
                Log.e(LOG_TAG, "Error converting advOptions JSON.");
            }
        }

        // enable geolocation based on mopub server value
        final boolean geolocationEnabled = Boolean.parseBoolean(serviceExtras.get(GEOLOCATION_ENABLED_KEY));
        adTargetingOptions.enableGeoLocation(geolocationEnabled);

        // Loading ads with AdTargetingOptions populated with slot and pk values will
        // help to identify and troubleshoot with application developers using this adapter
        adTargetingOptions.setAdvancedOption(SLOT_KEY, SLOT_VALUE);
        adTargetingOptions.setAdvancedOption(PK_KEY, PK_VALUE);
        amazonBanner.loadAd(adTargetingOptions);
    }

    /**
     * Clean up any resources
     */
    @Override
    protected void onInvalidate() {
        amazonBanner.destroy();
    }


    /**
     * Converts the requested ad size from the mopub server to an Amazon AdSize object.
     * @param adWidth the width of the requested ad
     * @param adHeight the height of the requested ad
     * @return AdSize
     */
    private AdSize convertToAmazonAdSize(final int adWidth, final int adHeight) {
        if (adWidth == 320 && adHeight == 50)
            return AdSize.SIZE_320x50;
        if (adWidth == 300 && adHeight == 250)
            return AdSize.SIZE_300x250;
        if (adWidth == 1024 && adHeight == 50)
            return AdSize.SIZE_1024x50;
        if (adWidth == 600 && adHeight == 90)
            return AdSize.SIZE_600x90;
        if (adWidth == 728 && adHeight == 90)
            return AdSize.SIZE_728x90;
        return AdSize.SIZE_AUTO;
    }

    /**
     * Implements AdListener for receiving callbacks from the Amazon Ads SDK.
     */
    private class AmazonAdBannerListener implements AdListener {

        /**
         * This callback is executed when an Amazon Banner Ad loads successfully.
         */
        @Override
        public void onAdLoaded(Ad ad, AdProperties adProperties) {
            mopubBannerListener.onBannerLoaded(amazonBanner);
        }

        /**
         * This callback is executed when an Amazon Banner Ad fails to load.
         */
        @Override
        public void onAdFailedToLoad(Ad ad, AdError adError) {
            mopubBannerListener.onBannerFailed(convertToMoPubErrorCode(adError));
        }

        /**
         * This callback is executed when an Amazon Banner Ad is expanded.
         */
        @Override
        public void onAdExpanded(Ad ad) {
            mopubBannerListener.onBannerExpanded();
        }

        /**
         * This callback is executed when an Amazon Banner Ad is collapsed.
         */
        @Override
        public void onAdCollapsed(Ad ad) {
            mopubBannerListener.onBannerCollapsed();
        }

        /**
         * This callback is executed when an Amazon Banner Ad is dismissed.
         */
        @Override
        public void onAdDismissed(Ad ad) {
            Log.i(LOG_TAG, "Amazon Banner Ad dismissed.");
        }

        /**
         * Converts the Amazon AdError to a MoPubErrorCode
         * @param adError the Amazon AdError
         * @return MoPubErrorCode mapped to Amazon AdError, defaults to MoPubErrorCode.UNSPECIFIED
         */
        private MoPubErrorCode convertToMoPubErrorCode(final AdError adError) {
            final AdError.ErrorCode errorCode = adError.getCode();
            if (errorCode.equals(AdError.ErrorCode.NO_FILL))
                return MoPubErrorCode.NETWORK_NO_FILL;
            if (errorCode.equals(AdError.ErrorCode.NETWORK_ERROR))
                return MoPubErrorCode.NETWORK_INVALID_STATE;
            if (errorCode.equals(AdError.ErrorCode.NETWORK_TIMEOUT))
                return MoPubErrorCode.NETWORK_TIMEOUT;
            if (errorCode.equals(AdError.ErrorCode.INTERNAL_ERROR))
                return MoPubErrorCode.INTERNAL_ERROR;
            return MoPubErrorCode.UNSPECIFIED;
        }
    }
}
