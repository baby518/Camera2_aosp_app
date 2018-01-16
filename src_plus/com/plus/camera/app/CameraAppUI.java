package com.plus.camera.app;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;

import com.android.camera.app.AppController;
import com.android.camera.ui.MainActivityLayout;
import com.android.camera.widget.RoundedThumbnailView;
import com.plus.camera.CameraActivity;

public class CameraAppUI extends com.android.camera.app.CameraAppUI {

    public CameraAppUI(AppController controller, MainActivityLayout appRootView, boolean isCaptureIntent) {
        super(controller, appRootView, isCaptureIntent);
    }

    private boolean isFilmstripSupported() {
        if (mController instanceof com.plus.camera.app.AppController) {
            return (((com.plus.camera.app.AppController) mController).isFilmstripSupported());
        }
        return true;
    }

    @Override
    protected void initFilmstrip(ViewGroup appRootView) {
        if (isFilmstripSupported()) {
            super.initFilmstrip(appRootView);
            mRoundedThumbnailView.setCallback(new RoundedThumbnailView.Callback() {
                @Override
                public void onHitStateFinished() {
                    showFilmstrip();
                }
            });
        } else {
            mRoundedThumbnailView.setCallback(new RoundedThumbnailView.Callback() {
                @Override
                public void onHitStateFinished() {
                    if (mController instanceof CameraActivity) {
                        ((CameraActivity) mController).gotoGallery();
                    }
                }
            });
        }
    }

    @Override
    public void setupClingForViewer(int viewerType) {
        if (!isFilmstripSupported()) return;
        super.setupClingForViewer(viewerType);
    }

    @Override
    public void clearClingForViewer(int viewerType) {
        if (!isFilmstripSupported()) return;
        super.clearClingForViewer(viewerType);
    }

    @Override
    public void resume() {
        super.resume();
    }

    @Override
    public void onModeButtonPressed(int modeIndex) {
        super.onModeButtonPressed(modeIndex);
    }

    @Override
    public void showBottomControls() {
        if (!isFilmstripSupported()) return;
        super.showBottomControls();
    }

    @Override
    public void hideBottomControls() {
        if (!isFilmstripSupported()) return;
        super.hideBottomControls();
    }

    @Override
    public void setFilmstripBottomControlsListener(BottomPanel.Listener listener) {
        if (!isFilmstripSupported()) return;
        super.setFilmstripBottomControlsListener(listener);
    }

    @Override
    public void hideFilmstrip() {
        if (!isFilmstripSupported()) return;
        super.hideFilmstrip();
    }

    @Override
    public int getFilmstripVisibility() {
        if (mFilmstripLayout == null) return View.GONE;
        return super.getFilmstripVisibility();
    }

    @Override
    public void updateCaptureIndicatorThumbnail(Bitmap thumbnailBitmap, int rotation) {
        super.updateCaptureIndicatorThumbnail(thumbnailBitmap, rotation);
    }

    @Override
    public void hideCaptureIndicator() {
        super.hideCaptureIndicator();
    }
}
