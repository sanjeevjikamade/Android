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

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.example.clouddrivefiles.database.CloudDriveNodesDatabaseHelper;
import com.example.clouddrivefiles.provider.CloudDriveContract.Nodes;
import com.example.clouddrivefiles.utils.Closer;

import java.io.FileNotFoundException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides nodes from Amazon Cloud Drive.
 *
 * URI scheme is: content://<authority>/<table>/
 */
public class CloudDriveProvider extends ContentProvider {

    // ContentProvider authority (initialized when attachInfo is called)
    private String mAuthority;

    // Uri matcher is intialized when the authority is known
    private UriMatcher mUriMatcher;

    private CloudDriveNodesDatabaseHelper mDatabaseHelper;

    ///////////////////////////////////////////////////////////////////////////
    // Lifecycle methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        mAuthority = info.authority;
        initializeUriMatcher();
        super.attachInfo(context, info);
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new CloudDriveNodesDatabaseHelper(getContext());
        return true;
    }

    ///////////////////////////////////////////////////////////////////////////
    // CRUD operations
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        int uriMatch = mUriMatcher.match(uri);
        String tableName = uriMatchToTableName(uriMatch);
        if (tableName == null) {
            throw new UnknownUriException(uri);
        }

        SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();

        long rowId;
        try {
            rowId = database.replace(tableName, null, values);
        } catch (SQLiteConstraintException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        if (rowId >= 0) {
            getContext().getContentResolver().notifyChange(uri, null);
            // Children are potentially impacted
            getContext().getContentResolver().notifyChange(CloudDriveContract.NodeChildren.CONTENT_URI, null);
            return uri;
        } else {
            throw new SQLException("Failed to insert row into " + uri);
        }
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {

        int uriMatch = mUriMatcher.match(uri);
        String tableName = uriMatchToTableName(uriMatch);
        if (tableName == null) {
            throw new UnknownUriException(uri);
        }

        SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();

        Cursor cursor;

        String type = getType(uri);
        if (type.startsWith(CloudDriveContract.MIME_TYPE_DIR)) {
            // No special restrictions
            SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            qb.setTables(tableName);
            cursor = database.query(tableName, projection, selection, selectionArgs, null, null, sortOrder);
        } else {
            // Restrict to just this item
            SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            qb.setTables(tableName);
            qb.appendWhere(getTableIdWhereClause(uri));
            cursor = qb.query(database, projection, selection, selectionArgs, null, null, sortOrder);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int update(
            Uri uri,
            ContentValues values,
            String selection,
            String[] selectionArgs) {

        int uriMatch = mUriMatcher.match(uri);
        String tableName = uriMatchToTableName(uriMatch);
        if (tableName == null) {
            throw new UnknownUriException(uri);
        }

        SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();

        int count;
        String type = getType(uri);
        if (type.startsWith(CloudDriveContract.MIME_TYPE_DIR)) {
            // No special restrictions
            count = database.update(tableName, values, selection, selectionArgs);
        } else {
            // Restrict to just this item
            count = database.update(tableName, values, getTableIdWhereClause(uri), null);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        int uriMatch = mUriMatcher.match(uri);
        String tableName = uriMatchToTableName(uriMatch);
        if (tableName == null) {
            throw new UnknownUriException(uri);
        }

        SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();

        int count;
        String type = getType(uri);
        if (type.startsWith(CloudDriveContract.MIME_TYPE_DIR)) {
            // No special restrictions
            count = database.delete(tableName, selection, selectionArgs);
        } else {
            // Restrict to just this item
            count = database.delete(tableName, getTableIdWhereClause(uri), null);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        int match = mUriMatcher.match(uri);
        switch (match)
        {
            case UriMatcherConstants.NODES:
                return Nodes.CONTENT_MIME_TYPE;
            case UriMatcherConstants.NODE:
                return Nodes.CONTENT_ITEM_MIME_TYPE;
            case UriMatcherConstants.NODE_CONTENT:
                return getMimeTypeForNodeContent(uri);
            case UriMatcherConstants.NODE_PARENTS:
                return CloudDriveContract.NodeParents.CONTENT_MIME_TYPE;
            case UriMatcherConstants.NODE_CHILDREN:
                return CloudDriveContract.NodeChildren.CONTENT_MIME_TYPE;
            case UriMatcherConstants.UPLOAD_QUEUE_ENTRIES:
                return CloudDriveContract.UploadQueueItems.CONTENT_MIME_TYPE;
            default:
                return null;
        }
    }

    /**
     * Returns the mime type for the node content
     * @param uri URI of the node content
     * @return the mime type for the node content
     */
    private String getMimeTypeForNodeContent(Uri uri) {
        SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = database.query(
                    CloudDriveContract.NodeContents.TABLE_NAME,
                    null,
                    getTableIdWhereClause(uri),
                    null,
                    null,
                    null,
                    null);
            cursor.moveToFirst();
            if (cursor.isAfterLast()) {
                return null;
            }

            String fileName = cursor.getString(cursor.getColumnIndex(CloudDriveContract.NodeContents.DATA));
            return URLConnection.guessContentTypeFromName(fileName);

        } finally {
            Closer.closeQuietly(cursor);
        }
    }

    /**
     * Do all of the ContentProviderOperations in a single transaction for better performance
     *
     * @param operations the ContentProviderOperations that will be performed in order
     * @return the results of each operation
     * @throws android.content.OperationApplicationException
     */
    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {

        if (operations.isEmpty()) {
            return null;
        }

        Set<Uri> uris = new HashSet<Uri>();
        for (ContentProviderOperation operation : operations) {
            uris.add(operation.getUri());
        }

        SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
        ContentProviderResult[] result;

        database.beginTransaction();
        try {
            // Let the super class iterate through and do all of the operations
            result = super.applyBatch(operations);

            // Commit the transaction
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        return result;
    }

    /**
     * Opens a node file's contents. This file must have been successfully downloaded in order to open it.
     * @param uri The URI to open.
     * @param mode The file mode.
     * @return the ParcelFileDescriptor that can be used to read the file across processes
     * @throws FileNotFoundException
     */
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (mUriMatcher.match(uri) != UriMatcherConstants.NODE_CONTENT) {
            throw new IllegalArgumentException("Unable to open this type of file.");
        }

        // open the file in the _data column
        return openFileHelper(uri, mode);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Uri helpers
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Simple interface for holding the uri switcher constants
     */
    private static final class UriMatcherConstants {
        private static final int NODES = 1;                // all nodes.
        private static final int NODE = 2;                 // a specific node.
        private static final int NODE_CONTENT = 3;         // file contents for a node.
        private static final int NODE_PARENTS = 4;         // all parents.
        private static final int NODE_CHILDREN = 5;        // all children.
        private static final int UPLOAD_QUEUE_ENTRIES = 6; // all queue entries.
    }

    /**
     * Bind the switcher constants to the authority/table pair.
     */
    private void initializeUriMatcher() {

        //
        // URIs have the form of
        // * content://<authority>/<table_name>
        // * OR content://<authority>/<table_name>/<table_specific_id>
        //
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(mAuthority, Nodes.TABLE_NAME, UriMatcherConstants.NODES);
        mUriMatcher.addURI(mAuthority, Nodes.TABLE_NAME + "/*", UriMatcherConstants.NODE);
        mUriMatcher.addURI(mAuthority, Nodes.TABLE_NAME + "/*/content", UriMatcherConstants.NODE_CONTENT);
        mUriMatcher.addURI(mAuthority, CloudDriveContract.NodeParents.TABLE_NAME, UriMatcherConstants.NODE_PARENTS);
        mUriMatcher.addURI(mAuthority, CloudDriveContract.NodeChildren.TABLE_NAME, UriMatcherConstants.NODE_CHILDREN);
        mUriMatcher.addURI(mAuthority, CloudDriveContract.UploadQueueItems.TABLE_NAME, UriMatcherConstants.UPLOAD_QUEUE_ENTRIES);

    }

    /**
     * Returns the table for the content or content item
     * @param uriMatch the match ID produced by UriMatcher
     * @return the table name that the uriMatch constant maps to
     */
    private String uriMatchToTableName(int uriMatch) {

        switch (uriMatch)
        {
            case UriMatcherConstants.NODES:
                return CloudDriveContract.Nodes.TABLE_NAME;
            case UriMatcherConstants.NODE:
                return CloudDriveContract.Nodes.TABLE_NAME;
            case UriMatcherConstants.NODE_CONTENT:
                return CloudDriveContract.NodeContents.TABLE_NAME;
            case UriMatcherConstants.NODE_PARENTS:
                return CloudDriveContract.NodeParents.TABLE_NAME;
            case UriMatcherConstants.NODE_CHILDREN:
                return CloudDriveContract.NodeChildren.TABLE_NAME;
            case UriMatcherConstants.UPLOAD_QUEUE_ENTRIES:
                return CloudDriveContract.UploadQueueItems.TABLE_NAME;
            default:
                return null;
        }
    }

    /**
     * Returns a where clause that identifies a specific row.
     * @param uri the URI for a specific item
     * @return the where clause
     */
    private String getTableIdWhereClause(Uri uri) {

        final int TABLE_ID_PATH_SEGMENT = 1;
        String id = uri.getPathSegments().get(TABLE_ID_PATH_SEGMENT);
        return "_id = " + id;
    }
}