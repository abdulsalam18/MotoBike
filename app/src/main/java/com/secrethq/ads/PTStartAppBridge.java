package com.secrethq.ads;

import java.lang.ref.WeakReference;

import org.cocos2dx.lib.Cocos2dxActivity;

import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import com.abdulsalam.R;

import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;

public class PTStartAppBridge {

    private static final String TAG = "PTStartAppBridge";

    private static Cocos2dxActivity activity;
    private static WeakReference<Cocos2dxActivity> s_activity;

    private static Banner addView;
    private static StartAppAd interstitial;

    private static boolean isBannerScheduledForShow = false;
    private static boolean isInterstitialScheduledForShow = false;

    private static String appId() {
        return activity.getString(R.string.app_id);
    }
    private static native void interstitialDidFail();
    private static native void bannerDidFail();

    // INIT
    public static void initBridge(Cocos2dxActivity act){

        Log.v(TAG, "INIT");

        s_activity = new WeakReference<>(act);
        activity = act;

        StartAppSDK.init(activity, appId(), false);
        StartAppAd.disableSplash();

        initBanner();
        initInterstitial();
    }

    // -------------------------
    // BANNER
    // -------------------------

    public static void initBanner(){

        Log.v(TAG, "initBanner");

        s_activity.get().runOnUiThread(() -> {

            if (banner != null) return;

            FrameLayout frameLayout = (FrameLayout) activity.findViewById(android.R.id.content);

            RelativeLayout layout = new RelativeLayout(activity);
            frameLayout.addView(layout);

            RelativeLayout.LayoutParams params =
                    new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT
                    );

            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.addRule(RelativeLayout.CENTER_HORIZONTAL);

            banner = new Banner(activity);

            layout.addView(banner, params);

            banner.setVisibility(View.INVISIBLE);
        });
    }

    public static boolean isBannerVisible(){

        if (banner == null) return false;

        return banner.getVisibility() == View.VISIBLE;
    }

    public static void showBannerAd(){

        Log.v(TAG, "showBannerAd");

        isBannerScheduledForShow = true;

        if (banner != null){

            s_activity.get().runOnUiThread(() -> {

                banner.setVisibility(View.VISIBLE);

            });
        }
    }

    public static void hideBannerAd(){

        Log.v(TAG, "hideBannerAd");

        isBannerScheduledForShow = false;

        if (banner != null){

            s_activity.get().runOnUiThread(() -> {

                banner.setVisibility(View.INVISIBLE);

            });
        }
    }

    // -------------------------
    // INTERSTITIAL
    // -------------------------

    public static void initInterstitial(){

        Log.v(TAG, "initInterstitial");

        s_activity.get().runOnUiThread(() -> {

            if (interstitial != null) return;

            interstitial = new StartAppAd(activity);

            loadInterstitial();
        });
    }

    private static void loadInterstitial(){

        interstitial.loadAd(new AdEventListener() {

            @Override
            public void onReceiveAd(com.startapp.sdk.adsbase.Ad ad) {

                Log.v(TAG, "Interstitial Loaded");

                if (isInterstitialScheduledForShow){
                    showFullScreen();
                }
            }

            @Override
            public void onFailedToReceiveAd(com.startapp.sdk.adsbase.Ad ad) {

                Log.v(TAG, "Interstitial Failed");

                if (isInterstitialScheduledForShow){
                    interstitialDidFail();
                }
            }
        });
    }

    public static void showFullScreen(){

        Log.v(TAG, "showFullScreen");

        isInterstitialScheduledForShow = true;

        if (interstitial != null){

            s_activity.get().runOnUiThread(() -> {

                if (interstitial.isReady()){

                    interstitial.showAd();

                    isInterstitialScheduledForShow = false;

                    loadInterstitial();

                } else {

                    isInterstitialScheduledForShow = true;
                }
            });
        }
    }
}
