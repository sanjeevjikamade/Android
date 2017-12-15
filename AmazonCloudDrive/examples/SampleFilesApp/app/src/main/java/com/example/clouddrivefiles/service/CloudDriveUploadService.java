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
package com.example.clouddrivefiles.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.amazon.clouddrive.AmazonCloudDriveClient;
import com.amazon.clouddrive.exceptions.CloudDriveException;
import com.amazon.clouddrive.exceptions.ConflictError;
import com.amazon.clouddrive.handlers.ProgressListener;
import com.amazon.clouddrive.model.ListNodesRequest;
import com.amazon.clouddrive.model.ListNodesResponse;
import com.amazon.clouddrive.model.Node;
import com.amazon.clouddrive.model.NodeKind;
import com.amazon.clouddrive.model.Suppress;
import com.amazon.clouddrive.model.UploadFileRequest;
import com.example.clouddrivefiles.R;
import com.example.clouddrivefiles.global.UserState;
import com.example.clouddrivefiles.provider.CloudDriveContract;
import com.example.clouddrivefiles.utils.Closer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple service that uploads items in the upload queue to the root of the drive.
 *
 * To add an item to the queue, insert the {@link com.example.clouddrivefiles.provider.CloudDriveContract.UploadQueueItems}
 * into the {@link com.example.clouddrivefiles.provider.CloudDriveProvider} instance.
 *
 * To trigger an upload of an item that has been queued, send an intent to the service.
 *
 * This class is an example of:
 * <ul>
 *     <li>uploading a file to the cloud</li>
 *     <li>using {@link ProgressListener} and {@link com.amazon.clouddrive.handlers.AsyncHandler} callbacks</li>
 *     <li>protecting customer's content by using a ContentProvider to vend the content</li>
 * </ul>
 */
public class CloudDriveUploadService extends IntentService {

    private static final String TAG = CloudDriveUploadService.class.getSimpleName();

    private AmazonCloudDriveClient mAmazonCloudDriveClient;
    private String mRootNodeId;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;

