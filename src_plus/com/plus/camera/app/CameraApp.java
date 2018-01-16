package com.plus.camera.app;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Debug;

import com.android.camera.stats.UsageStatistics;
import com.android.camera.stats.profiler.Profile;
import com.android.camera.stats.profiler.Profilers;
import com.android.camera.util.AndroidContext;
import com.android.camera.util.AndroidServices;
import com.plus.camera.util.CameraUtil;


/**
 * The Camera application class containing important services and functionality
 * to be used across modules.
 */
public class CameraApp extends Application {
    /**
     * This is for debugging only: If set to true, application will not start
     * until a debugger is attached.
     * <p>
     * Use this if you need to debug code that is executed while the app starts
     * up and it would be too late to attach a debugger afterwards.
     */
    private static final boolean WAIT_FOR_DEBUGGER_ON_START = false;

    @Override
    public void onCreate() {
        super.onCreate();

        if (WAIT_FOR_DEBUGGER_ON_START) {
            Debug.waitForDebugger();
        }

        // Android context must be the first item initialized.
        Context context = getApplicationContext();
        AndroidContext.initialize(context);

        CameraUtil.initialize(context);

        // This will measure and write to the exception handler if
        // the time between any two calls or the total time from
        // start to stop is over 10ms.
        Profile guard = Profilers.instance().guard("CameraApp onCreate()");

        UsageStatistics.instance().initialize(this);
        guard.mark("UsageStatistics.initialize");

        clearNotifications();
        guard.stop("clearNotifications");
    }

    /**
     * Clears all notifications. This cleans up notifications that we might have
     * created earlier but remained after a crash.
     */
    private void clearNotifications() {
        NotificationManager manager = AndroidServices.instance().provideNotificationManager();
        if (manager != null) {
            manager.cancelAll();
        }
    }
}

