package com.secrethq.store;

import android.app.AlertDialog;
import android.util.Log;

import com.android.billingclient.api.*;

import org.cocos2dx.lib.Cocos2dxActivity;

import java.lang.ref.WeakReference;
import java.util.List;

public class PTStoreBridge {

    private static final String TAG = "PTStoreBridge";

    private static BillingClient billingClient;
    private static Cocos2dxActivity activity;
    private static WeakReference<Cocos2dxActivity> s_activity;

    public static native void purchaseDidComplete(String productId);
    public static native void purchaseDidCompleteRestoring(String productId);
    public static native boolean isProductConsumible(String productId);

    // ---------------- INIT ----------------
    public static void initBridge(Cocos2dxActivity _activity) {

        activity = _activity;
        s_activity = new WeakReference<>(activity);

        billingClient = BillingClient.newBuilder(activity)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult result) {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing Ready");
                } else {
                    Log.e(TAG, "Billing Failed");
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.e(TAG, "Billing Disconnected");
            }
        });
    }

    // ---------------- PURCHASE LISTENER ----------------
    private static PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {

        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {

            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }

        } else {
            Log.e(TAG, "Purchase failed: " + billingResult.getDebugMessage());
        }
    };

    // ---------------- HANDLE PURCHASE ----------------
    private static void handlePurchase(Purchase purchase) {

        String productId = purchase.getProducts().get(0);

        if (isProductConsumible(productId)) {
            consumePurchase(purchase);
        } else {
            purchaseDidComplete(productId);
        }
    }

    // ---------------- PURCHASE ----------------
    public static void purchase(String productId) {

        QueryProductDetailsParams params =
                QueryProductDetailsParams.newBuilder()
                        .setProductList(
                                List.of(
                                        QueryProductDetailsParams.Product.newBuilder()
                                                .setProductId(productId)
                                                .setProductType(BillingClient.ProductType.INAPP)
                                                .build()
                                )
                        ).build();

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {

            if (productDetailsList == null || productDetailsList.isEmpty()) {
                Log.e(TAG, "Product not found");
                return;
            }

            ProductDetails productDetails = productDetailsList.get(0);

            BillingFlowParams flowParams =
                    BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(
                                    List.of(
                                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                                    .setProductDetails(productDetails)
                                                    .build()
                                    )
                            )
                            .build();

            billingClient.launchBillingFlow(activity, flowParams);
        });
    }

    // ---------------- CONSUME ----------------
    private static void consumePurchase(Purchase purchase) {

        ConsumeParams params =
                ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();

        billingClient.consumeAsync(params, (billingResult, token) -> {

            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                purchaseDidComplete(purchase.getProducts().get(0));
            } else {
                Log.e(TAG, "Consume failed");
            }
        });
    }

    // ---------------- RESTORE ----------------
    public static void restorePurchases() {

        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                (billingResult, purchasesList) -> {

                    for (Purchase purchase : purchasesList) {

                        String productId = purchase.getProducts().get(0);

                        if (!isProductConsumible(productId)) {
                            purchaseDidCompleteRestoring(productId);
                        }
                    }

                    new AlertDialog.Builder(activity)
                            .setMessage("Restore complete")
                            .setPositiveButton("OK", null)
                            .show();
                });
    }
}
