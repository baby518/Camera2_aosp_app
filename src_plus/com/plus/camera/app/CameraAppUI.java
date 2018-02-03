package com.plus.camera.app;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;

import com.android.camera.CameraModule;
import com.android.camera.app.AppController;
import com.android.camera.module.ModuleController;
import com.android.camera.ui.MainActivityLayout;
import com.android.camera2.R;
import com.plus.camera.CameraActivity;
import com.plus.camera.CaptureLayoutHelper;
import com.plus.camera.Thumbnail;
import com.plus.camera.util.CameraUtil;
import com.plus.camera.widget.RoundedThumbnailView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class CameraAppUI extends com.android.camera.app.CameraAppUI {
    /**
     * The bottom controls on the filmstrip.
     */
    public interface BottomPanelExtended {
        void setExtendedListener(Listener listener);
        /**
         * Classes implementing this interface can listen for events on the bottom
         * controls.
         */
        interface Listener {
            public void onAction(int actionId);
        }
    }

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
    protected void initCaptureLayoutHelper() {
        Resources res = mController.getAndroidContext().getResources();
        mCaptureLayoutHelper = new CaptureLayoutHelper(
                res.getDimensionPixelSize(R.dimen.bottom_bar_height_min),
                res.getDimensionPixelSize(R.dimen.bottom_bar_height_max),
                res.getDimensionPixelSize(R.dimen.bottom_bar_height_optimal));
    }

    @Override
    protected void initFilmstrip(ViewGroup appRootView) {
        if (isFilmstripSupported()) {
            ViewGroup layout = (ViewGroup) mAppRootView.findViewById(R.id.filmstrip_bottom_panel);
            layout.setPadding(
                    layout.getPaddingLeft(),
                    layout.getPaddingTop(),
                    layout.getPaddingRight(),
                    layout.getPaddingBottom() + CameraUtil.getNavigationBarHeight());
            mFilmstripBottomControls = new FilmstripBottomPanel(mController, layout);
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

    public void setFilmstripBottomExtendedListener(BottomPanelExtended.Listener listener) {
        if (!isFilmstripSupported()) return;
        if (mFilmstripBottomControls instanceof FilmstripBottomPanel) {
            ((FilmstripBottomPanel) mFilmstripBottomControls).setExtendedListener(listener);
        }
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
        updateCaptureIndicatorThumbnail(thumbnailBitmap, rotation, true);
    }

    private boolean needCaptureIndicator() {
        if (mIsCaptureIntent) return false;
        if (mSuppressCaptureIndicator || getFilmstripVisibility() == View.VISIBLE) {
            return false;
        }
        return true;
    }

    @Override
    public void hideCaptureIndicator() {
        super.hideCaptureIndicator();
    }

    public void showCaptureIndicator() {
        if (mRoundedThumbnailView instanceof RoundedThumbnailView) {
            ((RoundedThumbnailView) mRoundedThumbnailView).showThumbnail();
        }
    }

    private CameraModule getCurrentModule() {
        ModuleController moduleController = mController.getCurrentModuleController();
        if (moduleController instanceof CameraModule) {
            return (CameraModule) moduleController;
        }
        return null;
    }

    public void initThumbnail(Uri uri) {
        if (mRoundedThumbnailView == null) return;
        ContentResolver contentResolver = mController.getAndroidContext().getContentResolver();
        final WeakReference<ContentResolver> resolver = new WeakReference<ContentResolver>(contentResolver);
        updateThumbnail(resolver, uri);
    }

    public void initThumbnail() {
        if (mRoundedThumbnailView == null) return;
        ContentResolver contentResolver = mController.getAndroidContext().getContentResolver();
        final WeakReference<ContentResolver> resolver = new WeakReference<ContentResolver>(contentResolver);
        updateThumbnail(resolver, null);
    }

    public void initThumbnailInSecureCamera(ArrayList<Uri> secureUriList) {
        if (mRoundedThumbnailView == null) return;
        ContentResolver contentResolver = mController.getAndroidContext().getContentResolver();
        final WeakReference<ContentResolver> resolver = new WeakReference<ContentResolver>(contentResolver);
        if (secureUriList != null && secureUriList.size() > 0) {
            Uri thumbnailUri = secureUriList.get(0);
            updateThumbnail(resolver, thumbnailUri);
        } else {
            clearThumbnailView();
        }
    }

    /** update thumbnail bitmap by uri, update thumbnail bitmap by latest if pass null uri.*/
    private void updateThumbnail(final WeakReference<ContentResolver> resolver, Uri targetUri) {
        (new AsyncTask<Uri, Void, Thumbnail>() {
            @Override
            protected Thumbnail doInBackground(Uri... params) {
                Uri uri = params[0];
                ContentResolver cr = resolver.get();
                if (cr == null) return null;
                if (uri == null) {
                    return Thumbnail.getLastThumbnail(cr);
                } else {
                    return Thumbnail.getThumbnailByUri(cr, uri);
                }
            }

            @Override
            protected void onPostExecute(Thumbnail thumbnail) {
                if (thumbnail != null) {
                    if (mController instanceof CameraActivity) {
                        ((CameraActivity) mController).setThumbnailUri(thumbnail.getUri());
                    }
                    startThumbnailAnimation(thumbnail.getBitmap(), false);
                } else {
                    clearThumbnailView();
                }
            }
        }).execute(targetUri);
    }

    public void startThumbnailAnimation(Bitmap bitmap, boolean needAnimation) {
        startCaptureIndicatorRevealAnimation(getCurrentModule().getPeekAccessibilityString());
        updateCaptureIndicatorThumbnail(bitmap, 0, needAnimation);
    }

    private void updateCaptureIndicatorThumbnail(Bitmap thumbnailBitmap, int rotation, boolean needAnimation) {
        if (!needCaptureIndicator()) return;
        if (mRoundedThumbnailView instanceof RoundedThumbnailView) {
            ((RoundedThumbnailView) mRoundedThumbnailView).setThumbnail(thumbnailBitmap, rotation, needAnimation);
        }
    }

    private void clearThumbnailView() {
        clearThumbnailUri();
        if (mRoundedThumbnailView instanceof RoundedThumbnailView) {
            ((RoundedThumbnailView) mRoundedThumbnailView).clearThumbnail();
        }
    }

    public void clearThumbnailUri() {
        updateThumbnailUri(null);
    }

    public void updateThumbnailUri(Uri uri) {
        if (mController instanceof CameraActivity) {
            ((CameraActivity) mController).setThumbnailUri(uri);
        }
    }
}
