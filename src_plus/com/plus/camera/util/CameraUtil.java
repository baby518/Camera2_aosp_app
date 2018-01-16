package com.plus.camera.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.android.camera.debug.Log;

public class CameraUtil extends com.android.camera.util.CameraUtil {
    private CameraUtil(Context context) {
        super(context);
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
            return state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
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

    private static Intent generateIntent(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        // Calling startActivity() from outside of an Activity context requires
        // the FLAG_ACTIVITY_NEW_TASK flag.
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);

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
}
