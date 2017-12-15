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
import android.widget.ImageView;

/**
 * This is a fragment for displaying an image from the gallery 
 */
public class ImageGalleryFragment extends Fragment {  
    private int imageResource; // the image to display in this fragment

    /**
     * Constructs an ImageGalleryFragment with a reference to a specific image
     * @param imageResource - the image identifier to display
     */
    public void setImageResource(final int imageResource) {
        this.imageResource = imageResource;  
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        setRetainInstance(true);
        final ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.image_fragment, container, false);
        final ImageView imageView = (ImageView)rootView.findViewById(R.id.image_view);
        imageView.setImageResource(this.imageResource);
        return rootView;
    }
}