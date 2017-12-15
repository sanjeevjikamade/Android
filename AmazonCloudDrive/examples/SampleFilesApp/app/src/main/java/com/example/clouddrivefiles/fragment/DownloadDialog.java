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
package com.example.clouddrivefiles.fragment;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.amazon.clouddrive.AmazonCloudDriveClient;
import com.amazon.clouddrive.exceptions.CloudDriveException;
import com.amazon.clouddrive.handlers.ProgressListener;
import com.amazon.clouddrive.model.DownloadFileRequest;
import com.example.clouddrivefiles.R;
import com.example.clouddrivefiles.global.UserState;
import com.example.clouddrivefiles.provider.CloudDriveContract;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Dialog that downloads a file from Amazon Cloud Drive and attempts to
 * open it with an ACTION_VIEW intent.
 *
 * This class is an example of:
 * <ul>
 *     <li>downloading a file from the cloud</li>
 *     <li>using {@link ProgressListener} and {@link com.amazon.clouddrive.handlers.AsyncHandler} callbacks</li>
 *     <li>protecting customer's content by using a ContentProvider to vend the content</li>
 * </ul>
 */
public class DownloadDialog extends DialogFragment {

    private static final String TAG = DownloadDialog.class.getSimpleName();

    public final static String ARG_ID = "id";
    public final static String ARG_NODE_ID = "node_id";
    public final static String ARG_FILE_NAME = "file_name";

    // AsyncTask that is used to download the file, add metadata
    // to the ContentProvider, and send off an Intent to open
    // the file.
    private AsyncTask<Void, Integer, Uri> mDownloadFileTask;

    // Global client instance
    private AmazonCloudDriveClient mAmazonCloudDriveClient;

    // ProgressBar that shows the download progress in the Fragment
    private ProgressBar mProgressBar;

    /**
     * Create a new instance of the DownloadDialog
     * @param id The _ID of the node row
     * @param nodeId the node ID from the service
     * @param fileName the file name that we want to save it as
     * @return
     */
    public static DownloadDialog newInstance(int id, String nodeId, String fileName) {
        DownloadDialog fragment = new DownloadDialog();
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_ID, id);
        arguments.putString(ARG_NODE_ID, nodeId);
        arguments.putString(ARG_FILE_NAME, fileName);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        // Create initial dialog view with indeterminate progress.
        // We will turn this into actual progress once the file starts
        // downloading.
        View view = inflater.inflate(R.layout.fragment_download_dialog, container, false);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        mProgressBar.setIndeterminate(true);

        getDialog().setTitle(R.string.opening);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setRetainInstance(true);

        super.onCreate(savedInstanceState);

        mAmazonCloudDriveClient = UserState.getAmazonCloudDriveClientInstance(getActivity());

        final Integer id = getArguments().getInt(ARG_ID);
        final String nodeId = getArguments().getString(ARG_NODE_ID);
        final String fileName = getArguments().getString(ARG_FILE_NAME);

        mDownloadFileTask = new AsyncTask<Void, Integer, Uri>() {

            @Override
            protected Uri doInBackground(Void... voids) {

                try {
                    // Create file
                    File directory = new File(getActivity().getFilesDir(), "/nodes/" + id + "/content/");
                    directory.mkdirs();
                    File file = new File(directory, fileName);
                    OutputStream outputStream = new FileOutputStream(file);

                    // Setup a ProgressListener that will report progress to the AsyncTask
                    ProgressListener progressListener = new ProgressListener() {
                        @Override
                        public void onProgress(final long progress, final long maxProgress) {
                            //
                            // This callback happens on the background thread. We will
                            // post to the main thread by using publishProgress() so
                            // the ProgressBar can be updated.
                            //
                            // The progress from the client is a long, but Android progress
                            // bars require an int. This is usually not an issue, but can
                            // be a problem for very large files. The progress is rebased
                            // to Integer.MAX_VALUE to avoid this issue.
                            //
                            publishProgress(
                                    (int)(Integer.MAX_VALUE * (progress / (double) maxProgress)),
                                    Integer.MAX_VALUE);
                        }
                    };

                    // Download the File
                    DownloadFileRequest downloadFileRequest = new DownloadFileRequest(nodeId, outputStream);
                    mAmazonCloudDriveClient.downloadFile(downloadFileRequest, progressListener);

                    // Write the file metadata to the provider so other apps can read it.
                    Activity activity = getActivity();
                    if (activity != null) {
                        Uri uri = CloudDriveContract.NodeContents.getContentUri(id);

                        // Write the node contents row
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(CloudDriveContract.NodeContents._ID, id);
                        contentValues.put(CloudDriveContract.NodeContents.DATA, file.toString());
                        contentValues.put(CloudDriveContract.NodeContents.DISPLAY_NAME, fileName);
                        contentValues.put(CloudDriveContract.NodeContents.SIZE, file.length());
                        activity.getContentResolver().insert(uri, contentValues);

                        return uri;
                    }

                } catch (InterruptedException e) {
                    // There is nothing wrong with being interrupted.
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Could not download file");
                } catch (IOException e) {
                    Log.e(TAG, "Could not download file");
                } catch (CloudDriveException e) {
                    Log.e(TAG, "Could not download file");
                }

                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                // Update the progress bar with the current download progress.
                mProgressBar.setIndeterminate(false);
                mProgressBar.setProgress(values[0]);
                mProgressBar.setMax(values[1]);
            }

            @Override
            protected void onPostExecute(Uri uri) {
                if (uri != null) {

                    // Open the URI with another application
                    Activity activity = getActivity();
                    if (activity != null) {
                        // Fire off an intent to view the node content URI
                        Intent intent = new Intent();
                        intent.setAction(android.content.Intent.ACTION_VIEW);
                        intent.setDataAndType(uri, activity.getContentResolver().getType(uri));
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        try {
                            activity.startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(activity, R.string.no_application_found, Toast.LENGTH_LONG).show();
                        }
                    }
                }

                // Close the dialog
                dismiss();
            }

            @Override
            protected void onCancelled(Uri uri) {

                // Close the dialog
                dismiss();
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // There is no reason to keep downloading if the dialog has been destroyed.
        // Cancel the task.
        if (mDownloadFileTask != null) {
            mDownloadFileTask.cancel(true);
        }
    }

    @Override
    public void onDestroyView() {
        // Prevent destruction of the Activity from destroying this fragment
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }

        super.onDestroyView();
    }
}