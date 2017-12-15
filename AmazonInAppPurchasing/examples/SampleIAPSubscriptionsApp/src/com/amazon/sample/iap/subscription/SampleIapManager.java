package com.amazon.sample.iap.subscription;

import java.util.List;
import java.util.Map;
import java.util.Set;

import android.util.Log;

import com.amazon.device.iap.PurchasingListener;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.FulfillmentResult;
import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.UserData;

/**
 * This is a sample of how an application may handle InAppPurchasing. The major
 * functions includes
 * <ul>
 * <li>Simple user and subscription history management</li>
 * <li>Grant subscription purchases</li>
 * <li>Enable/disable subscribe from GUI</li>
 * <li>Save persistent subscriptions data into SQLite database</li>
 * </ul>
 * 
 * 
 */
public class SampleIapManager {
    private static final String TAG = "SampleIAPManager";
    private final MainActivity mainActivity;
    private final SubscriptionDataSource dataSource;

    private boolean magazineSubsAvailable;
    private UserIapData userIapData;

    public SampleIapManager(final MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.dataSource = new SubscriptionDataSource(mainActivity.getApplicationContext());
    }

    /**
     * Method to set the app's amazon user id and marketplace from IAP SDK
     * responses.
     * 
     * @param newAmazonUserId
     * @param newAmazonMarketplace
     */
    public void setAmazonUserId(final String newAmazonUserId, final String newAmazonMarketplace) {
        // Reload everything if the Amazon user has changed.
        if (newAmazonUserId == null) {
            // A null user id typically means there is no registered Amazon
            // account.
            if (userIapData != null) {
                userIapData = null;
                refreshMagazineSubsAvailability();
            }
        } else if (userIapData == null || !newAmazonUserId.equals(userIapData.getAmazonUserId())) {
            // If there was no existing Amazon user then either no customer was
            // previously registered or the application has just started.

            // If the user id does not match then another Amazon user has
            // registered.
            userIapData = new UserIapData(newAmazonUserId, newAmazonMarketplace);
            refreshMagazineSubsAvailability();
        }
    }

    /**
     * Enable the magazine subscription.
     * 
     * @param productData
     */
    public void enablePurchaseForSkus(final Map<String, Product> productData) {
        if (productData.containsKey(MySku.MY_MAGAZINE_SUBS.getSku())) {
            magazineSubsAvailable = true;
        }
    }

    /**
     * Disable the magazine subscription.
     * 
     * @param unavailableSkus
     */
    public void disablePurchaseForSkus(final Set<String> unavailableSkus) {
        if (unavailableSkus.contains(MySku.MY_MAGAZINE_SUBS.toString())) {
            magazineSubsAvailable = false;
            // reasons for product not available can be:
            // * Item not available for this country
            // * Item pulled off from Appstore by developer
            // * Item pulled off from Appstore by Amazon
            mainActivity.showMessage("the magazine subscription product isn't available now! ");
        }
    }

    /**
     * This method contains the business logic to fulfill the customer's
     * purchase based on the receipt received from InAppPurchase SDK's
     * {@link PurchasingListener#onPurchaseResponse} or
     * {@link PurchasingListener#onPurchaseUpdates} method.
     * 
     * 
     * @param requestId
     * @param receiptId
     */
    public void handleSubscriptionPurchase(final Receipt receipt, final UserData userData) {
        try {
            if (receipt.isCanceled()) {
                // Check whether this receipt is for an expired or canceled
                // subscription
                revokeSubscription(receipt, userData.getUserId());
            } else {
                // We strongly recommend that you verify the receipt on
                // server-side.
                if (!verifyReceiptFromYourService(receipt.getReceiptId(), userData)) {
                    // if the purchase cannot be verified,
                    // show relevant error message to the customer.
                    mainActivity.showMessage("Purchase cannot be verified, please retry later.");
                    return;
                }
                grantSubscriptionPurchase(receipt, userData);
            }
            return;
        } catch (final Throwable e) {
            mainActivity.showMessage("Purchase cannot be completed, please retry");
        }

    }

