package com.plus.camera;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ShareActionProvider;

import com.android.camera.stats.UsageStatistics;
import com.android.ex.camera2.portability.CameraAgent;
import com.google.common.logging.eventprotos;
import com.plus.camera.settings.CameraSettingsActivity;

public class CameraActivity extends com.android.camera.CameraActivity {
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
        super.onCreateTasks(state);
    }

    @Override
    public void onPauseTasks() {
        super.onPauseTasks();
    }

    @Override
    public void onResumeTasks() {
        super.onResumeTasks();
    }

    @Override
    public void onStartTasks() {
        super.onStartTasks();
    }

    @Override
    protected void onStopTasks() {
        super.onStopTasks();
    }

    @Override
    public void onDestroyTasks() {
        super.onDestroyTasks();
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
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void gotoGallery() {
        super.gotoGallery();
    }

    @Override
    public boolean isAutoRotateScreen() {
        return super.isAutoRotateScreen();
    }

    @Override
    public boolean onShareTargetSelected(ShareActionProvider shareActionProvider, Intent intent) {
        return super.onShareTargetSelected(shareActionProvider, intent);
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
        super.notifyNewMedia(uri);
    }

    @Override
    public void showUndoDeletionBar() {
        super.showUndoDeletionBar();
    }
}
