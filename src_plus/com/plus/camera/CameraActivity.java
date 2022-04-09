package com.plus.camera;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.CameraPerformanceTracker;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ShareActionProvider;

import com.android.camera.FatalErrorHandlerImpl;
import com.android.camera.SoundPlayer;
import com.android.camera.app.CameraController;
import com.android.camera.app.LocationManager;
import com.android.camera.app.MemoryQuery;
import com.android.camera.app.ModuleManagerImpl;
import com.android.camera.app.OrientationManagerImpl;
import com.android.camera.config.AppConfig;
import com.android.camera.data.FilmstripContentObserver;
import com.android.camera.data.FilmstripItem;
import com.android.camera.data.FilmstripItemUtils;
import com.android.camera.data.GlideFilmstripManager;
import com.android.camera.data.PhotoDataFactory;
import com.android.camera.data.PhotoItem;
import com.android.camera.data.PhotoItemFactory;
import com.android.camera.data.VideoDataFactory;
import com.android.camera.data.VideoItem;
import com.android.camera.data.VideoItemFactory;
import com.android.camera.debug.Log;
import com.android.camera.device.ActiveCameraDeviceTracker;
import com.android.camera.one.OneCameraException;
import com.android.camera.one.OneCameraModule;
import com.android.camera.one.config.OneCameraFeatureConfigCreator;
import com.android.camera.session.CaptureSessionManager;
import com.android.camera.settings.AppUpgrader;
import com.android.camera.settings.PictureSizeLoader;
import com.android.camera.settings.ResolutionSetting;
import com.android.camera.settings.ResolutionUtil;
import com.android.camera.settings.SettingsManager;
import com.android.camera.stats.UsageStatistics;
import com.android.camera.stats.profiler.Profile;
import com.android.camera.ui.MainActivityLayout;
import com.android.camera.ui.ModeListView;
import com.android.camera.ui.ModeListView.ModeListVisibilityChangedListener;
import com.android.camera.util.ApiHelper;
import com.android.camera.util.Callback;
import com.android.camera.util.IntentHelper;
import com.android.camera.util.ReleaseHelper;
import com.android.camera.widget.FilmstripView;
import com.android.camera2.R;

import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraAgentFactory;
import com.android.ex.camera2.portability.CameraExceptionHandler;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.MemoryCategory;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.executor.FifoPriorityThreadPoolExecutor;

import com.google.common.logging.eventprotos;
import com.google.common.logging.eventprotos.ForegroundEvent.ForegroundSource;
import com.google.common.logging.eventprotos.NavigationChange;

import com.plus.camera.app.CameraAppUI;
import com.plus.camera.module.ModulesInfo;
import com.plus.camera.settings.CameraSettingsActivity;
import com.plus.camera.settings.Keys;
import com.plus.camera.util.CameraUtil;

import java.util.ArrayList;
import java.util.HashMap;

public class CameraActivity extends com.android.camera.CameraActivity implements com.plus.camera.app.AppController {
    private static final Log.Tag TAG = new Log.Tag("CameraActivity");
    private Uri mThumbnailUri;
    private final Object mThumbnailLock = new Object();
    private FilmstripManager mFilmstripManager;
    private boolean mFilmstripUiVisibility = false;

    /** store uris which is taken in secure camera */
    private ArrayList<Uri> mSecureUriList = new ArrayList<>();

    @Override
    public void onCameraOpened(CameraAgent.CameraProxy camera) {
        super.onCameraOpened(camera);
    }

    @Override
    public void onCameraDisabled(int cameraId) {
        super.onCameraDisabled(cameraId);
    }

    @Override
    public void onDeviceOpenFailure(int cameraId, String info) {
        super.onDeviceOpenFailure(cameraId, info);
    }

    @Override
    public void onDeviceOpenedAlready(int cameraId, String info) {
        super.onDeviceOpenedAlready(cameraId, info);
    }

    @Override
    public void onReconnectionFailure(CameraAgent mgr, String info) {
        super.onReconnectionFailure(mgr, info);
    }