    private void grantSubscriptionPurchase(final Receipt receipt, final UserData userData) {

        final MySku mySku = MySku.fromSku(receipt.getSku(), userIapData.getAmazonMarketplace());
        // Verify that the SKU is still applicable.
        if (mySku != MySku.MY_MAGAZINE_SUBS) {
            Log.w(TAG, "The SKU [" + receipt.getSku() + "] in the receipt is not valid anymore ");
            // if the sku is not applicable anymore, call
            // PurchasingService.notifyFulfillment with status "UNAVAILABLE"
            PurchasingService.notifyFulfillment(receipt.getReceiptId(), FulfillmentResult.UNAVAILABLE);
            return;
        }
        try {
            // Set the purchase status to fulfilled for your application
            saveSubscriptionRecord(receipt, userData.getUserId());
            PurchasingService.notifyFulfillment(receipt.getReceiptId(), FulfillmentResult.FULFILLED);

        } catch (final Throwable e) {
            // If for any reason the app is not able to fulfill the purchase,
            // add your own error handling code here.
            Log.e(TAG, "Failed to grant entitlement purchase, with error " + e.getMessage());
        }

    }

    /**
     * Method to handle receipt
     * 
     * @param requestId
     * @param receipt
     * @param userData
     */
    public void handleReceipt(final String requestId, final Receipt receipt, final UserData userData) {
        switch (receipt.getProductType()) {
        case CONSUMABLE:
            // check consumable sample for how to handle consumable purchases
            break;
        case ENTITLED:
            // check entitlement sample for how to handle consumable purchases
            break;
        case SUBSCRIPTION:
            handleSubscriptionPurchase(receipt, userData);
            break;
        }
    }

    /**
     * Show purchase failed message
     * @param sku
     */
    public void purchaseFailed(final String sku) {
        mainActivity.showMessage("Purchase failed!");
    }

    public UserIapData getUserIapData() {
        return this.userIapData;
    }

    public boolean isMagazineSubsAvailable() {
        return magazineSubsAvailable;
    }

    public void setMagazineSubsAvailable(final boolean magazineSubsAvailable) {
        this.magazineSubsAvailable = magazineSubsAvailable;
    }

    /**
     * Disable all magezine subscriptions on UI
     */
    public void disableAllPurchases() {
        this.setMagazineSubsAvailable(false);
        refreshMagazineSubsAvailability();
    }
    
    /**
     * Reload the magazine subscription availability
     */
    public void refreshMagazineSubsAvailability() {
        final boolean available = magazineSubsAvailable && userIapData!=null;
        mainActivity.setMagazineSubsAvail(available,
                                          userIapData != null && !userIapData.isSubsActiveCurrently());
    }

    /**
     * Gracefully close the database when the main activity's onStop and
     * onDestroy
     * 
     */
    public void deactivate() {
        dataSource.close();

    }

    /**
     * Connect to the database when main activity's onStart and onResume
     */
    public void activate() {
        dataSource.open();

    }

    /**
     * Reload the subscription history from database
     */
    public void reloadSubscriptionStatus() {
        final List<SubscriptionRecord> subsRecords = dataSource.getSubscriptionRecords(userIapData.getAmazonUserId());
        userIapData.setSubscriptionRecords(subsRecords);
        userIapData.reloadSubscriptionStatus();
        refreshMagazineSubsAvailability();
    }

    /**
     * 
     * This sample app includes a simple SQLite implementation for save
     * subscription purchase detail locally.
     * 
     * We strongly recommend that you save the purchase information on a server.
     * 
     * 
     * 
     * @param receipt
     * @param userId
     */
    private void saveSubscriptionRecord(final Receipt receipt, final String userId) {
        // TODO replace with your own implementation

        dataSource
            .insertOrUpdateSubscriptionRecord(receipt.getReceiptId(),
                                              userId,
                                              receipt.getPurchaseDate().getTime(),
                                              receipt.getCancelDate() == null ? SubscriptionRecord.TO_DATE_NOT_SET
                                                      : receipt.getCancelDate().getTime(),
                                              receipt.getSku());

    }

    /**
     * We strongly recommend verifying the receipt on your own server side
     * first. The server side verification ideally should include checking with
     * Amazon RVS (Receipt Verification Service) to verify the receipt details.
     * 
     * @see <a href=
     *      "https://developer.amazon.com/appsandservices/apis/earn/in-app-purchasing/docs/rvs"
     *      >Appstore's Receipt Verification Service</a>
     * 
     * @param receiptId
     * @return
     */
    private boolean verifyReceiptFromYourService(final String receiptId, final UserData userData) {
        // TODO Add your own server side accessing and verification code
        return true;
    }

    /**
     * Private method to revoke a subscription purchase from the customer
     * 
     * Please implement your application-specific logic to handle the revocation
     * of a subscription purchase.
     * 
     * 
     * @param receipt
     * @param userId
     */
    private void revokeSubscription(final Receipt receipt, final String userId) {
        final String receiptId = receipt.getReceiptId();
        dataSource.cancelSubscription(receiptId, receipt.getCancelDate().getTime());

    }

}
