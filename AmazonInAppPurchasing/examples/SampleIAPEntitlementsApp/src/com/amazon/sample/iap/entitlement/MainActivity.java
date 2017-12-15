package com.amazon.sample.iap.entitlement;

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
 * Main activity for IAP Entitlement Sample Code
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
     * Call {@link PurchasingService#getProductData(Set)} to get the product availability  
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

    @Override
    protected void onPause() {
        super.onPause();
        sampleIapManager.deactivate();
    }

    /**
     * Click handler invoked when user clicks button to buy Level 2 access
     * entitlement. This method calls {@link PurchasingService#purchase(String)}
     * with the SKU to initialize the purchase from Amazon Appstore
     */
    public void onBuyAccessToLevel2Click(final View view) {
        final RequestId requestId = PurchasingService.purchase(MySku.LEVEL2.getSku());

        Log.d(TAG, "onBuyAccessToLevel2Click: requestId (" + requestId + ")");
    }

    // ///////////////////////////////////////////////////////////////////////////////////////
    // ////////////////////////// Application specific code below
    // ////////////////////////////
    // ///////////////////////////////////////////////////////////////////////////////////////

    private static final String TAG = "SampleIAPEntitlementsApp";

    private Handler guiThreadHandler;

    // Button to buy entitlement to level 2
    private Button buyLevel2Button;

    // TextView shows whether user has been entitled to level 2
    private TextView isLevel2EnabledTextView;

    /**
     * Setup application specific things, called from onCreate()
     */
    private void setupApplicationSpecificOnCreate() {
        setContentView(R.layout.activity_main);

        buyLevel2Button = (Button) findViewById(R.id.buy_level2_button);

        resetApplication();

        guiThreadHandler = new Handler();
    }

    /**
     * Show "Level 2 Disabled" text in gray color to indicate user does NOT have
     * this Entitlement initially
     */
    private void resetApplication() {
        //
        isLevel2EnabledTextView = (TextView) findViewById(R.id.is_level2_enabled);
        isLevel2EnabledTextView.setText(R.string.level2_disabled);
        isLevel2EnabledTextView.setTextColor(Color.GRAY);
        isLevel2EnabledTextView.setBackgroundColor(Color.WHITE);
        disableBuyLevel2Button();
    }

    /**
     * Disable "Buy Access to Level 2" button
     */
    private void disableBuyLevel2Button() {
        buyLevel2Button.setEnabled(false);
    }

    /**
     * Enable "Buy Access to Level 2" button
     */
    private void enableBuyLevel2Button() {
        buyLevel2Button.setEnabled(true);
    }

    /**
     * Show Level 2 as enabled in view
     */
    private void enableLevel2InView() {
        Log.d(TAG, "enableLevel2InView: enabling level 2, show by setting text color to blue and highlighting");
        guiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                isLevel2EnabledTextView.setText(R.string.level2_enabled);
                isLevel2EnabledTextView.setTextColor(Color.BLUE);
                isLevel2EnabledTextView.setBackgroundColor(Color.YELLOW);
            }
        });
    }

    /**
     * Show Level 2 as disabled in view
     */
    private void disableLevel2InView() {
        guiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                isLevel2EnabledTextView.setText(R.string.level2_disabled);
                isLevel2EnabledTextView.setTextColor(Color.GRAY);
                isLevel2EnabledTextView.setBackgroundColor(Color.WHITE);
            }
        });
    }

    /**
     * Show message on UI
     * @param message
     */
    public void showMessage(final String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Set the status for buy Level2 button 
     * @param productAvailable
     * @param userAlreadyPurchased
     */
    public void setLevel2Availbility(final boolean productAvailable, final boolean userAlreadyPurchased) {
        if (productAvailable) {
            if (userAlreadyPurchased) {
                enableLevel2InView();
                disableBuyLevel2Button();
            } else {
                disableLevel2InView();
                enableBuyLevel2Button();
            }
        } else {
            disableLevel2InView();
            disableBuyLevel2Button();
        }

    }
}
