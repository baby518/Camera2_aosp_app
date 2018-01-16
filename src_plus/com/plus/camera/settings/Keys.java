package com.plus.camera.settings;

import android.content.Context;

import com.android.camera.settings.SettingsManager;

/**
 * Keys is a class for storing SharedPreferences keys and configuring
 * their defaults.
 *
 * For each key that has a default value and set of possible values, it
 * stores those defaults so they can be used by the SettingsManager
 * on lookup.  This step is optional, and it can be done anytime before
 * a setting is accessed by the SettingsManager API.
 */
public class Keys extends com.android.camera.settings.Keys {
    /**
     * Set some number of defaults for the defined keys.
     * It's not necessary to set all defaults.
     */
    public static void setDefaults(SettingsManager settingsManager, Context context) {
        com.android.camera.settings.Keys.setDefaults(settingsManager, context);
        settingsManager.setDefaults(KEY_EXPOSURE_COMPENSATION_ENABLED, true);
    }
}

