package com.amazon.sample.iap.subscription;

import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.RequestId;

/**
 * Main activity for IAP subscriptions Sample Code
 * 
 * This is the main activity for this project.
 */
public class MainActivity extends Activity {
    private SampleIapManager sampleIapManager;

    /**
     * Setup IAP SDK and other UI related objects specific to this sample
     * application.
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupApplicationSpecificOnCreate();
        setupIAPOnCreate();
    }

    /**
     * Setup for IAP SDK called from onCreate. Sets up {@link SampleIapManager}
     * to handle InAppPurchasing logic and {@link SamplePurchasingListener} for
     * listening to IAP API callbacks
     */
    private void setupIAPOnCreate() {
        sampleIapManager = new SampleIapManager(this);
        final SamplePurchasingListener purchasingListener = new SamplePurchasingListener(sampleIapManager);
        Log.d(TAG, "onCreate: registering PurchasingListener");
        PurchasingService.registerListener(this.getApplicationContext(), purchasingListener);

    }

    /**
     * Call {@link PurchasingService#getProductData(Set)} to get the product
     * availability
     */
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: call getProductData for skus: " + MySku.values());
        final Set<String> productSkus = new HashSet<String>();
        for (final MySku mySku : MySku.values()) {
            productSkus.add(mySku.getSku());
        }
        PurchasingService.getProductData(productSkus);
    }

    /**
     * Calls {@link PurchasingService#getUserData()} to get current Amazon
     * user's data and {@link PurchasingService#getPurchaseUpdates(boolean)} to
     * get recent purchase updates
     */
    @Override
    protected void onResume() {
        super.onResume();
        sampleIapManager.activate();
        Log.d(TAG, "onResume: call getUserData");
        PurchasingService.getUserData();
        Log.d(TAG, "onResume: getPurchaseUpdates");
        PurchasingService.getPurchaseUpdates(false);

    }

    /**
     * Deactivate Sample IAP manager on main activity's Pause event
     */
    @Override
    protected void onPause() {
        super.onPause();
        sampleIapManager.deactivate();
    }

    /**
     * Click handler invoked when user clicks button to buy magazine
     * subscription. This method calls
     * {@link PurchasingService#purchase(String)} with the SKU to initialize the
     * purchase from Amazon Appstore
     */
    public void onBuyMagazineClick(final View view) {
        final RequestId requestId = PurchasingService.purchase(MySku.MY_MAGAZINE_SUBS.getSku());
        Log.d(TAG, "onBuyMagazineClick: requestId (" + requestId + ")");
    }

    /**
     * Callback on failed purchase updates response
     * {@link PurchaseUpdatesRequestStatus#FAILED}
     * 
     * @param requestId
     */
    public void onPurchaseUpdatesResponseFailed(final String requestId) {
        Log.d(TAG, "onPurchaseUpdatesResponseFailed: for requestId (" + requestId + ")");
    }

    // ///////////////////////////////////////////////////////////////////////////////////////
    // ////////////////////////// Application specific code below
    // ////////////////////////////
    // ///////////////////////////////////////////////////////////////////////////////////////

    private static final String TAG = "SampleIAPSubscriptionsApp";

    private Handler guiThreadHandler;

    // Button to subscribe magazine
    private Button buyMagazineButton;

    // TextView shows whether user is subscribed to magazine
    private TextView isSubscriptionEnabled;

    /**
     * Setup application specific things, called from onCreate()
     */
    private void setupApplicationSpecificOnCreate() {
        setContentView(R.layout.activity_main);

        buyMagazineButton = (Button) findViewById(R.id.buy_magazine_button);

        resetApplication();

        guiThreadHandler = new Handler();
    }

    /**
     * Show "Subscription Disabled" text in gray color to indicate user is not
     * subscribed to this magazine initially
     */
    private void resetApplication() {
        isSubscriptionEnabled = (TextView) findViewById(R.id.is_magazine_enabled);
        isSubscriptionEnabled.setText(R.string.subscription_disabled);
        isSubscriptionEnabled.setTextColor(Color.GRAY);
        isSubscriptionEnabled.setBackgroundColor(Color.WHITE);
    }

    /**
     * Disable "Buy Magazine Subscription" button
     */
    private void disableBuyMagazineButton() {
        buyMagazineButton.setEnabled(false);
    }

    /**
     * Enable "Buy Magazine Subscription" button
     */
    private void enableBuyMagazineButton() {
        buyMagazineButton.setEnabled(true);
    }

    /**
     * Show Subscription as enabled in view
     */
    private void enableMagazineSubscriptionInView() {
        Log.i(TAG,
              "enableMagazineSubscriptionInView: enabling magazine subscription, show by setting text color to blue and highlighting");
        guiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                isSubscriptionEnabled.setText(R.string.subscription_enabled);
                isSubscriptionEnabled.setTextColor(Color.BLUE);
                isSubscriptionEnabled.setBackgroundColor(Color.YELLOW);
            }
        });
    }

    /**
     * Show Subscription as disabled in view
     */
    protected void disableMagazineSubscriptionInView() {
        guiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                isSubscriptionEnabled.setText(R.string.subscription_disabled);
                isSubscriptionEnabled.setTextColor(Color.GRAY);
                isSubscriptionEnabled.setBackgroundColor(Color.WHITE);
            }
        });
    }

    /**
     * Show message on UI
     * 
     * @param message
     */
    public void showMessage(final String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Set the magazine subscription button status on UI
     * 
     * @param productAvailable
     * @param userSubscribed
     */
    public void setMagazineSubsAvail(final boolean productAvailable, final boolean userCanSubscribe) {
        if (productAvailable) {
            if (userCanSubscribe) {
                disableMagazineSubscriptionInView();
                enableBuyMagazineButton();
            } else {
                enableMagazineSubscriptionInView();
                disableBuyMagazineButton();
            }
        } else {
            disableMagazineSubscriptionInView();
            disableBuyMagazineButton();
        }

    }
}
