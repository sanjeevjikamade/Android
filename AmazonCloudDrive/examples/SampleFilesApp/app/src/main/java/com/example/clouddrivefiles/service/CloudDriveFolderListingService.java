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
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.util.Log;
import com.amazon.clouddrive.AmazonCloudDriveClient;
import com.amazon.clouddrive.exceptions.CloudDriveException;
import com.amazon.clouddrive.model.ListChildrenRequest;
import com.amazon.clouddrive.model.ListChildrenResponse;
import com.amazon.clouddrive.model.ListNodesRequest;
import com.amazon.clouddrive.model.ListNodesResponse;
import com.amazon.clouddrive.model.Node;
import com.example.clouddrivefiles.global.UserState;
import com.example.clouddrivefiles.provider.CloudDriveContract;
import com.example.clouddrivefiles.utils.Closer;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple service that lists nodes and saves them in {@link com.example.clouddrivefiles.provider.CloudDriveProvider}.
 */
public class CloudDriveFolderListingService extends IntentService {

    private static String TAG = CloudDriveFolderListingService.class.getSimpleName();

    public static String ACTION_LIST_FOLDER = "list_folder";
    public static String ACTION_LIST_ROOT_FOLDER = "list_root_folder";
    public static String EXTRA_NODE_ID = "node_id";

    public CloudDriveFolderListingService() {
        super(CloudDriveFolderListingService.class.getSimpleName());
    }

    /**
     * Creates an Intent that will list the children of a specific node
     * and save it in {@link com.example.clouddrivefiles.provider.CloudDriveProvider}
     * @param context a Context
     * @param nodeId The node ID to list.
     * @return the new Intent
     */
    public static Intent newListFolderIntent(Context context, String nodeId) {
        Intent intent = new Intent(context, CloudDriveFolderListingService.class);
        intent.setAction(ACTION_LIST_FOLDER);
        intent.putExtra(EXTRA_NODE_ID, nodeId);
        return intent;
    }

    /**
     * Creates an Intent that will list the children of the root node
     * and save it in {@link com.example.clouddrivefiles.provider.CloudDriveProvider}
     * @param context a Context
     * @return the new Intent
     */
    public static Intent newListRootFolderIntent(Context context) {
        Intent intent = new Intent(context, CloudDriveFolderListingService.class);
        intent.setAction(ACTION_LIST_ROOT_FOLDER);
        return intent;
    }

    private AmazonCloudDriveClient mAmazonCloudDriveClient;

    @Override
    protected void onHandleIntent(Intent intent) {
        mAmazonCloudDriveClient = UserState.getAmazonCloudDriveClientInstance(this);

        String action = intent.getAction();
        if (ACTION_LIST_FOLDER.equals(action)) {
            String nodeId = intent.getStringExtra(EXTRA_NODE_ID);
            listFolder(nodeId);
        } else if (ACTION_LIST_ROOT_FOLDER.equals(action)) {
            listRootFolder();
        }
    }

