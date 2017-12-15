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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.amazon.clouddrive.model.NodeKind;
import com.example.clouddrivefiles.R;
import com.example.clouddrivefiles.activity.ContentActivity;
import com.example.clouddrivefiles.provider.CloudDriveContract;
import com.example.clouddrivefiles.service.CloudDriveFolderListingService;

public class NodeListingFragment extends Fragment {

    private static final String TAG = NodeListingFragment.class.getSimpleName();

    public static final String ARG_PARENT_NODE_ID = "parent_node_id";

    private ListView mNodeListing;
    private NodesAdapter mNodesAdapter;
    private String mParentNodeId;

    private final LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(final int i, final Bundle bundle) {
            return new CursorLoader(
                    getActivity(),
                    CloudDriveContract.NodeChildren.CONTENT_URI,
                    new String[]{
                            CloudDriveContract.NodeChildren._ID,
                            CloudDriveContract.NodeChildren.NODE_ID,
                            CloudDriveContract.NodeChildren.NAME,
                            CloudDriveContract.NodeChildren.KIND,
                            CloudDriveContract.NodeChildren.MODIFIED_DATE},
                    CloudDriveContract.NodeChildren.STATUS + " != ?" +
                            " AND " + CloudDriveContract.NodeChildren.STATUS + " != ?" +
                            " AND " + CloudDriveContract.NodeChildren.KIND + " != ?" +
                            " AND " + CloudDriveContract.NodeChildren.PARENT_NODE_ID + " = ?",
                    new String[]{"PURGED", "TRASH", "ASSET", mParentNodeId},
                    CloudDriveContract.NodeChildren.KIND + " DESC, " + CloudDriveContract.NodeChildren.NAME + " ASC ");
        }

        @Override
        public void onLoadFinished(final Loader<Cursor> cursorLoader, final Cursor cursor) {
            mNodesAdapter.changeCursor(cursor);
        }

