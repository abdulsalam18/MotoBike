package com.secrethq.utils;

import android.app.AlertDialog;
import android.app.UiModeManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.util.Log;

import com.android.MemoryManager;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;

import org.cocos2dx.lib.Cocos2dxActivity;

import java.lang.ref.WeakReference;
import java.security.MessageDigest;

public class PTServicesBridge {

    private static final String TAG = "PTServicesBridge";

    private static native String getLeaderboardId();
    private static native void warningMessageClicked(boolean accepted);

    private static Cocos2dxActivity activity;
    private static WeakReference<Cocos2dxActivity> s_activity;

    private static String urlString;

    public static final int REQUEST_LEADERBOARD = 5000;

    // ---------------- INIT ----------------
    public static void initBridge(Cocos2dxActivity act, String appId) {
        Log.v(TAG, "INIT");

        activity = act;
        s_activity = new WeakReference<>(act);

        // NEW Play Games init
        PlayGamesSdk.initialize(activity);
    }

    // ---------------- SHARE ----------------
    public static void openShareWidget(String message) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, message);
        activity.startActivity(Intent.createChooser(sharingIntent, "Share"));
    }

    // ---------------- URL ----------------
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

    // ---------------- LEADERBOARD ----------------
    public static void showLeaderboard() {
        s_activity.get().runOnUiThread(() -> {

            String leaderboardId = getLeaderboardId();
            if (leaderboardId == null || leaderboardId.isEmpty()) return;

            PlayGames.getLeaderboardsClient(activity)
                    .getLeaderboardIntent(leaderboardId)
                    .addOnSuccessListener(intent ->
                            activity.startActivityForResult(intent, REQUEST_LEADERBOARD))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Leaderboard Error", e));
        });
    }

    // ---------------- SUBMIT SCORE ----------------
    public static void submitScore(int score) {

        String leaderboardId = getLeaderboardId();
        if (leaderboardId == null || leaderboardId.isEmpty()) return;

        PlayGames.getLeaderboardsClient(activity)
                .submitScore(leaderboardId, score);
    }

    // ---------------- LOGIN ----------------
    public static void loginGameServices() {
        // Play Games v2 auto sign-in handled internally
        Log.d(TAG, "Play Games Sign-In handled automatically");
    }

    public static boolean isGameServiceAvailable() {
        return true; // v2 handles internally
    }

    // ---------------- WARNING ----------------
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

    // ---------------- DEVICE CHECK ----------------
    public static boolean isRunningOnTV() {
        UiModeManager uiModeManager =
                (UiModeManager) activity.getSystemService(Context.UI_MODE_SERVICE);

        return uiModeManager.getCurrentModeType()
                == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    // ---------------- SHA1 ----------------
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
}