    /**
     * List the root folder and save both the root node and
     * root's child nodes to the ContentProvider
     */
    private void listRootFolder() {
        try {
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
                return;
            }

            // There is only one root node, so we will just get the first item.
            Node rootNode = nodes.get(0);

            // Save the root node information through the ContentProvider
            ArrayList<ContentProviderOperation> contentProviderOperations = new ArrayList<ContentProviderOperation>();
            contentProviderOperations.add(createInsertNodeContentProviderOperation(rootNode));
            if (!contentProviderOperations.isEmpty()) {
                getContentResolver().applyBatch(
                        CloudDriveContract.AUTHORITY,
                        contentProviderOperations);
            }

            // List and save the children of the root.
            listFolder(rootNode.getId());

        } catch (InterruptedException e) {
            Log.d(TAG, "Interrupted while getting root node.");
        } catch (CloudDriveException e) {
            Log.e(TAG, "Caught exception while getting root node.", e);
        } catch (RemoteException e) {
            Log.e(TAG, "Caught exception while getting root node.", e);
        } catch (OperationApplicationException e) {
            Log.e(TAG, "Caught exception while getting root node.", e);
        }
    }

    /**
     * List a folder and save the child nodes to the ContentProvider
     * @param id the node ID to list
     */
    private void listFolder(String id) {
        try {

            // Mark all existing rows for this folder as 'dirty' so we will
            // know which ones need to be deleted at the end.

            ArrayList<ContentProviderOperation> updateAllChildrenToDirtyOperations =
                    createUpdateAllChildrenToDirtyOperations(id);
            if (!updateAllChildrenToDirtyOperations.isEmpty()) {
                getContentResolver().applyBatch(
                        CloudDriveContract.AUTHORITY,
                        updateAllChildrenToDirtyOperations);
            }

            // ListChildren is an example of a paged request. We may not get all
            // of the nodes back in one request, so we will need to keep looping
            // until we get all of the a null next token as a response.
            String nextToken = null;
            do {
                // Make a synchronous (blocking) call to Amazon Cloud Drive that lists
                // all of the children for the node.
                ListChildrenRequest listChildrenRequest = new ListChildrenRequest(id);
                listChildrenRequest.setStartToken(nextToken);
                ListChildrenResponse response = mAmazonCloudDriveClient.listChildren(listChildrenRequest);
                nextToken = response.getNextToken();
                List<Node> nodes = response.getData();

                // Save all of the node children through the ContentProvider
                ArrayList<ContentProviderOperation> contentProviderOperations = new ArrayList<ContentProviderOperation>();
                for (Node node : nodes) {
                    contentProviderOperations.add(createInsertNodeContentProviderOperation(node));
                    contentProviderOperations.addAll(createReplaceNodeParentContentProviderOperations(node));
                }
                if (!contentProviderOperations.isEmpty()) {
                    getContentResolver().applyBatch(
                            CloudDriveContract.AUTHORITY,
                            contentProviderOperations);
                }
            }
            while (nextToken != null);

            // Remove all rows that are still considered 'dirty' these are ones
            // that no longer exist.

            getContentResolver().delete(
                    CloudDriveContract.Nodes.CONTENT_URI,
                    CloudDriveContract.Nodes.IS_DIRTY + " = ?",
                    new String[]{Integer.toString(1)});


        } catch (InterruptedException e) {
            Log.d(TAG, "Interrupted while listing node contents.");
        } catch (CloudDriveException e) {
            Log.e(TAG, "Caught exception listing node contents.", e);
        } catch (RemoteException e) {
            Log.e(TAG, "Caught exception listing node contents.", e);
        } catch (OperationApplicationException e) {
            Log.e(TAG, "Caught exception listing node contents.", e);
        }
    }

    private ContentProviderOperation createInsertNodeContentProviderOperation(final Node node) {

        // Save some of the fields on the node. The node contains many more fields that may
        // be of use to us. For this application, we are choosing a few.
        // The dirty flag is updated in this operation to let us know that this row has been updated.
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(CloudDriveContract.Nodes.CONTENT_URI)
                .withValue(CloudDriveContract.Nodes.NODE_ID, node.getId())
                .withValue(CloudDriveContract.Nodes.CREATED_BY, node.getCreatedBy())
                .withValue(CloudDriveContract.Nodes.CREATED_DATE, node.getCreatedDate())
                .withValue(CloudDriveContract.Nodes.DESCRIPTION, node.getDescription())
                .withValue(CloudDriveContract.Nodes.EXCLUSIVELY_TRASHED, node.isExclusivelyTrashed())
                .withValue(CloudDriveContract.Nodes.IS_ROOT, node.isRoot())
                .withValue(CloudDriveContract.Nodes.IS_SHARED, node.isShared())
                .withValue(CloudDriveContract.Nodes.KIND, node.getKind())
                .withValue(CloudDriveContract.Nodes.MODIFIED_DATE, node.getModifiedDate())
                .withValue(CloudDriveContract.Nodes.NAME, node.getName())
                .withValue(CloudDriveContract.Nodes.RECURSIVELY_TRASHED, node.isRecursivelyTrashed())
                .withValue(CloudDriveContract.Nodes.STATUS, node.getStatus())
                .withValue(CloudDriveContract.Nodes.VERSION, node.getVersion())
                .withValue(CloudDriveContract.Nodes.IS_DIRTY, Integer.toString(0));

        return builder.build();
    }

    private List<ContentProviderOperation> createReplaceNodeParentContentProviderOperations(final Node node) {

        // Delete all existing node_parent rows for the node
        List<ContentProviderOperation> replaceNodeParentOperations = new ArrayList<ContentProviderOperation>();
        replaceNodeParentOperations.add(
                ContentProviderOperation.newDelete(CloudDriveContract.NodeParents.CONTENT_URI)
                        .withSelection(CloudDriveContract.NodeParents.NODE_ID + " = ?", new String[]{node.getId()}).build());

        // Insert all parents for the node. There could be multiple parents for each node.
        List<String> nodeParentIds = node.getParents();
        for (String nodeParentId : nodeParentIds) {
            replaceNodeParentOperations.add(
                    ContentProviderOperation.newInsert(CloudDriveContract.NodeParents.CONTENT_URI)
                            .withValue(CloudDriveContract.NodeParents.NODE_ID, node.getId())
                            .withValue(CloudDriveContract.NodeParents.PARENT_NODE_ID, nodeParentId).build());
        }

        return replaceNodeParentOperations;
    }

    private ArrayList<ContentProviderOperation> createUpdateAllChildrenToDirtyOperations(String parentNodeId) {
        Cursor parentCursor = null;
        try {
            parentCursor = getContentResolver().query(
                    CloudDriveContract.NodeChildren.CONTENT_URI,
                    new String[]{CloudDriveContract.NodeChildren._ID},
                    CloudDriveContract.NodeChildren.PARENT_NODE_ID + " = ?",
                    new String[]{parentNodeId},
                    null);
            int idIndex = parentCursor.getColumnIndex(CloudDriveContract.NodeChildren._ID);
            ArrayList<ContentProviderOperation> updateToDirtyOperations = new ArrayList<ContentProviderOperation>();
            for (parentCursor.moveToFirst(); !parentCursor.isAfterLast(); parentCursor.moveToNext()) {
                int id = parentCursor.getInt(idIndex);
                updateToDirtyOperations.add(ContentProviderOperation.newUpdate(
                        CloudDriveContract.Nodes.CONTENT_URI.buildUpon().appendPath(Integer.toString(id)).build())
                        .withValue(CloudDriveContract.Nodes.IS_DIRTY, 1).build());

            }
            return updateToDirtyOperations;
        } finally {
            Closer.closeQuietly(parentCursor);
        }
    }
}
