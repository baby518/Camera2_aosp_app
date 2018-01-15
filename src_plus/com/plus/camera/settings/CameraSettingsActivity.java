package com.plus.camera.settings;

import com.android.camera.FatalErrorHandler;
import com.android.camera.FatalErrorHandlerImpl;
import com.android.camera.device.CameraId;
import com.android.camera.one.OneCamera.Facing;
import com.android.camera.one.OneCameraAccessException;
import com.android.camera.one.OneCameraException;
import com.android.camera.one.OneCameraManager;
import com.android.camera.one.OneCameraModule;
import com.android.camera2.R;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;

/**
 * Provides the settings UI for the Camera app.
 */
public class CameraSettingsActivity extends FragmentActivity {

    /**
     * Used to denote a subsection of the preference tree to display in the
     * Fragment. For instance, if 'Advanced' key is provided, the advanced
     * preference section will be treated as the root for display. This is used
     * to enable activity transitions between preference sections, and allows
     * back/up stack to operate correctly.
     */
    public static final String PREF_SCREEN_EXTRA = "pref_screen_extra";
    public static final String HIDE_ADVANCED_SCREEN = "hide_advanced";
    private OneCameraManager mOneCameraManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FatalErrorHandler fatalErrorHandler = new FatalErrorHandlerImpl(this);
        boolean hideAdvancedScreen = false;

        try {
            mOneCameraManager = OneCameraModule.provideOneCameraManager();
        } catch (OneCameraException e) {
            // Log error and continue. Modules requiring OneCamera should check
            // and handle if null by showing error dialog or other treatment.
            fatalErrorHandler.onGenericCameraAccessFailure();
        }

        // Check if manual exposure is available, so we can decide whether to
        // display Advanced screen.
        try {
            CameraId frontCameraId = mOneCameraManager.findFirstCameraFacing(Facing.FRONT);
            CameraId backCameraId = mOneCameraManager.findFirstCameraFacing(Facing.BACK);

            // The exposure compensation is supported when both of the following conditions meet
            //   - we have the valid camera, and
            //   - the valid camera supports the exposure compensation
            boolean isExposureCompensationSupportedByFrontCamera = (frontCameraId != null) &&
                    (mOneCameraManager.getOneCameraCharacteristics(frontCameraId)
                            .isExposureCompensationSupported());
            boolean isExposureCompensationSupportedByBackCamera = (backCameraId != null) &&
                    (mOneCameraManager.getOneCameraCharacteristics(backCameraId)
                            .isExposureCompensationSupported());

            // Hides the option if neither front and back camera support exposure compensation.
            if (!isExposureCompensationSupportedByFrontCamera &&
                    !isExposureCompensationSupportedByBackCamera) {
                hideAdvancedScreen = true;
            }
        } catch (OneCameraAccessException e) {
            fatalErrorHandler.onGenericCameraAccessFailure();
        }

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.mode_settings);

        String prefKey = getIntent().getStringExtra(PREF_SCREEN_EXTRA);
        CameraSettingsFragment dialog = new CameraSettingsFragment();
        Bundle bundle = new Bundle(1);
        bundle.putString(PREF_SCREEN_EXTRA, prefKey);
        bundle.putBoolean(HIDE_ADVANCED_SCREEN, hideAdvancedScreen);
        dialog.setArguments(bundle);
        getFragmentManager().beginTransaction().replace(android.R.id.content, dialog).commit();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        return true;
    }
}
