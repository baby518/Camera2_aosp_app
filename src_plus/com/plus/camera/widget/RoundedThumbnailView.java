package com.plus.camera.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;

import com.android.camera.async.MainThread;
import com.android.camera.debug.Log;

public class RoundedThumbnailView extends com.android.camera.widget.RoundedThumbnailView {
    public RoundedThumbnailView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void startRevealThumbnailAnimation(String accessibilityString) {
        MainThread.checkMainThread();
        // Create a new request.
        mPendingRequest = new RevealRequest(mViewRect.width(), accessibilityString);
    }

    @Override
    public void setThumbnail(final Bitmap thumbnailBitmap, final int rotation) {
        setThumbnail(thumbnailBitmap, rotation, true);
    }

    public void setThumbnail(final Bitmap thumbnailBitmap, final int rotation, boolean needAnimation) {
        MainThread.checkMainThread();

        if (mPendingRequest != null) {
            if (needAnimation) {
                mPendingRequest.setThumbnailBitmap(thumbnailBitmap, rotation);
                runPendingRequestAnimation();
            } else {
                boolean running = mThumbnailAnimatorSet != null && mThumbnailAnimatorSet.isRunning();
                if (running) {
                    mThumbnailAnimatorSet.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mPendingRequest.setThumbnailBitmap(thumbnailBitmap, rotation);
                            runPendingRequestWithoutAnimation();
                        }
                    });
                } else {
                    mPendingRequest.setThumbnailBitmap(thumbnailBitmap, rotation);
                    runPendingRequestWithoutAnimation();
                }
            }
        } else {
            Log.e(TAG, "Pending thumb was null!");
        }
    }

    private void runPendingRequestWithoutAnimation() {
        // maybe use a runnable to avoid draw a blank thumbnail.
        if (mPendingRequest != null) {
            mBackgroundRequest = mPendingRequest;
            mBackgroundRequest.finishRippleAnimation();
            mBackgroundRequest.finishThumbnailAnimation();
        }
        onAllAnimationEnd();
    }

    private void onAllAnimationEnd() {
        // Must set visibility or invalidate()
        // Make this view visible.
        setVisibility(VISIBLE);
    }

    @Override
    public void hideThumbnail() {
        MainThread.checkMainThread();
        // Make this view invisible.
        setVisibility(GONE);

        clearAnimations();

        // Don't Remove all pending reveal requests, because they will be used in VISIBLE.
    }

    /** clear thumbnail and remove pending reveal requests. */
    public void clearThumbnail() {
        super.hideThumbnail();
    }

    public void showThumbnail() {
        setVisibility(VISIBLE);
    }
}
