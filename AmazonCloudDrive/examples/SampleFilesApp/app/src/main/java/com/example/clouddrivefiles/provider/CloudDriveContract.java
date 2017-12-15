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
package com.example.clouddrivefiles.provider;

import android.net.Uri;
import android.provider.OpenableColumns;

/**
 * <p>
 * The contract between the CloudNodes provider and applications. Contains
 * definitions for the supported URIs and columns.
 * </p>
 * <h3>Overview</h3>
 * <p>
 * CloudNodesContract defines a database of Cloud Drive Node related information.
 * </p>
 * <p>
 * Tables include:
 * </p>
 * <ul>
 * <li>
 * {@link CloudDriveContract.Nodes}: Table that contains the Node info
 * </li>
 * <li>
 * {@link CloudDriveContract.NodeParents}: Table that contains the parents for a node
 * </li>
 * <li>
 * {@link CloudDriveContract.NodeChildren}: View that contains the children for a node
 * </li>
 * </ul>
 */
public class CloudDriveContract {

    private static final String CONTENT_RESOURCE = "content://";
    public static final String AUTHORITY = "com.example.clouddrivefiles";

    public static final String MIME_TYPE_DIR = "vnd.android.cursor.dir";
    public static final String MIME_TYPE_ITEM = "vnd.android.cursor.item";

    /**
     * A view of all of the node info excluding any one-to-many relationships.
     */
    public static final class Nodes {
        public static final String TABLE_NAME = "nodes";

        public static Uri CONTENT_URI = Uri.parse(CONTENT_RESOURCE + AUTHORITY + "/" + TABLE_NAME);

        /**
         * The MIME-type of content providing a directory of nodes
         */
        public static final String CONTENT_MIME_TYPE = "vnd.android.cursor.dir/nodes";

        /**
         * The MIME type of a content item.
         */
        public static final String CONTENT_ITEM_MIME_TYPE = "vnd.android.cursor.item/nodes";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String _ID = "_id";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String NODE_ID = "node_id";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String CREATED_BY = "created_by";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String CREATED_DATE = "created_date";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String DESCRIPTION = "description";

        /**
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String EXCLUSIVELY_TRASHED = "exclusively_trashed";

        /**
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String IS_ROOT = "is_root";

        /**
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String IS_SHARED = "is_shared";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String KIND = "kind";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String MODIFIED_DATE = "modified_date";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String NAME = "name";

        /**
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String RECURSIVELY_TRASHED = "recursively_trashed";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String STATUS = "status";

        /**
         * <P>Type: INTEGER</P>
         */
        public static final String VERSION = "version";

        /**
         * Flag indicating whether this row needs to be updated
         * from the service.
         *
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String IS_DIRTY = "is_dirty";
    }

    /**
     * Parents (containers) of the Nodes
     */
    public static final class NodeParents {
        public static final String TABLE_NAME = "node_parents";

        public static Uri CONTENT_URI = Uri.parse(CONTENT_RESOURCE + AUTHORITY + "/" + TABLE_NAME);
        /**
         * The MIME-type of content providing a directory of node_parents
         */
        public static final String CONTENT_MIME_TYPE = "vnd.android.cursor.dir/node_parents";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String _ID = "_id";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String NODE_ID = "node_id";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String PARENT_NODE_ID = "parent_node_id";
    }

    /**
     * Parents (containers) of the Nodes
     */
    public static final class NodeChildren {
        public static final String TABLE_NAME = "node_children";

        public static Uri CONTENT_URI = Uri.parse(CONTENT_RESOURCE + AUTHORITY + "/" + TABLE_NAME);

        /**
         * The MIME-type of content providing a directory of nodes
         */
        public static final String CONTENT_MIME_TYPE = "vnd.android.cursor.dir/node_children";

        /**
         * The MIME type of a content item.
         */
        public static final String CONTENT_ITEM_MIME_TYPE = "vnd.android.cursor.item/node_children";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String _ID = "_id";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String PARENT_NODE_ID = "parent_node_id";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String PARENT_IS_ROOT = "parent_is_root";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String NODE_ID = "node_id";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String CREATED_BY = "created_by";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String CREATED_DATE = "created_date";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String DESCRIPTION = "description";

        /**
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String EXCLUSIVELY_TRASHED = "exclusively_trashed";

        /**
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String IS_ROOT = "is_root";

        /**
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String IS_SHARED = "is_shared";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String KIND = "kind";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String MODIFIED_DATE = "modified_date";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String NAME = "name";

        /**
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String RECURSIVELY_TRASHED = "recursively_trashed";

        /**
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String RESTRICTED = "restricted";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String STATUS = "status";

        /**
         * <P>Type: INTEGER</P>
         */
        public static final String VERSION = "version";
    }

    /**
     * Contents for nodes.
     */
    public static final class NodeContents implements OpenableColumns {
        public static final String TABLE_NAME = "node_contents";

        public static Uri getContentUri(int id) {
            return Uri.parse(CONTENT_RESOURCE + AUTHORITY + "/" + Nodes.TABLE_NAME + "/" + id + "/content");
        }

        /**
         * The MIME-type of content providing a directory of node_parents
         */
        public static final String CONTENT_ITEM_MIME_TYPE = "vnd.android.cursor.item/" + TABLE_NAME;

        /**
         * <P>Type: TEXT</P>
         */
        public static final String _ID = "_id";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String DATA = "_data";
    }

    /**
     * Upload queue items
     */
    public static final class UploadQueueItems {
        public static final String TABLE_NAME = "upload_queue_entries";

        public static Uri CONTENT_URI = Uri.parse(CONTENT_RESOURCE + AUTHORITY + "/" + TABLE_NAME);

        /**
         * The MIME-type of content providing a directory of upload queue entries
         */
        public static final String CONTENT_MIME_TYPE = "vnd.android.cursor.dir/" + TABLE_NAME;

        /**
         * <P>Type: TEXT</P>
         */
        public static final String _ID = "_id";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String SOURCE_URI = "source_uri";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String STATUS = "status";
    }
}
