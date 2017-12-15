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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * This is a fragment for display a ModelessInterstitialAd
 */
public class ModelessInterstitialFragment extends Fragment {
    private FrameLayout adContainerLayout; // the layout for containing the ad

    public ModelessInterstitialFragment setAdContainerLayout(FrameLayout adContainerLayout) {
        this.adContainerLayout = adContainerLayout;
        return this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.modeless_fragment, container, false);
        final LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.modeless_layout);
        // ensure the parent of the ad layout is the layout for this fragment
        if (this.adContainerLayout.getParent() != null && !this.adContainerLayout.getParent().equals(layout)) {
            ((ViewGroup) this.adContainerLayout.getParent()).removeView(this.adContainerLayout);
        }
        layout.addView(this.adContainerLayout, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        return rootView;
    }
}