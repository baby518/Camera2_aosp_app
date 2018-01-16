package com.android.camera.config;

import com.android.camera2.BuildConfig;

public class AppConfig {
    public static boolean isCaptureModuleSupported() {
        return BuildConfig.USE_CAPTURE_MODULE;
    }
}
