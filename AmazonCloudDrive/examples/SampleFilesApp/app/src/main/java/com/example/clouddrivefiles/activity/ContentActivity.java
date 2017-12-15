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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager;
import com.amazon.identity.auth.device.shared.APIListener;
import com.example.clouddrivefiles.global.UserState;
import com.example.clouddrivefiles.R;
import com.example.clouddrivefiles.fragment.NodeListingFragment;
import com.example.clouddrivefiles.provider.CloudDriveContract;
import com.example.clouddrivefiles.utils.Constants;

/**
 * Activity that holds the NodeListingFragments.
 */
public class ContentActivity extends FragmentActivity {

    private FragmentManager mFragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_content);
        mFragmentManager = getSupportFragmentManager();
        if (savedInstanceState == null) {
            // We have not gone through onCreate before, so we should add
            // the initial fragment. It is added without pushing on the
            // back stack because it is the first thing there.
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.fragment_container, new NodeListingFragment());
            fragmentTransaction.commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_content, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_logout:
                logout();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void openFragment(Fragment fragment, String transactionName) {

        // Navigates to the target fragment by adding it to the back stack.
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.addToBackStack(transactionName);
        fragmentTransaction.commit();
    }

    public void popBackStack() {
        mFragmentManager.popBackStack();
    }

    /**
     * Logs customer out of LWA and clears data from database.
     */
    private void logout() {

        AmazonAuthorizationManager amazonAuthorizationManager = UserState.getAmazonAuthorizationManagerInstance(this);
        amazonAuthorizationManager.clearAuthorizationState(new APIListener() {
            @Override
            public void onSuccess(Bundle bundle) {

                ///////////////////////////////////////////////////////////////
                // Log out was successful. Clear out the database, and send
                // the user back to the Launcher so that they log in again.

                SharedPreferences sharedPrefs = getSharedPreferences(
                        Constants.SHARED_PREFERENCE_FILE,
                        Context.MODE_PRIVATE);
                sharedPrefs.edit().putBoolean(Constants.KEY_AUTHENTICATED, false).commit();

                getContentResolver().delete(CloudDriveContract.Nodes.CONTENT_URI, null, null);
                getContentResolver().delete(CloudDriveContract.NodeParents.CONTENT_URI, null, null);

                UserState.reset();
                startActivity(new Intent(ContentActivity.this, LauncherActivity.class));
                finish();
            }

            @Override
            public void onError(AuthError authError) {
            }
        });
    }
}
