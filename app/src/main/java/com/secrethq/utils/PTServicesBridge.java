package com.secrethq.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.app.UiModeManager;

import com.android.MemoryManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;

import org.cocos2dx.lib.Cocos2dxActivity;

import java.io.File;
import java.io.FileFilter;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.util.regex.Pattern;

public class PTServicesBridge
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static PTServicesBridge sInstance;
    private static final String TAG = "PTServicesBridge";

    private static native String getLeaderboardId();
    private static native void warningMessageClicked(boolean accepted);

    private static Cocos2dxActivity activity;
    private static WeakReference<Cocos2dxActivity> s_activity;

    private static GoogleApiClient mGoogleApiClient;

    private static String urlString;
    private static int scoreValue;

    public static final int RC_SIGN_IN = 9001;
    private static final int REQUEST_LEADERBOARD = 5000;

    // NEW: Google Sign-In client
    private static GoogleSignInClient googleSignInClient;

    public static PTServicesBridge instance() {
        if (sInstance == null)
            sInstance = new PTServicesBridge();
        return sInstance;
    }

    public static void initBridge(Cocos2dxActivity activity, String appId) {
        Log.v(TAG, "PTServicesBridge INIT");

        PTServicesBridge.s_activity = new WeakReference<>(activity);
        PTServicesBridge.activity = activity;

        if (appId == null || appId.length() == 0) {
            return;
        }

        // Google Play Games API
        PTServicesBridge.mGoogleApiClient = new GoogleApiClient.Builder(activity)
                .addApi(Games.API)
                .addScope(Games.SCOPE_GAMES)
                .addConnectionCallbacks(instance())
                .addOnConnectionFailedListener(instance())
                .build();

        // NEW Google Sign-In (replacement for Plus API)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                .build();

        googleSignInClient = GoogleSignIn.getClient(activity, gso);
    }

    public static void openShareWidget(String message) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, message);
        activity.startActivity(Intent.createChooser(sharingIntent, "Share"));
    }

    public static void openUrl(String url) {
        urlString = url;

        s_activity.get().runOnUiThread(() -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
                activity.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "OpenURL Failed", e);
            }
        });
    }

    public static void showLeaderboard() {
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
            Log.e(TAG, "Google Play Services not connected");
            return;
        }

        s_activity.get().runOnUiThread(() -> {
            String leaderboardId = getLeaderboardId();
            if (leaderboardId == null || leaderboardId.isEmpty()) return;

            activity.startActivityForResult(
                    Games.Leaderboards.getLeaderboardIntent(mGoogleApiClient, leaderboardId),
                    REQUEST_LEADERBOARD
            );
        });
    }

    public static void submitScrore(int score) {
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) return;

        String leaderboardId = getLeaderboardId();
        if (leaderboardId == null || leaderboardId.isEmpty()) return;

        Games.Leaderboards.submitScore(mGoogleApiClient, leaderboardId, score);
    }

    public static void loginGameServices() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    public static boolean isGameServiceAvialable() {
        return (mGoogleApiClient != null && mGoogleApiClient.isConnected());
    }

    public static void showWarningMessage(String message) {
        s_activity.get().runOnUiThread(() -> {
            AlertDialog.Builder dlg = new AlertDialog.Builder(activity);

            dlg.setMessage(message);

            dlg.setNegativeButton("Cancel", (dialog, which) -> {
                warningMessageClicked(false);
                MemoryManager.manageMemory();
            });

            dlg.setPositiveButton("OK", (dialog, which) -> {
                warningMessageClicked(true);
                MemoryManager.manageMemory();
            });

            dlg.setCancelable(true);
            dlg.show();
        });
    }

    public static boolean isRunningOnTV() {
        UiModeManager uiModeManager =
                (UiModeManager) activity.getSystemService(Context.UI_MODE_SERVICE);

        return uiModeManager.getCurrentModeType()
                == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    public static String sha1(byte[] data, int length) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(data, 0, length);
        byte[] digest = md.digest();

        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.v(TAG, "Google API Connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.v(TAG, "Connection Suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v(TAG, "Connection Failed: " + connectionResult);

        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(activity, RC_SIGN_IN);
            } catch (SendIntentException e) {
                mGoogleApiClient.connect();
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN && resultCode == -1) {
            mGoogleApiClient.connect();
        }
    }
}
