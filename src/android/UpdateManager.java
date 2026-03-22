package com.apk.update;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpdateManager {

    // 🔴 GitHub latest release API
    private static final String GITHUB_API =
            "https://api.github.com/repos/abdulsalam18/rectangle-max-apk/releases/latest";

    // 🔴 APK file name in GitHub release
    private static final String APK_NAME = "rectangle-max.apk";

    private static final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private static final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    /* ================= PUBLIC ================= */

    public static void check(Activity activity) {
        executor.execute(() -> checkUpdate(activity));
    }

    /* ================= CHECK UPDATE ================= */

    private static void checkUpdate(Activity activity) {
        try {
            URL url = new URL(GITHUB_API);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.connect();

            InputStream is = c.getInputStream();
            StringBuilder sb = new StringBuilder();
            int ch;
            while ((ch = is.read()) != -1) sb.append((char) ch);
            is.close();

            JSONObject json = new JSONObject(sb.toString());

            String tag = json.getString("tag_name");
            int remoteVersion = parseVersion(tag);

            PackageInfo p = activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0);

            long currentVersion = p.getLongVersionCode();

            if (remoteVersion > currentVersion) {

                JSONArray assets = json.getJSONArray("assets");
                String apkUrl = assets.getJSONObject(0)
                        .getString("browser_download_url");

                mainHandler.post(() ->
                        showForceDialog(activity, apkUrl)
                );
            }

        } catch (Exception ignored) {
        }
    }

    /* ================= FORCE UPDATE DIALOG ================= */

    private static void showForceDialog(Activity a, String url) {
        new AlertDialog.Builder(a)
                .setTitle("Update Required")
                .setMessage("Please update the app to continue.")
                .setCancelable(false)
                .setPositiveButton(
                        "Update",
                        (d, w) -> downloadAndInstall(a, url)
                )
                .show();
    }

    /* ================= DOWNLOAD & INSTALL ================= */

    private static void downloadAndInstall(Activity a, String link) {

        executor.execute(() -> {
            try {
                URL url = new URL(link);
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.connect();

                File apkFile = new File(
                        a.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                        APK_NAME
                );

                InputStream is = c.getInputStream();
                FileOutputStream fos = new FileOutputStream(apkFile);

                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                is.close();

                mainHandler.post(() -> installApk(a, apkFile));

            } catch (Exception ignored) {
            }
        });
    }

    private static void installApk(Activity a, File file) {

        Uri uri = FileProvider.getUriForFile(
                a,
                a.getPackageName() + ".provider",
                file
        );

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(
                uri,
                "application/vnd.android.package-archive"
        );
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        a.startActivity(intent);
    }

    /* ================= VERSION PARSE ================= */

    // v1.2.3 → 123
    private static int parseVersion(String v) {
        v = v.replace("v", "");
        String[] p = v.split("\\.");
        return Integer.parseInt(p[0]) * 100
                + Integer.parseInt(p[1]) * 10
                + Integer.parseInt(p[2]);
    }
}
