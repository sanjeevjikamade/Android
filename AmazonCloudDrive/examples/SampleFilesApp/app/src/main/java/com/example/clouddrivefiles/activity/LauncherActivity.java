/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.example.clouddrivefiles.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.example.clouddrivefiles.utils.Constants;

/**
 * Activity that dispatches to either the ContentActivity or the LoginActivity depending
 * on whether the user is already logged in.
 */
public class LauncherActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the shared preference that determines whether the customer is logged in.
        SharedPreferences sharedPrefs = getSharedPreferences(
                Constants.SHARED_PREFERENCE_FILE,
                Context.MODE_PRIVATE);
        if (sharedPrefs.getBoolean(Constants.KEY_AUTHENTICATED, false)) {
            // Already logged in. Go directly to ContentActivity
            startActivity(new Intent(LauncherActivity.this, ContentActivity.class));
            finish();
        } else {
            // Not logged in. Go to the LoginActivity.
            startActivity(new Intent(LauncherActivity.this, LoginActivity.class));
            finish();
        }
    }
}
