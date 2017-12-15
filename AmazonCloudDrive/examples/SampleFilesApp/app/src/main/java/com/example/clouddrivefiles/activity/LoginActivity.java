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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;
import com.example.clouddrivefiles.R;
import com.example.clouddrivefiles.utils.Constants;
import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager;
import com.amazon.identity.auth.device.authorization.api.AuthorizationListener;

/**
 * Activity that is responsible for showing the login button and authenticating the user.
 */
public class LoginActivity extends Activity {

    private static final String TAG = LoginActivity.class.getName();

    // Button for initiating login
    private ImageButton mLoginButton;

    // LWA authorization manager used for authenticating the user and getting
    // tokens for Amazon Cloud Drive requests.
    private AmazonAuthorizationManager mAmazonAuthorizationManager;

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            mAmazonAuthorizationManager = new AmazonAuthorizationManager(this, Bundle.EMPTY);
        } catch (IllegalArgumentException e) {
            showCriticalError("API Key Error", "The assets/api_key.txt file is invalid. See https://developer.amazon.com/public/apis/engage/login-with-amazon/docs/register_android.html");
            Log.e(TAG, "Unable to Use Amazon Authorization Manager. APIKey is incorrect or does not exist.", e);
        }
        setContentView(R.layout.activity_login);
        mLoginButton = (ImageButton) findViewById(R.id.login_with_amazon);
        mLoginButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mAmazonAuthorizationManager.authorize(
                        Constants.APP_AUTHORIZATION_SCOPES,
                        Bundle.EMPTY,
                        new LoginListener());
            }
        });
    }

    /**
     * {@link AuthorizationListener} which is passed in to authorize calls made on the {@link AmazonAuthorizationManager} member.
     * Starts getToken workflow if the authorization was successful, or displays a toast if the user cancels authorization.
     * @implements {@link AuthorizationListener}
     */
    private class LoginListener implements AuthorizationListener {

        /**
         * Authorization was completed successfully.
         * Display the profile of the user who just completed authorization
         * @param response bundle containing authorization response. Not used. 
         */
        @Override
        public void onSuccess(Bundle response) {
            SharedPreferences sharedPrefs = getSharedPreferences(
                    Constants.SHARED_PREFERENCE_FILE,
                    Context.MODE_PRIVATE);
            sharedPrefs.edit().putBoolean(Constants.KEY_AUTHENTICATED, true).commit();

            startActivity(new Intent(LoginActivity.this, ContentActivity.class));
            finish();
        }


        /**
         * There was an error during the attempt to authorize the application.
         * Log the error, and reset the profile text view.
         * @param ae the error that occurred during authorize
         */
        @Override
        public void onError(AuthError ae) {
            Log.e(TAG, "AuthError during authorization", ae);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToast("Error during authorization.  Please try again.");
                }
            });
        }

        /**
         * Authorization was cancelled before it could be completed.
         * A toast is shown to the user, to confirm that the operation was cancelled, and the profile text view is reset.
         * @param cause bundle containing the cause of the cancellation. Not used.
         */
        @Override
        public void onCancel(Bundle cause) {
            Log.e(TAG, "User cancelled authorization");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToast("Authorization cancelled");
                }
            });
        }
    }

    /**
     * Show a dialog for critical errors.
     * @param title
     * @param message
     */
    private void showCriticalError(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle(title)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        finish();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Show a toast to the user.
     * @param toastMessage
     */
    private void showToast(String toastMessage){
        Toast authToast = Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_LONG);
        authToast.setGravity(Gravity.CENTER, 0, 0);
        authToast.show();
    }
}