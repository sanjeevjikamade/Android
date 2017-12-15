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
package com.example.clouddrivefiles.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.clouddrivefiles.provider.CloudDriveContract;

/**
 * Creates database tables, views, and indices
 */
public class CloudDriveNodesDatabaseHelper extends SQLiteOpenHelper {

    /**
     * Current db version
     */
    private static final int DB_VERSION = 1;

    private static final String DB_NAME = "com.example.clouddrivefiles.db";

    public CloudDriveNodesDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        ///////////////////////////////////////////////////////////////////////
        // nodes
        ///////////////////////////////////////////////////////////////////////

        db.execSQL(
                "CREATE TABLE " + CloudDriveContract.Nodes.TABLE_NAME + "(" +
                        CloudDriveContract.Nodes._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        CloudDriveContract.Nodes.NODE_ID + " TEXT UNIQUE NOT NULL, " +
                        CloudDriveContract.Nodes.CREATED_BY + " TEXT, " +
                        CloudDriveContract.Nodes.CREATED_DATE + " TEXT, " +
                        CloudDriveContract.Nodes.DESCRIPTION + " TEXT, " +
                        CloudDriveContract.Nodes.EXCLUSIVELY_TRASHED + " INTEGER, " +
                        CloudDriveContract.Nodes.IS_ROOT + " INTEGER, " +
                        CloudDriveContract.Nodes.IS_SHARED + " INTEGER, " +
                        CloudDriveContract.Nodes.KIND + " TEXT, " +
                        CloudDriveContract.Nodes.MODIFIED_DATE + " TEXT, " +
                        CloudDriveContract.Nodes.NAME + " TEXT, " +
                        CloudDriveContract.Nodes.RECURSIVELY_TRASHED + " INTEGER, " +
                        CloudDriveContract.Nodes.STATUS + " TEXT, " +
                        CloudDriveContract.Nodes.VERSION + " INTEGER, " +
                        CloudDriveContract.Nodes.IS_DIRTY + " INTEGER" +
                        ")");

        db.execSQL(
                "CREATE INDEX idx_nodes_nid " +
                        "ON " + CloudDriveContract.Nodes.TABLE_NAME + " (" +
                        CloudDriveContract.Nodes.NODE_ID +
                        ")");
        db.execSQL(
                "CREATE INDEX idx_nodes_md " +
                        "ON " + CloudDriveContract.Nodes.TABLE_NAME + " (" +
                        CloudDriveContract.Nodes.MODIFIED_DATE +
                        ")");


        ///////////////////////////////////////////////////////////////////////
        // node_parents
        ///////////////////////////////////////////////////////////////////////

        db.execSQL(
                "CREATE TABLE " + CloudDriveContract.NodeParents.TABLE_NAME + "(" +
                        CloudDriveContract.NodeParents._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        CloudDriveContract.NodeParents.NODE_ID + " TEXT NOT NULL, " +
                        CloudDriveContract.NodeParents.PARENT_NODE_ID + " TEXT NOT NULL " +
                        ")");


        ///////////////////////////////////////////////////////////////////////
        // node_children
        ///////////////////////////////////////////////////////////////////////

        db.execSQL(
                "CREATE VIEW " + CloudDriveContract.NodeChildren.TABLE_NAME + " AS " +
                    "SELECT " +
                        "n." + CloudDriveContract.Nodes._ID + " AS " + CloudDriveContract.NodeChildren._ID + ", " +
                        "np." + CloudDriveContract.NodeParents.PARENT_NODE_ID + " AS " + CloudDriveContract.NodeChildren.PARENT_NODE_ID + ", " +
                        "npn." + CloudDriveContract.Nodes.IS_ROOT + " AS " + CloudDriveContract.NodeChildren.PARENT_IS_ROOT + ", " +
                        "n." + CloudDriveContract.Nodes.NODE_ID + " AS " + CloudDriveContract.NodeChildren.NODE_ID + ", " +
                        "n." + CloudDriveContract.Nodes.CREATED_BY + " AS " + CloudDriveContract.NodeChildren.CREATED_BY + ", " +
                        "n." + CloudDriveContract.Nodes.CREATED_DATE + " AS " + CloudDriveContract.NodeChildren.CREATED_DATE + ", " +
                        "n." + CloudDriveContract.Nodes.DESCRIPTION + " AS " + CloudDriveContract.NodeChildren.DESCRIPTION + ", " +
                        "n." + CloudDriveContract.Nodes.EXCLUSIVELY_TRASHED + " AS " + CloudDriveContract.NodeChildren.EXCLUSIVELY_TRASHED + ", " +
                        "n." + CloudDriveContract.Nodes.IS_ROOT + " AS " + CloudDriveContract.NodeChildren.IS_ROOT + ", " +
                        "n." + CloudDriveContract.Nodes.IS_SHARED + " AS " + CloudDriveContract.NodeChildren.IS_SHARED + ", " +
                        "n." + CloudDriveContract.Nodes.KIND + " AS " + CloudDriveContract.NodeChildren.KIND + ", " +
                        "n." + CloudDriveContract.Nodes.MODIFIED_DATE+ " AS " + CloudDriveContract.NodeChildren.MODIFIED_DATE + ", " +
                        "n." + CloudDriveContract.Nodes.NAME + " AS " + CloudDriveContract.NodeChildren.NAME + ", " +
                        "n." + CloudDriveContract.Nodes.RECURSIVELY_TRASHED + " AS " + CloudDriveContract.NodeChildren.RECURSIVELY_TRASHED + ", " +
                        "n." + CloudDriveContract.Nodes.STATUS + " AS " + CloudDriveContract.NodeChildren.STATUS + ", " +
                        "n." + CloudDriveContract.Nodes.VERSION + " AS " + CloudDriveContract.NodeChildren.VERSION + " " +
                    "FROM " +
                        CloudDriveContract.NodeParents.TABLE_NAME + " np, " +
                        CloudDriveContract.Nodes.TABLE_NAME + " n, " +
                        CloudDriveContract.Nodes.TABLE_NAME + " npn " +
                    "WHERE " +
                        "np." + CloudDriveContract.NodeParents.NODE_ID + " = n." + CloudDriveContract.Nodes.NODE_ID +
                        " AND np." + CloudDriveContract.NodeParents.PARENT_NODE_ID + " = npn." + CloudDriveContract.Nodes.NODE_ID + " " );

        ///////////////////////////////////////////////////////////////////////
        // node_contents
        ///////////////////////////////////////////////////////////////////////

        db.execSQL(
                "CREATE TABLE " + CloudDriveContract.NodeContents.TABLE_NAME + "(" +
                        CloudDriveContract.NodeContents._ID + " INTEGER PRIMARY KEY, " +
                        CloudDriveContract.NodeContents.DATA + " TEXT, " +
                        CloudDriveContract.NodeContents.DISPLAY_NAME + " TEXT, " +
                        CloudDriveContract.NodeContents.SIZE + " INTEGER" +
                        ")");

        ///////////////////////////////////////////////////////////////////////
        // upload_queue_entries
        ///////////////////////////////////////////////////////////////////////

        db.execSQL(
                "CREATE TABLE " + CloudDriveContract.UploadQueueItems.TABLE_NAME + "(" +
                        CloudDriveContract.UploadQueueItems._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        CloudDriveContract.UploadQueueItems.SOURCE_URI + " TEXT NOT NULL, " +
                        CloudDriveContract.UploadQueueItems.STATUS + " TEXT " +
                        ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}