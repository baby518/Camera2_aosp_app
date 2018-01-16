package com.plus.camera.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.android.camera.debug.Log;

import java.util.ArrayList;
import java.util.Iterator;

public class CameraUtil extends com.android.camera.util.CameraUtil {
    public static final String GOOGLE_PHOTO_PACKAGE_NAME = "com.google.android.apps.photos";
    public static final String KEY_GOOGLE_PHOTO_SECURE_MODE = "com.google.android.apps.photos.api.secure_mode";
    public static final String KEY_GOOGLE_PHOTO_SECURE_MODE_IDS = "com.google.android.apps.photos.api.secure_mode_ids";
    public static final String KEY_GOOGLE_PHOTOS_EXIT_ON_SWIPE = "exit_on_swipe";

    private static boolean sHasGooglePhotosApp = false;

    private CameraUtil(Context context) {
        super(context);
    }

    public static void initialize(Context context) {
        sHasGooglePhotosApp = isPackageExist(context, GOOGLE_PHOTO_PACKAGE_NAME);
    }

    public static boolean hasGooglePhotosApp() {
        return sHasGooglePhotosApp;
    }

    public static boolean isPackageExist(Context context, String packageName) {
        if (context == null || packageName == null || "".equals(packageName)) {
            return false;
        }
        try {
            PackageManager manager = context.getPackageManager();
            // get target package info
            manager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            // get target package state, return false if disabled.
            int state = manager.getApplicationEnabledSetting(packageName);
            return state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    && state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static String getMimeType(Context context, Uri uri) {
        ContentResolver cr = context.getContentResolver();
        return cr.getType(uri);
    }

    public static void viewUri(Uri uri, Context context) {
        if (!isUriValid(uri, context.getContentResolver())) {
            Log.w(TAG, "Uri invalid. uri=" + uri);
            return;
        }

        Intent intent = generateIntent(context);
        String mimeType = getMimeType(context, uri);
        intent.setDataAndType(uri, mimeType);

        startGallery(context, intent);
    }

    public static void viewLimitUris(ArrayList<Uri> uris, Context context) {
        int length = uris.size();
        if (length <= 0) return;
        Intent intent = generateIntent(context);
        String mimeType = getMimeType(context, uris.get(0));
        intent.setDataAndType(uris.get(0), mimeType);
        intent.putExtra(KEY_GOOGLE_PHOTO_SECURE_MODE, true);
        // must use long[] pass ids
        long[] ids = new long[length];
        for (int i = 0; i < length; i++) {
            long id = Integer.valueOf(uris.get(i).getLastPathSegment());
            ids[i] = id;
        }
        intent.putExtra(KEY_GOOGLE_PHOTO_SECURE_MODE_IDS, ids);

        startGallery(context, intent);
    }

    private static Intent generateIntent(Context context) {
        Intent intent = new Intent(sHasGooglePhotosApp ?
                com.android.camera.util.CameraUtil.REVIEW_ACTION : Intent.ACTION_VIEW);
        // Calling startActivity() from outside of an Activity context requires
        // the FLAG_ACTIVITY_NEW_TASK flag.
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);

        if (sHasGooglePhotosApp) {
            intent.setPackage(GOOGLE_PHOTO_PACKAGE_NAME);
        }
        // back to camera when swipe out from first photo when set true.
        intent.putExtra(KEY_GOOGLE_PHOTOS_EXIT_ON_SWIPE, false);

        return intent;
    }

    private static void startGallery(Context context, Intent intent) {
        // use system gallery to view uri, if start failed, show system choose list.
        if (!startActivity(context, intent)) {
            intent.setPackage(null);
            intent.setComponent(null);
            startActivity(context, intent);
        }
    }

    public static boolean startActivity(Context context, Intent intent) {
        if (intent == null) return false;
        if (context.getPackageManager().resolveActivity(intent,
                PackageManager.MATCH_DEFAULT_ONLY) != null) {
            context.startActivity(intent);
            return true;
        } else {
            Log.w(TAG, "has no application can view this uri " + intent.getData());
            return false;
        }
    }

    public static void filterInvalidUris(ContentResolver contentResolver, ArrayList<Uri> uris) {
        if (uris == null) return;
        Iterator<Uri> it = uris.iterator();
        while (it.hasNext()) {
            Uri uri = it.next();
            if (!CameraUtil.isUriValid(uri, contentResolver)) {
                it.remove();
            }
        }
    }
}
