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
package com.example.clouddrivefiles.global;

import android.content.Context;
import android.os.Bundle;
import com.amazon.clouddrive.AmazonCloudDriveClient;
import com.amazon.clouddrive.auth.AmazonAuthorizationConnectionFactory;
import com.amazon.clouddrive.configuration.AccountConfiguration;
import com.amazon.clouddrive.configuration.ClientConfiguration;
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager;
import com.example.clouddrivefiles.utils.Constants;

/**
 * Holds global user state. State should be cleared on log out.
 */
public class UserState {

    // The LWA authorization manager
    private static AmazonAuthorizationManager sAmazonAuthorizationManager;

    // The Amazon Cloud Drive client. We prefer to use the same instance for
    // all places in the application because the client caches the endpoints.
    // Creating a new instance causes the cache to be cleared.
    private static AmazonCloudDriveClient sAmazonCloudDriveClient;

    /**
     * Get the global instance of the AmazonAuthorizationManager.
     * @param context an application Context
     * @return the auth manager
     */
    public static synchronized AmazonAuthorizationManager getAmazonAuthorizationManagerInstance(Context context) {
        if (sAmazonAuthorizationManager == null) {
            sAmazonAuthorizationManager = new AmazonAuthorizationManager(context.getApplicationContext(), Bundle.EMPTY);
        }

        return sAmazonAuthorizationManager;
    }

    /**
     * Gets the global instance of the AmazonCloudDriveClient
     * @param context an application Context
     * @return the client
     */
    public static synchronized AmazonCloudDriveClient getAmazonCloudDriveClientInstance(Context context) {
        if (sAmazonCloudDriveClient == null) {
            sAmazonCloudDriveClient = new AmazonCloudDriveClient(
                new AccountConfiguration(
                        new AmazonAuthorizationConnectionFactory(
                                getAmazonAuthorizationManagerInstance(context),
                                Constants.APP_AUTHORIZATION_SCOPES)),
                new ClientConfiguration(Constants.USER_AGENT));
        }

        return sAmazonCloudDriveClient;
    }

    /**
     * Clears the global user state.
     */
    public static synchronized void reset() {
        sAmazonAuthorizationManager = null;
        sAmazonCloudDriveClient = null;
    }
}