    public CloudDriveUploadService() {
        super(CloudDriveUploadService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        // Get the global client instance
        mAmazonCloudDriveClient = UserState.getAmazonCloudDriveClientInstance(this);

        // Notification manager and builder for creating the upload notifications
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationBuilder = new NotificationCompat.Builder(this);

        Cursor queueCursor = null;
        try {

            // Query for all upload queue items ordered by _ID (this orders the
            // entries by when they were inserted into the database).
            queueCursor = getContentResolver().query(
                    CloudDriveContract.UploadQueueItems.CONTENT_URI,
                    null,
                    null,
                    null,
                    CloudDriveContract.UploadQueueItems._ID + " ASC");

            // For each queue item, upload it.
            for (queueCursor.moveToFirst(); !queueCursor.isAfterLast(); queueCursor.moveToNext()) {
                long id = queueCursor.getLong(queueCursor.getColumnIndex(CloudDriveContract.UploadQueueItems._ID));
                String sourceUri = queueCursor.getString(queueCursor.getColumnIndex(CloudDriveContract.UploadQueueItems.SOURCE_URI));
                try {

                    // Do the upload
                    uploadItem(Uri.parse(sourceUri));

                    // Upload was successful, remove entry from upload queue
                    deleteItemFromUploadQueue(id);

                } catch (ConflictError e) {
                    // Already uploaded. Delete the queue entry.
                    deleteItemFromUploadQueue(id);
                } catch (CloudDriveException e) {
                    Log.e(TAG, "Could not upload " + sourceUri, e);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Could not upload " + sourceUri, e);
                } catch (IOException e) {
                    Log.e(TAG, "Could not upload " + sourceUri, e);
                }
            }
        } catch (InterruptedException e) {
            // Execution interrupted.
        } finally {
            Closer.closeQuietly(queueCursor);
        }

        // Notify that all of the work is done.
        mNotificationBuilder.setContentText(getString(R.string.upload_notification_complete))
                            .setProgress(0, 0, false);

        mNotificationManager.notify(R.id.upload_notification, mNotificationBuilder.build());
    }

    /**
     * Removes an item from the upload queue by the item's id.
     * @param id ID of the item to remove
     */
    private void deleteItemFromUploadQueue(long id) {
        getContentResolver().delete(
                CloudDriveContract.UploadQueueItems.CONTENT_URI,
                CloudDriveContract.UploadQueueItems._ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    /**
     * Upload the content with sourceUri to Amazon Cloud Drive
     * @param sourceUri the content URI to upload
     */
    private void uploadItem(Uri sourceUri) throws IOException, InterruptedException, CloudDriveException {
        File stagedUploadFile = null;
        try {
            String displayName = getDisplayName(sourceUri);

            // Start notification as indeterminate, switch to determinate progress
            // when upload actually starts.
            mNotificationBuilder.setContentTitle(String.format(getString(R.string.upload_notification_title), displayName))
                                .setContentText(getString(R.string.upload_notification_upload_in_progress))
                                .setSmallIcon(R.drawable.ic_backup_white)
                                .setProgress(0, 0, true);
            mNotificationManager.notify(R.id.upload_notification, mNotificationBuilder.build());

            // Create a staged file that we will upload from.
            stagedUploadFile = copyContentStreamToStagingFile(sourceUri, displayName);

            // Upload the file with the root as its parent.
            List<String> parents = new ArrayList<String>();
            parents.add(getRootNodeId());
            UploadFileRequest uploadFileRequest = new UploadFileRequest(
                    stagedUploadFile.getName(),
                    new FileInputStream(stagedUploadFile),
                    stagedUploadFile.length());
            uploadFileRequest.setParents(parents);
            uploadFileRequest.setSuppress(Suppress.Deduplication);
            mAmazonCloudDriveClient.uploadFile(uploadFileRequest, new ProgressListener() {
                @Override
                public void onProgress(long progress, long maxProgress) {
                    // Progress is reported on the background thread. Notifications can be updated
                    // from the background thread so there is no need to post a message to the
                    // main thread.

                    // Rebase progress to be in Integer.MAX_VALUE scale so that we can pass it to
                    // Android's progress bar.
                    int rebasedProgress = (int)(Integer.MAX_VALUE * (progress / (double) maxProgress));
                    mNotificationBuilder.setProgress(rebasedProgress, Integer.MAX_VALUE, false);

                    // Report progress to notification
                    mNotificationManager.notify(R.id.upload_notification, mNotificationBuilder.build());
                }
            });

        } finally {
            // Clean up staged file.
            if (stagedUploadFile != null) {
                stagedUploadFile.delete();
            }
        }
    }

    /**
     * Get the display name from the URI's provider.
     * @param uri the URI that will be resolved and opened
     * @return the display name if found, uri.getLastPathSegment() otherwise.
     */
    private String getDisplayName(Uri uri) {
        Cursor contentCursor = null;
        try {
            // Open content cursor to get the display_name.
            contentCursor = getContentResolver().query(
                    uri,
                    new String[]{OpenableColumns.DISPLAY_NAME},
                    null,
                    null,
                    null);
            if (contentCursor == null) {
                return uri.getLastPathSegment();
            }
            contentCursor.moveToFirst();
            if (contentCursor.isAfterLast()) {
                return uri.getLastPathSegment();
            }

            int displayNameIndex;
            if ((displayNameIndex = contentCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)) != -1) {
                return contentCursor.getString(displayNameIndex);
            } else {
                return uri.getLastPathSegment();
            }
        } finally {
            Closer.closeQuietly(contentCursor);
        }
    }

    /**
     * Copy stream from ContentProvider to a staged file.
     * @param uri the URI that will be resolved and opened
     * @return the staged file that was created.
     * @throws IOException
     * @throws InterruptedException
     */
    private File copyContentStreamToStagingFile(Uri uri, String displayName) throws IOException, InterruptedException {
        // Copy stream from content provider into a staged file.
        InputStream inputStream = null;
        FileOutputStream fos = null;
        try {
            inputStream = getContentResolver().openInputStream(uri);
            File stagingDirectory = new File(getCacheDir() + "/staged/");
            stagingDirectory.mkdirs();
            File stagedUploadFile = new File(stagingDirectory, displayName);
            fos = new FileOutputStream(stagedUploadFile);
            copyInputStreamToOutputStream(inputStream, fos);
            return stagedUploadFile;
        } finally {
            Closer.closeQuietly(inputStream);
            Closer.closeQuietly(fos);
        }
    }

    /**
     * Returns the root node for the drive.
     * @return the root node for the drive.
     * @throws InterruptedException
     * @throws CloudDriveException
     */
    private String getRootNodeId() throws InterruptedException, CloudDriveException {

        if (mRootNodeId != null) {
            return mRootNodeId;
        }

        // To list the root node, we filter to only show the nodes that
        // have the property isRoot set to true. To learn more about
        // filtering, see https://developer.amazon.com/public/apis/experience/cloud-drive/content/nodes#Filtering
        ListNodesRequest listNodesRequest = new ListNodesRequest();
        String filters = "isRoot:true";
        listNodesRequest.setFilters(filters);

        // Make a synchronous (blocking) call to Amazon Cloud Drive that lists
        // the root node.
        ListNodesResponse listNodesResponse = mAmazonCloudDriveClient.listNodes(listNodesRequest);
        List<Node> nodes = listNodesResponse.getData();

        if (nodes.isEmpty()) {
            return null;
        }

        // There is only one root node, so we will just get the first item.
        Node rootNode = nodes.get(0);

        mRootNodeId = rootNode.getId();
        return mRootNodeId;
    }

    /**
     * Copies bytes from InputStream to OutputStream
     * @param in InputStream to copy from
     * @param out OutputStream to copy to
     * @throws IOException
     * @throws InterruptedException
     */
    private static void copyInputStreamToOutputStream(InputStream in, OutputStream out) throws IOException, InterruptedException {
        byte[] buf = new byte[4096];
        int len;
        while((len = in.read(buf)) > 0){
            // When reading the stream, it is important to check to see if the thread has been interrupted
            // so it can stop downloading the file.
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            out.write(buf, 0, len);
        }
    }
}