    @Override
    public void onCreateTasks(Bundle state) {
        Profile profile = mProfiler.create("CameraActivity.onCreateTasks").start();
        CameraPerformanceTracker.onEvent(CameraPerformanceTracker.ACTIVITY_START);
        mOnCreateTime = System.currentTimeMillis();
        mAppContext = getApplicationContext();
        mMainHandler = new MainHandler(this, getMainLooper());
        mLocationManager = new LocationManager(mAppContext, shouldUseNoOpLocation());
        mOrientationManager = new OrientationManagerImpl(this, mMainHandler);
        mSettingsManager = getServices().getSettingsManager();
        mSoundPlayer = new SoundPlayer(mAppContext);
        mFeatureConfig = OneCameraFeatureConfigCreator.createDefault(getContentResolver(),
                getServices().getMemoryManager());
        mFatalErrorHandler = new FatalErrorHandlerImpl(this);
        checkPermissions();
        if (!mHasCriticalPermissions) {
            Log.v(TAG, "onCreate: Missing critical permissions.");
            finish();
            return;
        }
        profile.mark();

        checkStartIntent();

        mFilmstripManager = new FilmstripManager();
        if (isFilmstripSupported()) {
            mFilmstripManager.init();
            profile.mark("Glide.setup");
        }

        mActiveCameraDeviceTracker = ActiveCameraDeviceTracker.instance();
        try {
            mOneCameraOpener = OneCameraModule.provideOneCameraOpener(
                    mFeatureConfig,
                    mAppContext,
                    mActiveCameraDeviceTracker,
                    ResolutionUtil.getDisplayMetrics(this));
            mOneCameraManager = OneCameraModule.provideOneCameraManager();
        } catch (OneCameraException e) {
            // Log error and continue start process while showing error dialog..
            Log.e(TAG, "Creating camera manager failed.", e);
            mFatalErrorHandler.onGenericCameraAccessFailure();
        }
        profile.mark("OneCameraManager.get");

        try {
            mCameraController = new CameraController(mAppContext, this, mMainHandler,
                    CameraAgentFactory.getAndroidCameraAgent(mAppContext,
                            CameraAgentFactory.CameraApi.API_1),
                    CameraAgentFactory.getAndroidCameraAgent(mAppContext,
                            CameraAgentFactory.CameraApi.AUTO),
                    mActiveCameraDeviceTracker);
            mCameraController.setCameraExceptionHandler(
                    new CameraExceptionHandler(mCameraExceptionCallback, mMainHandler));
        } catch (AssertionError e) {
            Log.e(TAG, "Creating camera controller failed.", e);
            mFatalErrorHandler.onGenericCameraAccessFailure();
        }

        // TODO: Try to move all the resources allocation to happen as soon as
        // possible so we can call module.init() at the earliest time.
        mModuleManager = new ModuleManagerImpl();

        ModulesInfo.setupModules(mAppContext, mModuleManager, mFeatureConfig);

        AppUpgrader appUpgrader = new AppUpgrader(this);
        appUpgrader.upgrade(mSettingsManager);

        // Make sure the picture sizes are correctly cached for the current OS
        // version.
        profile.mark();
        try {
            (new PictureSizeLoader(mAppContext)).computePictureSizes();
        } catch (AssertionError e) {
            Log.e(TAG, "Creating camera controller failed.", e);
            mFatalErrorHandler.onGenericCameraAccessFailure();
        }
        profile.mark("computePictureSizes");
        Keys.setDefaults(mSettingsManager, mAppContext);

        mResolutionSetting = new ResolutionSetting(mSettingsManager, mOneCameraManager,
                getContentResolver());

        // We suppress this flag via theme when drawing the system preview
        // background, but once we create activity here, reactivate to the
        // default value. The default is important for L, we don't want to
        // change app behavior, just starting background drawable layout.
        if (ApiHelper.isLOrHigher()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }
        profile.mark();
        if (isFilmstripSupported()) {
            getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
            setContentView(R.layout.activity_main_plus);
        } else {
            setContentView(R.layout.activity_main_plus_without_filmstrip);
        }
        profile.mark("setContentView()");

        // A window background is set in styles.xml for the system to show a
        // drawable background with gray color and camera icon before the
        // activity is created. We set the background to null here to prevent
        // overdraw, all views must take care of drawing backgrounds if
        // necessary. This call to setBackgroundDrawable must occur after
        // setContentView, otherwise a background may be set again from the
        // style.
        getWindow().setBackgroundDrawable(null);

        if (isFilmstripSupported()) {
            mFilmstripManager.setActionBar();
        }

        mModeListView = (ModeListView) findViewById(R.id.mode_list_layout);
        mModeListView.init(mModuleManager.getSupportedModeIndexList());
        if (ApiHelper.HAS_ROTATION_ANIMATION) {
            setRotationAnimation();
        }
        mModeListView.setVisibilityChangedListener(new ModeListVisibilityChangedListener() {
            @Override
            public void onVisibilityChanged(boolean visible) {
                mModeListVisible = visible;
                mCameraAppUI.setShutterButtonImportantToA11y(!visible);
                updatePreviewVisibility();
            }
        });

        if (mSecureCamera) {
            // Change the window flags so that secure camera can show when
            // locked
            Window win = getWindow();
            WindowManager.LayoutParams params = win.getAttributes();
            params.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
            win.setAttributes(params);

            // Filter for screen off so that we can finish secure camera
            // activity when screen is off.
            IntentFilter filter_screen_off = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            registerReceiver(mShutdownReceiver, filter_screen_off);

            // Filter for phone unlock so that we can finish secure camera
            // via this UI path:
            //    1. from secure lock screen, user starts secure camera
            //    2. user presses home button
            //    3. user unlocks phone
            IntentFilter filter_user_unlock = new IntentFilter(Intent.ACTION_USER_PRESENT);
            registerReceiver(mShutdownReceiver, filter_user_unlock);
        }
        mCameraAppUI = new CameraAppUI(this,
                (MainActivityLayout) findViewById(R.id.activity_root_view), isCaptureIntent());

        if (isFilmstripSupported()) {
            mFilmstripManager.initController();
        }

        if (AppConfig.isCaptureModuleSupported()) {
            // Add the session listener so we can track the session progress updates.
            getServices().getCaptureSessionManager().addSessionListener(mSessionListener);
        }

        profile.mark("Configure Camera UI");

        setModuleFromModeIndex(getModeIndex());

        profile.mark();
        mCameraAppUI.prepareModuleUI();
        profile.mark("Init Current Module UI");
        mCurrentModule.init(this, isSecureCamera(), isCaptureIntent());
        profile.mark("Init CurrentModule");

        if (isFilmstripSupported()) {
            mFilmstripManager.initItems();
        }

        mMemoryManager = getServices().getMemoryManager();

        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                HashMap memoryData = mMemoryManager.queryMemory();
                UsageStatistics.instance().reportMemoryConsumed(memoryData,
                        MemoryQuery.REPORT_LABEL_LAUNCH);
            }
        });

        mMotionManager = getServices().getMotionManager();

        profile.stop();
    }

    private void checkStartIntent() {
        // Check if this is in the secure camera mode.
        Intent intent = getIntent();
        String action = intent.getAction();
        if (INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE.equals(action)
                || ACTION_IMAGE_CAPTURE_SECURE.equals(action)) {
            mSecureCamera = true;
        } else {
            mSecureCamera = intent.getBooleanExtra(SECURE_CAMERA_EXTRA, false);
        }
    }

    @Override
    public void onPauseTasks() {
        CameraPerformanceTracker.onEvent(CameraPerformanceTracker.ACTIVITY_PAUSE);
        Profile profile = mProfiler.create("CameraActivity.onPause").start();

        /*
         * Save the last module index after all secure camera and icon launches,
         * not just on mode switches.
         *
         * Right now we exclude capture intents from this logic, because we also
         * ignore the cross-Activity recovery logic in onStart for capture intents.
         */
        if (!isCaptureIntent()) {
            mSettingsManager.set(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_STARTUP_MODULE_INDEX,
                    mCurrentModeIndex);
        }

        mPaused = true;

        // Delete photos that are pending deletion
        performDeletion();
        mCurrentModule.pause();
        mOrientationManager.pause();

        if (isFilmstripSupported()) {
            mFilmstripManager.pause();
        }
        resetScreenOn();

        mMotionManager.stop();

        // Always stop recording location when paused. Resume will start
        // location recording again if the location setting is on.
        mLocationManager.recordLocation(false);

        UsageStatistics.instance().backgrounded();

        // Camera is in fatal state. A fatal dialog is presented to users, but users just hit home
        // button. Let's just kill the process.
        if (mCameraFatalError && !isFinishing()) {
            Log.v(TAG, "onPause when camera is in fatal state, call Activity.finish()");
            finish();
        } else {
            // Close the camera and wait for the operation done.
            Log.v(TAG, "onPause closing camera");
            if (mCameraController != null) {
                mCameraController.closeCamera(true);
            }
        }

        profile.stop();
    }

    @Override
    public void onResumeTasks() {
        mPaused = false;
        checkPermissions();
        if (!mHasCriticalPermissions) {
            Log.v(TAG, "onResume: Missing critical permissions.");
            finish();
            return;
        }
        resume();
    }

    @Override
    protected void resume() {
        Profile profile = mProfiler.create("CameraActivity.resume").start();
        CameraPerformanceTracker.onEvent(CameraPerformanceTracker.ACTIVITY_RESUME);
        Log.v(TAG, "Build info: " + Build.DISPLAY);
        updateStorageSpaceAndHint(null);

        mLastLayoutOrientation = getResources().getConfiguration().orientation;

        if (AppConfig.isLandscapeScreenSupported()) {
            // TODO: Handle this in OrientationManager.
            // Auto-rotate off
            if (Settings.System.getInt(getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION, 0) == 0) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                mAutoRotateScreen = false;
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                mAutoRotateScreen = true;
            }
        }

        checkStartSource();

        mGalleryIntent = IntentHelper.getGalleryIntent(mAppContext);

        mOrientationManager.resume();

        mCurrentModule.hardResetSettings(mSettingsManager);

        profile.mark();
        mCurrentModule.resume();
        UsageStatistics.instance().changeScreen(currentUserInterfaceMode(),
                NavigationChange.InteractionCause.BUTTON);
        setSwipingEnabled(true);
        profile.mark("mCurrentModule.resume");

        if (isFilmstripSupported()) {
            mFilmstripManager.resume();
        }
        // Default is showing the preview, unless disabled by explicitly
        // starting an activity we want to return from to the filmstrip rather
        // than the preview.
        mResetToPreviewOnResume = true;

        keepScreenOnForAWhile();

        // Lights-out mode at all times.
        mLightsOutRunnable.run();
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        mMainHandler.removeCallbacks(mLightsOutRunnable);
                        mMainHandler.postDelayed(mLightsOutRunnable, LIGHTS_OUT_DELAY_MS);
                    }
                });

        ReleaseHelper.showReleaseInfoDialogOnStart(this, mSettingsManager);
        // Enable location recording if the setting is on.
        final boolean locationRecordingEnabled =
                mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION);
        mLocationManager.recordLocation(locationRecordingEnabled);

        final int previewVisibility = getPreviewVisibility();
        updatePreviewRendering(previewVisibility);

        mMotionManager.start();
        profile.stop();
    }

    private void checkStartSource() {
        // Foreground event logging.  ACTION_STILL_IMAGE_CAMERA and
        // INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE are double logged due to
        // lockscreen onResume->onPause->onResume sequence.
        int source;
        String action = getIntent().getAction();
        if (action == null) {
            source = ForegroundSource.UNKNOWN_SOURCE;
        } else {
            switch (action) {
                case MediaStore.ACTION_IMAGE_CAPTURE:
                    source = ForegroundSource.ACTION_IMAGE_CAPTURE;
                    break;
                case MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA:
                    // was UNKNOWN_SOURCE in Fishlake.
                    source = ForegroundSource.ACTION_STILL_IMAGE_CAMERA;
                    break;
                case MediaStore.INTENT_ACTION_VIDEO_CAMERA:
                    // was UNKNOWN_SOURCE in Fishlake.
                    source = ForegroundSource.ACTION_VIDEO_CAMERA;
                    break;
                case MediaStore.ACTION_VIDEO_CAPTURE:
                    source = ForegroundSource.ACTION_VIDEO_CAPTURE;
                    break;
                case MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE:
                    // was ACTION_IMAGE_CAPTURE_SECURE in Fishlake.
                    source = ForegroundSource.ACTION_STILL_IMAGE_CAMERA_SECURE;
                    break;
                case MediaStore.ACTION_IMAGE_CAPTURE_SECURE:
                    source = ForegroundSource.ACTION_IMAGE_CAPTURE_SECURE;
                    break;
                case Intent.ACTION_MAIN:
                    source = ForegroundSource.ACTION_MAIN;
                    break;
                default:
                    source = ForegroundSource.UNKNOWN_SOURCE;
                    break;
            }
        }
        UsageStatistics.instance().foregrounded(source, currentUserInterfaceMode(),
                isKeyguardSecure(), isKeyguardLocked(),
                mStartupOnCreate, mExecutionStartNanoTime);
    }

    @Override
    public void onStartTasks() {
        mIsActivityRunning = true;

        /*
         * If we're starting after launching a different Activity (lockscreen),
         * we need to use the last mode used in the other Activity, and
         * not the old one from this Activity.
         *
         * This needs to happen before CameraAppUI.resume() in order to set the
         * mode cover icon to the actual last mode used.
         *
         * Right now we exclude capture intents from this logic.
         */
        int modeIndex = getModeIndex();
        if (!isCaptureIntent() && mCurrentModeIndex != modeIndex) {
            onModeSelected(modeIndex);
        }

        if (mResetToPreviewOnResume) {
            mCameraAppUI.resume();
            mResetToPreviewOnResume = false;
            initThumbnail();
        }
    }

    @Override
    protected void onStopTasks() {
        mIsActivityRunning = false;

        mLocationManager.disconnect();
    }

    @Override
    public void onDestroyTasks() {
        super.onDestroyTasks();
        mSecureUriList.clear();
    }

    @Override
    protected void startPermissionsActivity() {
        Intent intent = new Intent(this, PermissionsActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void setRotationAnimation() {
        // maybe use WindowManager.LayoutParams.ROTATION_ANIMATION_JUMPCUT
        super.setRotationAnimation();
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mFilmstripVisible) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (!isFilmstripSupported()) return false;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        /* ZhangChao time:2015-04-21, real delete it when back to camera's preview. START ++++ */
        // Real deletion is postponed until the next user interaction after
        // the gesture that triggers deletion. Until real deletion is
        // performed, users can click the undo button to bring back the
        // image that they chose to delete.
        if (mPendingDeletion && !mIsUndoingDeletion) {
            performDeletion();
        }
        /* ZhangChao time:2015-04-21, real delete it when back to camera's preview. END ---- */

        super.onBackPressed();
    }

    @Override
    public void gotoGallery() {
        if (isFilmstripSupported()) {
            super.gotoGallery();
        } else {
            gotoGallery(getThumbnailUri());
        }
    }

    public void gotoGallery(Uri uri) {
        CameraUtil.viewUri(uri, this);
    }

    @Override
    protected void setFilmstripUiVisibility(boolean visible) {
        boolean backToCamera = mFilmstripUiVisibility && !visible;
        mFilmstripUiVisibility = visible;
        super.setFilmstripUiVisibility(visible);
        // back to camera preview.
        if (backToCamera) {
            onFilmstripBackToCamera();
        }
    }

    /** just called if {@link #isFilmstripSupported()} */
    private void onFilmstripBackToCamera() {
        if (mPendingDeletion && !mIsUndoingDeletion) {
            performDeletion();
        }

        // deletion is a Async task, so get uri from mDataAdapter and generate thumbnail.
        final FilmstripItem lastItem = mDataAdapter.getFilmstripItemAt(0);
        if (lastItem instanceof PhotoItem || lastItem instanceof VideoItem) {
            Uri uri = lastItem.getData().getUri();
            ((CameraAppUI) mCameraAppUI).initThumbnail(uri);
        }
    }

    private void initThumbnail() {
        if (mCameraAppUI instanceof CameraAppUI) {
            if (isSecureCamera()) {
                // refreshSecureUris();
                ((CameraAppUI) mCameraAppUI).initThumbnailInSecureCamera(mSecureUriList);
            } else {
                ((CameraAppUI) mCameraAppUI).initThumbnail();
            }
        }
    }

    @Override
    public boolean isAutoRotateScreen() {
        // TODO: Move to OrientationManager.
        return super.isAutoRotateScreen();
    }

    @Override
    public boolean onShareTargetSelected(ShareActionProvider shareActionProvider, Intent intent) {
        if (!isFilmstripSupported()) return false;
        return super.onShareTargetSelected(shareActionProvider, intent);
    }

    @Override
    protected CaptureSessionManager.SessionListener createSessionListener() {
        return new NewSessionListener();
    }

    /** just support CaptureIndicator if not support filmstrip. */
    protected class NewSessionListener extends MySessionListener {
        @Override
        public void onSessionQueued(Uri uri) {
            if (!isFilmstripSupported()) return;
            super.onSessionQueued(uri);
        }

        @Override
        public void onSessionUpdated(Uri uri) {
            if (!isFilmstripSupported()) return;
            super.onSessionUpdated(uri);
        }

        @Override
        public void onSessionDone(Uri sessionUri) {
            if (!isFilmstripSupported()) return;
            super.onSessionDone(sessionUri);
        }

        @Override
        public void onSessionProgress(Uri uri, int progress) {
            if (!isFilmstripSupported()) return;
            super.onSessionProgress(uri, progress);
        }

        @Override
        public void onSessionProgressText(Uri uri, int messageId) {
            if (!isFilmstripSupported()) return;
            super.onSessionProgressText(uri, messageId);
        }

        @Override
        public void onSessionCaptureIndicatorUpdate(Bitmap indicator, int rotationDegrees) {
            super.onSessionCaptureIndicatorUpdate(indicator, rotationDegrees);
        }

        @Override
        public void onSessionFailed(Uri uri, int failureMessageId, boolean removeFromFilmstrip) {
            if (!isFilmstripSupported()) return;
            super.onSessionFailed(uri, failureMessageId, removeFromFilmstrip);
        }

        @Override
        public void onSessionCanceled(Uri uri) {
            if (!isFilmstripSupported()) return;
            super.onSessionCanceled(uri);
        }

        @Override
        public void onSessionThumbnailUpdate(Bitmap bitmap) {
            if (!isFilmstripSupported()) return;
            super.onSessionThumbnailUpdate(bitmap);
        }

        @Override
        public void onSessionPictureDataUpdate(byte[] pictureData, int orientation) {
            if (!isFilmstripSupported()) return;
            super.onSessionPictureDataUpdate(pictureData, orientation);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return super.onContextItemSelected(item);
    }

    @Override
    public void onModeSelected(int modeIndex) {
        super.onModeSelected(modeIndex);
    }

    /**
     * Shows the settings dialog.
     */
    @Override
    public void onSettingsSelected() {
        UsageStatistics.instance().controlUsed(
                eventprotos.ControlEvent.ControlType.OVERALL_SETTINGS);
        Intent intent = new Intent(this, CameraSettingsActivity.class);
        startActivity(intent);
    }

    @Override
    public void notifyNewMedia(Uri uri) {
        setThumbnailUri(uri);
        if (isFilmstripSupported()) {
            super.notifyNewMedia(uri);
        } else {
            if (mSecureCamera) {
                mSecureUriList.add(0, uri);
            }
            execIndicateCaptureTask(uri);
        }
    }

    protected void execIndicateCaptureTask(final Uri uri) {
        new AsyncTask<Uri, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Uri... uris) {
                final ContentResolver cr = getContentResolver();
                if (!CameraUtil.isUriValid(uri, cr)) {
                    return null;
                }
                String mimeType = cr.getType(uri);
                if (FilmstripItemUtils.isMimeTypeVideo(mimeType)) {
                    sendBroadcast(new Intent(CameraUtil.ACTION_NEW_VIDEO, uri));
                } else if (FilmstripItemUtils.isMimeTypeImage(mimeType)) {
                    CameraUtil.broadcastNewPicture(mAppContext, uri);
                }
                Thumbnail thumbnail = Thumbnail.getThumbnailByUri(cr, uri);
                if (thumbnail == null) return null;
                return thumbnail.getBitmap();
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    indicateCapture(bitmap, 0);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, uri);
    }

    @Override
    public void showUndoDeletionBar() {
        if (isFilmstripSupported()) {
            super.showUndoDeletionBar();
        }
    }

    @Override
    protected void performDeletion() {
        if (isFilmstripSupported()) {
            super.performDeletion();
        }
    }

    public Uri getThumbnailUri() {
        synchronized (mThumbnailLock) {
            return mThumbnailUri;
        }
    }

    public void setThumbnailUri(Uri uri) {
        synchronized (mThumbnailLock) {
            mThumbnailUri = uri;
        }
    }

    public void clearThumbnailUri() {
        setThumbnailUri(null);
    }


    /** use filmstrip open secure camera photos. */
    @Override
    public boolean isFilmstripSupported() {
        return AppConfig.isFilmstripSupported() || isSecureCamera();
    }

    class FilmstripManager {
        void init() {
            if (!Glide.isSetup()) {
                Context context = getAndroidContext();
                Glide.setup(new GlideBuilder(context)
                        .setDecodeFormat(DecodeFormat.ALWAYS_ARGB_8888)
                        .setResizeService(new FifoPriorityThreadPoolExecutor(2)));

                Glide glide = Glide.get(context);

                // As a camera we will use a large amount of memory
                // for displaying images.
                glide.setMemoryCategory(MemoryCategory.HIGH);
            }
        }

        void setActionBar() {
            mActionBar = getActionBar();
            // set actionbar background to 100% or 50% transparent
            if (ApiHelper.isLOrHigher()) {
                mActionBar.setBackgroundDrawable(new ColorDrawable(0x00000000));
            } else {
                mActionBar.setBackgroundDrawable(new ColorDrawable(0x80000000));
            }
        }

        void initController() {
            mCameraAppUI.setFilmstripBottomControlsListener(mMyFilmstripBottomControlListener);

            mAboveFilmstripControlLayout =
                    (FrameLayout) findViewById(R.id.camera_filmstrip_content_layout);

            mFilmstripController = ((FilmstripView) findViewById(R.id.filmstrip_view)).getController();
            mFilmstripController.setImageGap(
                    getResources().getDimensionPixelSize(R.dimen.camera_film_strip_gap));

            ContentResolver appContentResolver = mAppContext.getContentResolver();
            GlideFilmstripManager glideManager = new GlideFilmstripManager(mAppContext);
            mPhotoItemFactory = new PhotoItemFactory(mAppContext, glideManager, appContentResolver,
                    new PhotoDataFactory());
            mVideoItemFactory = new VideoItemFactory(mAppContext, glideManager, appContentResolver,
                    new VideoDataFactory());

            mCameraAppUI.getFilmstripContentPanel().setFilmstripListener(mFilmstripListener);
        }

        void initItems() {
            preloadFilmstripItems();

            setupNfcBeamPush();

            mLocalImagesObserver = new FilmstripContentObserver();
            mLocalVideosObserver = new FilmstripContentObserver();

            getContentResolver().registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true,
                    mLocalImagesObserver);
            getContentResolver().registerContentObserver(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true,
                    mLocalVideosObserver);
        }

        void resume() {
            if (ApiHelper.isLOrHigher()) {
                // hide the up affordance for L devices, it's not very Materially
                mActionBar.setDisplayShowHomeEnabled(false);
            }

            if (!mResetToPreviewOnResume) {
                FilmstripItem item = mDataAdapter.getItemAt(
                        mFilmstripController.getCurrentAdapterIndex());
                if (item != null) {
                    mDataAdapter.refresh(item.getData().getUri());
                }
            }

            // The share button might be disabled to avoid double tapping.
            mCameraAppUI.getFilmstripBottomControls().setShareEnabled(true);

            if (mLocalVideosObserver.isMediaDataChangedDuringPause()
                    || mLocalImagesObserver.isMediaDataChangedDuringPause()) {
                if (!mSecureCamera) {
                    // If it's secure camera, requestLoad() should not be called
                    // as it will load all the data.
                    if (!mFilmstripVisible) {
                        mDataAdapter.requestLoad(new Callback<Void>() {
                            @Override
                            public void onCallback(Void result) {
                                fillTemporarySessions();
                            }
                        });
                    } else {
                        mDataAdapter.requestLoadNewPhotos();
                    }
                }
            }
            mLocalImagesObserver.setActivityPaused(false);
            mLocalVideosObserver.setActivityPaused(false);
            if (!mSecureCamera) {
                mLocalImagesObserver.setForegroundChangeListener(
                        new FilmstripContentObserver.ChangeListener() {
                            @Override
                            public void onChange() {
                                mDataAdapter.requestLoadNewPhotos();
                            }
                        });
            }
        }

        void pause() {
            mLocalImagesObserver.setForegroundChangeListener(null);
            mLocalImagesObserver.setActivityPaused(true);
            mLocalVideosObserver.setActivityPaused(true);
            if (mPreloader != null) {
                mPreloader.cancelAllLoads();
            }
        }
    }
}
