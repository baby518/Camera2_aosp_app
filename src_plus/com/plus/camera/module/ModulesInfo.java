package com.plus.camera.module;

import android.content.Context;
import android.content.res.Resources;

import com.android.camera.app.ModuleManager;
import com.android.camera.one.config.OneCameraFeatureConfig;
import com.android.camera.settings.SettingsScopeNamespaces;
import com.android.camera2.R;

public class ModulesInfo extends com.android.camera.module.ModulesInfo {

    public static void setupModules(Context context, ModuleManager moduleManager,
                                    OneCameraFeatureConfig config) {
        Resources res = context.getResources();
        int photoModuleId = context.getResources().getInteger(R.integer.camera_mode_photo);
        registerPhotoModule(moduleManager, photoModuleId, SettingsScopeNamespaces.PHOTO,
                config.isUsingCaptureModule());
        moduleManager.setDefaultModuleIndex(photoModuleId);
        registerVideoModule(moduleManager, res.getInteger(R.integer.camera_mode_video),
                SettingsScopeNamespaces.VIDEO);

        int imageCaptureIntentModuleId = res.getInteger(R.integer.camera_mode_capture_intent);
        registerCaptureIntentModule(moduleManager, imageCaptureIntentModuleId,
                SettingsScopeNamespaces.PHOTO, config.isUsingCaptureModule());
    }
}
