/*
 * CoffeeFetcher.java
 *
 * Copyright (c) 2014 Amazon.com, Inc. or its affiliates. All rights reserved.
 *
 * Use is subject to license terms.
 */

package com.example.amazon.capitolhillcoffee;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.amazon.geo.mapsv2.AmazonMap;
import com.amazon.geo.mapsv2.AmazonMap.OnInfoWindowClickListener;
import com.amazon.geo.mapsv2.model.BitmapDescriptor;
import com.amazon.geo.mapsv2.model.BitmapDescriptorFactory;
import com.amazon.geo.mapsv2.model.LatLng;
import com.amazon.geo.mapsv2.model.LatLngBounds;
import com.amazon.geo.mapsv2.model.Marker;
import com.amazon.geo.mapsv2.model.MarkerOptions;

/**
 * 
 * Fetches coffee shops details.
 * 
 */
public class CoffeeFetcher {
    final Map<Marker, CoffeeShop> markers = new HashMap<Marker, CoffeeShop>();

    private static final CoffeeShop[] COFFEE_SHOPS = {
            new CoffeeShop("Joe Bar Cafe", "810 East Roy Street",
                    "(206) 324-0407", new LatLng(47.625148, -122.321623)),
            new CoffeeShop("Roy Street Coffee and Tea",
                    "700 Broadway Ave East", "(206) 325-2211", new LatLng(
                            47.625206, -122.321108)),
            new CoffeeShop("Vivace Espresso Bar", "532 Broadway Ave East",
                    "(206) 860-2722", new LatLng(47.623749, -122.320861)),
            new CoffeeShop("Dilettante Mocha Cafe", "538 Broadway Ave East",
                    "(206) 329-6463", new LatLng(47.623922, -122.320861)),
            new CoffeeShop("Starbucks", "700 Broadway Ave East",
                    "(206) 325-2211", new LatLng(47.625495, -122.321287)),
            new CoffeeShop("Starbucks", "434 Broadway Ave East",
                    "(206) 323-7888", new LatLng(47.623058, -122.320792)),
            new CoffeeShop("Cafe Canape", "700 Broadway Ave East",
                    "(206) 708-1210", new LatLng(47.625401, -122.320791)), };

    public void removeShops() {
        // Remove the old markers, if any.
        for (Marker marker : markers.keySet()) {
            marker.remove();
        }
        // Clear our list of markers
        markers.clear();
    }

    public LatLngBounds addShops(final Context context, AmazonMap map) {
        // Clear existing markers.
        removeShops();
        map.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {
            CoffeeDetails details = new CoffeeDetails();

            @Override
            public void onInfoWindowClick(Marker marker) {
                if (markers.containsKey(marker)) {
                    details.display(context, markers.get(marker));
                }
            }
        });

        // Add markers
        final BitmapDescriptor coffeeMarkerIcon = BitmapDescriptorFactory
                .fromResource(R.drawable.coffeeshop);
        MarkerOptions coffeeMarkerOptions = new MarkerOptions()
                .icon(coffeeMarkerIcon);
        LatLngBounds.Builder shopsBounds = LatLngBounds.builder();
        for (CoffeeShop shop : COFFEE_SHOPS) {
            coffeeMarkerOptions.position(shop.getLocation())
                    .title(shop.getTitle()).snippet(shop.getAddress());
            Marker newMarker = map.addMarker(coffeeMarkerOptions);
            // Associate the marker with the shop.
            markers.put(newMarker, shop);

            shopsBounds.include(shop.getLocation());
        }

        return shopsBounds.build();
    }
}