        @Override
        public void onLoaderReset(final Loader<Cursor> cursorLoader) {
            mNodesAdapter.changeCursor(null);
        }
    };

    // There is a special query against the ContentProvider to get the root children
    // because we may not know what the root's node ID is yet. This query allows us
    // to get all of the children without having to resolve it first in this Fragment.
    private final LoaderCallbacks<Cursor> mRootLoaderCallbacks = new LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(final int i, final Bundle bundle) {
            return new CursorLoader(
                    getActivity(),
                    CloudDriveContract.NodeChildren.CONTENT_URI,
                    new String[]{
                            CloudDriveContract.NodeChildren._ID,
                            CloudDriveContract.NodeChildren.NODE_ID,
                            CloudDriveContract.NodeChildren.NAME,
                            CloudDriveContract.NodeChildren.KIND,
                            CloudDriveContract.NodeChildren.MODIFIED_DATE},
                    CloudDriveContract.NodeChildren.STATUS + " != ?" +
                            " AND " + CloudDriveContract.NodeChildren.STATUS + " != ?" +
                            " AND " + CloudDriveContract.NodeChildren.KIND + " != ?" +
                            " AND " + CloudDriveContract.NodeChildren.PARENT_IS_ROOT + " = ?",
                    new String[]{"PURGED", "TRASH", "ASSET", "1"},
                    CloudDriveContract.NodeChildren.KIND + " DESC, " + CloudDriveContract.NodeChildren.NAME + " ASC ");
        }

        @Override
        public void onLoadFinished(final Loader<Cursor> cursorLoader, final Cursor cursor) {
            mNodesAdapter.changeCursor(cursor);
        }

        @Override
        public void onLoaderReset(final Loader<Cursor> cursorLoader) {
            mNodesAdapter.changeCursor(null);
        }
    };

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.nodes_view, container, false);

        mNodeListing = (ListView) view.findViewById(R.id.nodes_list_view);
        mNodesAdapter = new NodesAdapter(null);
        mNodeListing.setAdapter(mNodesAdapter);
        mNodeListing.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Tag tag = (Tag) view.getTag();

                if (NodeKind.FILE.equals(tag.kind)) {
                    // Download and open the file
                    DownloadDialog downloadDialog = DownloadDialog.newInstance(tag.id, tag.nodeId, tag.name);
                    FragmentManager fm = getActivity().getSupportFragmentManager();
                    downloadDialog.show(fm, "download_progress_fragment");
                } else if (NodeKind.FOLDER.equals(tag.kind)) {
                    // List the contents by opening another NodeFragment
                    ContentActivity contentActivity = (ContentActivity) getActivity();
                    NodeListingFragment nodeListingFragment = new NodeListingFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString(ARG_PARENT_NODE_ID, tag.nodeId);
                    nodeListingFragment.setArguments(bundle);
                    contentActivity.openFragment(nodeListingFragment, "nodes_fragment");
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        Bundle arguments = getArguments();
        if (arguments != null) {
            mParentNodeId = arguments.getString(ARG_PARENT_NODE_ID);
        }

        if (mParentNodeId == null) {
            // list root
            Intent listRootFolderIntent = CloudDriveFolderListingService.newListRootFolderIntent(getActivity());
            getActivity().startService(listRootFolderIntent);
            getActivity().getSupportLoaderManager().initLoader(0, null, mRootLoaderCallbacks);
        } else {
            // list other folder
            Intent listFolderIntent = CloudDriveFolderListingService.newListFolderIntent(getActivity(), mParentNodeId);
            getActivity().startService(listFolderIntent);
            getActivity().getSupportLoaderManager().initLoader(mParentNodeId.hashCode(), null, mLoaderCallbacks);
        }

        mNodeListing.requestFocus();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private class NodesAdapter extends CursorAdapter {

        private NodesAdapter(final Cursor cursor) {
            super(getActivity(), cursor, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        }

        @Override
        public View newView(final Context context, final Cursor cursor, final ViewGroup viewGroup) {
            return LayoutInflater.from(context).inflate(R.layout.node_row, viewGroup, false);
        }

        @Override
        public void bindView(final View view, final Context context, final Cursor cursor) {
            Tag tag = (Tag) view.getTag();
            if (tag == null) {
                tag = new Tag();

                // Hold the views and the columns so they do not need to
                tag.nodeNameTextView = (TextView) view.findViewById(R.id.node_name);
                tag.iconImageView = (ImageView) view.findViewById(R.id.icon);
                tag.idColumnIndex = cursor.getColumnIndex(CloudDriveContract.NodeChildren._ID);
                tag.nodeIdColumnIndex = cursor.getColumnIndex(CloudDriveContract.NodeChildren.NODE_ID);
                tag.nameColumnIndex = cursor.getColumnIndex(CloudDriveContract.NodeChildren.NAME);
                tag.dateColumnIndex = cursor.getColumnIndex(CloudDriveContract.NodeChildren.MODIFIED_DATE);
                tag.kindColumnIndex = cursor.getColumnIndex(CloudDriveContract.NodeChildren.KIND);
            }

            tag.id = cursor.getInt(tag.idColumnIndex);
            tag.nodeId = cursor.getString(tag.nodeIdColumnIndex);
            tag.kind = cursor.getString(tag.kindColumnIndex);
            tag.name = cursor.getString(tag.nameColumnIndex);

            TextView nodeNameTextView = tag.nodeNameTextView;
            nodeNameTextView.setText(tag.name);

            String mimeType = null;
            if (tag.name != null) {
                String[] parts = tag.name.split("[.]");
                if (parts.length > 0) {
                    mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                            parts[parts.length - 1].toLowerCase());
                }
            }

            final String kind = cursor.getString(tag.kindColumnIndex);
            ImageView iconImageView = tag.iconImageView;
            if (NodeKind.FOLDER.equals(kind)) {
                iconImageView.setImageResource(R.drawable.ic_folder);
            } else if (mimeType != null) {
                if (mimeType.startsWith("image")) {
                    iconImageView.setImageResource(R.drawable.ic_photo);
                } else if (mimeType.startsWith("video")) {
                    iconImageView.setImageResource(R.drawable.ic_video);
                } else if (mimeType.startsWith("audio")) {
                    iconImageView.setImageResource(R.drawable.ic_music);
                } else if (mimeType.startsWith("application/pdf")) {
                    iconImageView.setImageResource(R.drawable.ic_pdf);
                } else if (mimeType.startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
                    iconImageView.setImageResource(R.drawable.ic_excel);
                } else if (mimeType.startsWith("application/mspowerpoint")) {
                    iconImageView.setImageResource(R.drawable.ic_powerpoint);
                } else if (mimeType.startsWith("application/vnd.openxmlformats-officedocument.presentationml.presentation")) {
                    iconImageView.setImageResource(R.drawable.ic_powerpoint);
                } else if (mimeType.startsWith("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                    iconImageView.setImageResource(R.drawable.ic_word);
                } else {
                    iconImageView.setImageResource(R.drawable.ic_file);
                }
            } else {
                iconImageView.setImageResource(R.drawable.ic_file);
            }

            view.setTag(tag);
        }
    }

    private static class Tag {
        int id;
        String nodeId;
        String kind;
        String name;

        int idColumnIndex;
        int nodeIdColumnIndex;
        int nameColumnIndex;
        int dateColumnIndex;
        int kindColumnIndex;

        TextView nodeNameTextView;
        ImageView iconImageView;
    }
}
