package com.secrethq.ads;

import java.lang.ref.WeakReference;

import org.cocos2dx.lib.Cocos2dxActivity;

import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;

public class PTStartAppBridge {

    private static final String TAG = "PTStartAppBridge";

    private static Cocos2dxActivity activity;
    private static WeakReference<Cocos2dxActivity> s_activity;

    private static Banner banner;
    private static StartAppAd interstitial;

    private static boolean isInterstitialReady = false;

    // =========================
    // INIT
    // =========================
    public static void initBridge(Cocos2dxActivity act){

        Log.v(TAG, "INIT");

        activity = act;
        s_activity = new WeakReference<>(act);

        // 🔥 Init SDK
        StartAppSDK.init(activity, activity.getString(
                activity.getResources().getIdentifier("app_id","string",activity.getPackageName())
        ), false);

        // 🔥 Enable test ads (remove later)
        StartAppSDK.setTestAdsEnabled(true);

        // Disable splash
        StartAppAd.disableSplash();

        initBanner();
        initInterstitial();
    }

    // =========================
    // BANNER
    // =========================
    public static void initBanner(){

        Log.v(TAG, "initBanner");

        s_activity.get().runOnUiThread(() -> {

            if (banner != null) return;

            FrameLayout root = (FrameLayout) activity.findViewById(android.R.id.content);

            banner = new Banner(activity);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;

            root.addView(banner, params);

            banner.loadAd(); // 🔥 IMPORTANT

            banner.setVisibility(android.view.View.GONE);
        });
    }

    public static void showBannerAd(){

        Log.v(TAG, "showBannerAd");

        if (banner != null){
            s_activity.get().runOnUiThread(() -> banner.setVisibility(android.view.View.VISIBLE));
        }
    }

    public static void hideBannerAd(){

        Log.v(TAG, "hideBannerAd");

        if (banner != null){
            s_activity.get().runOnUiThread(() -> banner.setVisibility(android.view.View.GONE));
        }
    }

    // =========================
    // INTERSTITIAL
    // =========================
    public static void initInterstitial(){

        Log.v(TAG, "initInterstitial");

        interstitial = new StartAppAd(activity);

        loadInterstitial();
    }

    private static void loadInterstitial(){

        interstitial.loadAd(new AdEventListener() {

            @Override
            public void onReceiveAd(com.startapp.sdk.adsbase.Ad ad) {
                Log.v(TAG, "Interstitial Loaded");
                isInterstitialReady = true;
            }

            @Override
            public void onFailedToReceiveAd(com.startapp.sdk.adsbase.Ad ad) {
                Log.v(TAG, "Interstitial Failed");
                isInterstitialReady = false;
            }
        });
    }

    public static void showFullScreen(){

        Log.v(TAG, "showFullScreen");

        if (interstitial == null) return;

        s_activity.get().runOnUiThread(() -> {

            if (isInterstitialReady){

                interstitial.showAd();
                isInterstitialReady = false;

                loadInterstitial(); // reload next ad

            } else {
                Log.v(TAG, "Interstitial not ready yet");
            }

        });
    }
}
