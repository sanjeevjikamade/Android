/*
 * CoffeeShop.java
 *
 * Copyright (c) 2010-2014 Amazon.com, Inc. or its affiliates. All rights reserved.
 *
 * Use is subject to license terms.
 */

package com.example.amazon.capitolhillcoffee;

import com.amazon.geo.mapsv2.model.LatLng;

/**
 * Represents a coffee shop.
 */
public class CoffeeShop {
    private final String title;
    private final String address;
    private final String phone;
    private final LatLng location;

    public CoffeeShop(final String title, final String address,
            final String phone, final LatLng location) {
        this.title = title;
        this.address = address;
        this.phone = phone;
        this.location = location;
    }

    public String getTitle() {
        return title;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public LatLng getLocation() {
        return location;
    }
}
