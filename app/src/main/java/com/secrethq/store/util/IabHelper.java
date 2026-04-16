package com.secrethq.store.util;

import android.app.Activity;
import android.util.Log;

import com.android.billingclient.api.*;

import java.util.ArrayList;
import java.util.List;

public class IabHelper {

    private static final String TAG = "IabHelper_NEW";

    private BillingClient billingClient;
    private Activity activity;

    private boolean isReady = false;

    // Listener
    public interface OnIabPurchaseFinishedListener {
        void onIabPurchaseFinished(boolean success, String productId);
    }

    private OnIabPurchaseFinishedListener purchaseListener;

    public IabHelper(Activity activity, String ignoredPublicKey) {
        this.activity = activity;

        billingClient = BillingClient.newBuilder(activity)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();
    }

    // ---------------- INIT ----------------
    public void startSetup(final OnIabSetupFinishedListener listener) {

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {

                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    isReady = true;
                    Log.d(TAG, "Billing Connected");
                    if (listener != null) listener.onIabSetupFinished(true);
                } else {
                    isReady = false;
                    if (listener != null) listener.onIabSetupFinished(false);
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                isReady = false;
                Log.d(TAG, "Billing Disconnected");
            }
        });
    }

    public interface OnIabSetupFinishedListener {
        void onIabSetupFinished(boolean success);
    }

    // ---------------- PURCHASE ----------------
    public void launchPurchaseFlow(String productId, OnIabPurchaseFinishedListener listener) {

        this.purchaseListener = listener;

        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();

        productList.add(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
        );

        QueryProductDetailsParams params =
                QueryProductDetailsParams.newBuilder()
                        .setProductList(productList)
                        .build();

        billingClient.queryProductDetailsAsync(params,
                (billingResult, productDetailsList) -> {

                    if (productDetailsList.isEmpty()) {
                        if (purchaseListener != null)
                            purchaseListener.onIabPurchaseFinished(false, productId);
                        return;
                    }

                    ProductDetails productDetails = productDetailsList.get(0);

                    List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                            new ArrayList<>();

                    productDetailsParamsList.add(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(productDetails)
                                    .build()
                    );

                    BillingFlowParams billingFlowParams =
                            BillingFlowParams.newBuilder()
                                    .setProductDetailsParamsList(productDetailsParamsList)
                                    .build();

                    billingClient.launchBillingFlow(activity, billingFlowParams);
                });
    }

    // ---------------- PURCHASE RESULT ----------------
    private final PurchasesUpdatedListener purchasesUpdatedListener =
            (billingResult, purchases) -> {

                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                        && purchases != null) {

                    for (Purchase purchase : purchases) {

                        Log.d(TAG, "Purchased: " + purchase.getProducts());

                        if (purchaseListener != null) {
                            purchaseListener.onIabPurchaseFinished(true,
                                    purchase.getProducts().get(0));
                        }
                    }

                } else if (purchaseListener != null) {
                    purchaseListener.onIabPurchaseFinished(false, null);
                }
            };

    // ---------------- DISPOSE ----------------
    public void dispose() {
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }
}
