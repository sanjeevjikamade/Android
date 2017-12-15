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
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import com.example.clouddrivefiles.R;
import com.example.clouddrivefiles.provider.CloudDriveContract;
import com.example.clouddrivefiles.service.CloudDriveUploadService;

/**
 * Activity that forwards to the UploadService
 */
public class UploadActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (!Intent.ACTION_SEND.equals(action) || type == null) {
            finish();
            return;
        }

        if ("text/plain".equals(type)) {
            // Do nothing.
            finish();
            return;
        }

        Toast.makeText(this, getString(R.string.upload_toast_uploading_to_clouddrive), Toast.LENGTH_SHORT).show();

        // Enqueue new upload
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        ContentValues contentValues = new ContentValues();
        contentValues.put(CloudDriveContract.UploadQueueItems.SOURCE_URI, uri.toString());
        getContentResolver().insert(CloudDriveContract.UploadQueueItems.CONTENT_URI, contentValues);

        // Start UploadService to work through queue.
        startService(new Intent(this, CloudDriveUploadService.class));

        finish();
    }
}
