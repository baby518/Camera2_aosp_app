package com.plus.camera.settings;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

import com.android.camera.app.CameraServicesImpl;
import com.android.camera.settings.SettingsManager;

public class ManagedSwitchPreference extends SwitchPreference {
    public ManagedSwitchPreference(Context context) {
        super(context);
    }

    public ManagedSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ManagedSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean getPersistedBoolean(boolean defaultReturnValue) {
        SettingsManager settingsManager = CameraServicesImpl.instance().getSettingsManager();
        if (settingsManager != null) {
            return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, getKey());
        } else {
            // If the SettingsManager is for some reason not initialized,
            // perhaps triggered by a monkey, return default value.
            return defaultReturnValue;
        }
    }

    @Override
    public boolean persistBoolean(boolean value) {
        SettingsManager settingsManager = CameraServicesImpl.instance().getSettingsManager();
        if (settingsManager != null) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, getKey(), value);
            return true;
        } else {
            // If the SettingsManager is for some reason not initialized,
            // perhaps triggered by a monkey, return false to note the value
            // was not persisted.
            return false;
        }
    }
}
