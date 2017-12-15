package com.example.clouddrivelisting;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.amazon.clouddrive.AmazonCloudDrive;
import com.amazon.clouddrive.AmazonCloudDriveClient;
import com.amazon.clouddrive.auth.AmazonAuthorizationConnectionFactory;
import com.amazon.clouddrive.auth.ApplicationScope;
import com.amazon.clouddrive.configuration.AccountConfiguration;
import com.amazon.clouddrive.configuration.ClientConfiguration;
import com.amazon.clouddrive.handlers.AsyncHandler;
import com.amazon.clouddrive.model.ListNodesRequest;
import com.amazon.clouddrive.model.ListNodesResponse;
import com.amazon.clouddrive.model.Node;
import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager;
import com.amazon.identity.auth.device.authorization.api.AuthorizationListener;
import com.amazon.identity.auth.device.authorization.api.AuthzConstants;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getName();

    // Button that logs the user in
    private ImageButton mLoginButton;

    // ListView that displays Cloud Drive Node information
    private ListView mNodeList;

    // LWA authorization managger
    private AmazonAuthorizationManager mAuthManager;

    // Amazon Cloud Drive client used for getting the nodes
    private AmazonCloudDrive mAmazonCloudDriveClient;

    // Authorization scopes used for getting information from
    // LWA and Amazon Cloud Drive
    private static final String[] APP_AUTHORIZATION_SCOPES = {
            ApplicationScope.CLOUDDRIVE_READ,
            ApplicationScope.CLOUDDRIVE_WRITE,
            "profile"};

    /**
     * {@link AuthorizationListener} which is passed in to authorize calls made on the {@link AmazonAuthorizationManager} member.
     * Starts getToken workflow if the authorization was successful, or displays a toast if the user cancels authorization.
     */
    private class LoginListener implements AuthorizationListener {

        /**
         * Authorization was completed successfully.
         * Display the profile of the user who just completed authorization
         * @param response bundle containing authorization response. Not used.
         */
        @Override
        public void onSuccess(Bundle response) {
            moveStateToReady();
        }

        /**
         * There was an error during the attempt to authorize the application.
         * Log the error, and reset the profile text view.
         * @param ae the error that occurred during authorize
         */
        @Override
        public void onError(AuthError ae) {
            Log.e(TAG, "AuthError during authorization", ae);
            showToast("Error during authorization.  Please try again.");
            moveStateToRequiresLogin();
        }

        /**
         * Authorization was cancelled before it could be completed.
         * A toast is shown to the user, to confirm that the operation was cancelled, and the profile text view is reset.
         * @param cause bundle containing the cause of the cancellation. Not used.
         */
        @Override
        public void onCancel(Bundle cause) {
            showToast("Authorization cancelled");
            moveStateToRequiresLogin();
        }
    }

    /**
     * Listener for getting the token. Moves to the Ready state if successful,
     * otherwise, moves to RequiresLogin state.
     */
    private class GetTokenListener implements AuthorizationListener {

        @Override
        public void onCancel(Bundle bundle) {
            moveStateToRequiresLogin();
        }

        @Override
        public void onSuccess(Bundle bundle) {
            if (bundle.getString(AuthzConstants.BUNDLE_KEY.TOKEN.val) != null) {
                moveStateToReady();
            } else {
                moveStateToRequiresLogin();
            }
        }

        @Override
        public void onError(AuthError authError) {
            moveStateToRequiresLogin();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mLoginButton = (ImageButton) findViewById(R.id.login_with_amazon_button);
        mLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

        mNodeList = (ListView) findViewById(R.id.node_list);

        try {
            mAuthManager = new AmazonAuthorizationManager(this, Bundle.EMPTY);
            mAuthManager.getToken(APP_AUTHORIZATION_SCOPES, new GetTokenListener());
        } catch (IllegalArgumentException e) {
            //
            // We cannot proceed if the API key is invalid. Finish the Activity.
            //
            showToast("Unable to Use Amazon Authorization Manager. APIKey is incorrect or does not exist.");
            Log.e(TAG, "Unable to Use Amazon Authorization Manager. APIKey is incorrect or does not exist.", e);
            finish();
        }
    }

    /**
     * Moves to the Ready state. Hides the login button, creates AmazonCloudDrive client, starts fetch.
     */
    private void moveStateToReady() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLoginButton.setVisibility(View.GONE);
            }
        });

        //
        // AmazonCloudDriveClient requires AccountConfiguration (which it uses to get
        // authentication tokens from LWA) and ClientConfiguration (which has the user agent).
        //
        mAmazonCloudDriveClient = new AmazonCloudDriveClient(
                new AccountConfiguration(new AmazonAuthorizationConnectionFactory(mAuthManager, APP_AUTHORIZATION_SCOPES)),
                new ClientConfiguration("ExampleAgent/1.0")
        );

        refreshNodeListingFromCloudDrive();
    }

    /**
     * Moves to RequiresLogin state. Shows the login button.
     */
    private void moveStateToRequiresLogin() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLoginButton.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Authorizes the application, moves to Ready or RequiresLogin when complete
     */
    private void login() {
        mAuthManager.authorize(APP_AUTHORIZATION_SCOPES, Bundle.EMPTY, new LoginListener());
    }

    /**
     * Updates the mNodeList to show node kind and name.
     * @param nodes The nodes to show
     */
    private void updateNodeListView(List<Node> nodes) {
        final List<String> nodeStrings = new ArrayList<String>();
        for (Node node : nodes) {
            nodeStrings.add(node.getKind() + ":" + node.getName());
        }
        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                        MainActivity.this,
                        android.R.layout.simple_list_item_1,
                        nodeStrings);
                mNodeList.setAdapter(arrayAdapter);
            }
        });
    }

    /**
     * Fetches the a node listing from Cloud Drive, updates ListView with results.
     */
    private void refreshNodeListingFromCloudDrive() {

        ListNodesRequest listNodesRequest = new ListNodesRequest();
        mAmazonCloudDriveClient.listNodesAsync(listNodesRequest, new AsyncHandler<ListNodesRequest, ListNodesResponse>() {

            @Override
            public void onError(ListNodesRequest listNodesRequest, final Exception e) {
                showToast("There was an error calling Amazon Cloud Drive " + e.getMessage());
            }

            @Override
            public void onCanceled(ListNodesRequest listNodesRequest) {
                // This callback happens when the operation was canceled
            }

            @Override
            public void onSuccess(ListNodesRequest listNodesRequest, ListNodesResponse listNodesResponse) {
                List<Node> nodes = listNodesResponse.getData();
                updateNodeListView(nodes);
            }
        });
    }

    /**
     * Shows a toast
     * @param toastMessage The message to show to the user
     */
    private void showToast(final String toastMessage){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        });
    }
}