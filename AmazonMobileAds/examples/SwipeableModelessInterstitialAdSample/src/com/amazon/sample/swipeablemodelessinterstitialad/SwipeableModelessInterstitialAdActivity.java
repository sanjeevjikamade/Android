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
package com.amazon.sample.swipeablemodelessinterstitialad;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.util.SparseArray;
import android.widget.FrameLayout;

import com.amazon.device.ads.Ad;
import com.amazon.device.ads.AdError;
import com.amazon.device.ads.AdProperties;
import com.amazon.device.ads.AdRegistration;
import com.amazon.device.ads.DefaultAdListener;
import com.amazon.device.ads.ModelessInterstitialAd;

/**
 * This is a simple app for testing modeless interstitial ad loading capabilities of the Amazon Mobile Ads API.
 */
public class SwipeableModelessInterstitialAdActivity extends FragmentActivity {
    private static final String APP_KEY = "sample-app-v1_pub-2"; // Sample Application Key. Replace this value with your Application Key.
    private static final String LOG_TAG = "SwipeableModelessInterstitialAdSample"; // Tag used to prefix all log messages.
    private static final int AD_FREQUENCY = 5; // the frequency of display ads
    
    private PagerAdapter pagerAdapter;
    private ModelessInterstitialAd modeless;
    private boolean isReadyToShow; // whether the current ad is ready to show
    private SparseArray<ModelessInterstitialAd> modelessAds = new SparseArray<ModelessInterstitialAd>(); // the collection of ads loaded and shown so far

    /**
     * When the activity starts, set up the pager adapter for handling fragments and swipe interactions.
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
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
        
        // assign a fragment pager adapter to the activity
        this.pagerAdapter = new ModelessInterstitialFragmentPagerAdapter(getSupportFragmentManager());
        final ViewPager viewPager = (ViewPager)findViewById(R.id.modeless_interstitials_pager);
        viewPager.setAdapter(this.pagerAdapter);
        viewPager.setOnPageChangeListener(new ModelessInterstitialOnPageChangeListener());
    }
    
    /**
     * This class is for handling displaying the fragments and the swiping interaction
     * between fragments.
     */
    private class ModelessInterstitialFragmentPagerAdapter extends FragmentPagerAdapter {
        private final int[] images; // the collection of images to display in the image gallery
        private int nextImageIndex; // the next image to display from the collection of images
        private static final int PAGE_COUNT = 20;
        private FrameLayout adContainerLayout;
    
        public ModelessInterstitialFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
            this.images = new int[]{R.drawable.image1, R.drawable.image2, R.drawable.image3, R.drawable.image4, R.drawable.image5};
        }
    
        @Override
        public Fragment getItem(int index) {
            if (index % AD_FREQUENCY == 2) {
                this.adContainerLayout = new FrameLayout(SwipeableModelessInterstitialAdActivity.this);
                modeless = new ModelessInterstitialAd(adContainerLayout);
                modeless.setListener(new SampleAdListener());
                modeless.loadAd();
                
                // Note: You can choose to provide additional targeting information to
                // modify how your ads are targeted to your users. This is done via an 
                // AdTargetingOptions parameter that's passed to the loadAd call. 
                // See an example below:
                //
                // final AdTargetingOptions adOptions = new AdTargetingOptions();
                // adOptions.enableGeoLocation(true);
                // if (this.modelessInterstitialAd.loadAd(adOptions)) ...
            }
            if (index != 0 && index % AD_FREQUENCY == 0) {
                if(isReadyToShow) {
                    modelessAds.put(index, modeless);
                    return new ModelessInterstitialFragment().setAdContainerLayout(this.adContainerLayout);
                }
            }
            final ImageGalleryFragment fragment = new ImageGalleryFragment();
            fragment.setImageResource(this.images[this.nextImageIndex++]);
            if(this.nextImageIndex == this.images.length) {
                this.nextImageIndex = 0;
            }
            return fragment;
        }
    
        @Override
        public int getCount() {
            return PAGE_COUNT;
        }
        
        /**
         * This class is for an event listener that tracks ad lifecycle events. It
         * extends DefaultAdListener, so you can override only the methods that you
         * need. In this case, there is no need to override methods specific to
         * expandable ads.
         */
        private class SampleAdListener extends DefaultAdListener {
            /**
             * This event is called once an ad loads successfully.
             */
            @Override
            public void onAdLoaded(final Ad ad, final AdProperties adProperties) {
                Log.i(LOG_TAG, adProperties.getAdType().toString() + " ad loaded successfully.");
                isReadyToShow = true;
            }

            /**
             * This event is called if an ad fails to load.
             */
            @Override
            public void onAdFailedToLoad(final Ad view, final AdError error) {
                Log.w(LOG_TAG, "Ad failed to load. Code: " + error.getCode() + ", Message: " + error.getMessage());
                isReadyToShow = false;
            }
        }
    } 
    
    /**
     * Used for capturing page-change events
     */
    private class ModelessInterstitialOnPageChangeListener implements OnPageChangeListener {
        private int prevPosition = -1; // index of previous on screen fragment
        private int currPosition = -1; // index of current on screen fragment

        @Override
        public void onPageScrollStateChanged(final int state) {
            switch (state) {
                case ViewPager.SCROLL_STATE_IDLE:
                    ModelessInterstitialAd modelessAd = modelessAds.get(this.currPosition);
                    if (modelessAd != null) {
                        modelessAd.adShown();
                    }
                    modelessAd = modelessAds.get(this.prevPosition);
                    if (modelessAd != null) {
                        modelessAd.adHidden();
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels)
        { }

        @Override
        public void onPageSelected(final int position) {
            this.prevPosition = this.currPosition;
            this.currPosition = position;
        }
    }
}
