package com.plus.camera;

import android.graphics.Matrix;

import com.android.camera.TextureViewHelper;
import com.plus.camera.util.CameraUtil;

public class CaptureLayoutHelper extends com.android.camera.CaptureLayoutHelper {
    private int mBottomBarCompatibilityHeight = 0;
    private final int mNavigationBarHeight;

    public CaptureLayoutHelper(int bottomBarMinHeight, int bottomBarMaxHeight, int bottomBarOptimalHeight) {
        super(bottomBarMinHeight, bottomBarMaxHeight, bottomBarOptimalHeight);
        mNavigationBarHeight = CameraUtil.getNavigationBarHeight();
    }

    private void calcBottomBarCompatibilityHeight(int width, int height) {
        int longerEdge = Math.max(width, height);
        int shorterEdge = Math.min(width, height);
        // calc suitable bottom bar height depend on 4:3 aspect ratio.
        float spaceNeededLongerEdge = shorterEdge * 4f / 3f;
        mBottomBarCompatibilityHeight = (int) (longerEdge - spaceNeededLongerEdge);
    }

    @Override
    protected PositionConfiguration getPositionConfiguration(int width, int height,
                                                             float previewAspectRatio, int rotation) {
        boolean landscape = width > height;
        calcBottomBarCompatibilityHeight(width, height);
        // If the aspect ratio is defined as fill the screen, then preview should
        // take the screen rect.
        PositionConfiguration config = new PositionConfiguration();
        if (previewAspectRatio == TextureViewHelper.MATCH_SCREEN) {
            config.mPreviewRect.set(0, 0, width, height);
            config.mBottomBarOverlay = true;
            if (landscape) {
                config.mBottomBarRect.set(width - mBottomBarOptimalHeight, 0, width, height);
            } else {
                config.mBottomBarRect.set(0, height - mBottomBarOptimalHeight, width, height);
            }
        } else {
            if (previewAspectRatio < 1) {
                previewAspectRatio = 1 / previewAspectRatio;
            }
            // Get the bottom bar width and height.
            float barSize = mBottomBarCompatibilityHeight;
            int longerEdge = Math.max(width, height);
            int shorterEdge = Math.min(width, height);

            // Check the remaining space if fit short edge.
            float spaceNeededAlongLongerEdge = shorterEdge * previewAspectRatio;
            float remainingSpaceAlongLongerEdge = longerEdge - spaceNeededAlongLongerEdge;

            float previewShorterEdge;
            float previewLongerEdge;
            if (remainingSpaceAlongLongerEdge <= 0) {
                // Preview aspect ratio > screen aspect ratio: fit longer edge.
                previewLongerEdge = longerEdge;
                previewShorterEdge = longerEdge / previewAspectRatio;
                config.mBottomBarOverlay = true;
                if (barSize < mBottomBarMinHeight) {
                    barSize = mBottomBarMinHeight;
                }

                if (landscape) {
                    config.mPreviewRect.set(0, height / 2 - previewShorterEdge / 2, previewLongerEdge,
                            height / 2 + previewShorterEdge / 2);
                    config.mBottomBarRect.set(width - barSize, height / 2 - previewShorterEdge / 2,
                            width, height / 2 + previewShorterEdge / 2);
                } else {
                    config.mPreviewRect.set(width / 2 - previewShorterEdge / 2, 0,
                            width / 2 + previewShorterEdge / 2, previewLongerEdge);
                    config.mBottomBarRect.set(width / 2 - previewShorterEdge / 2, height - barSize,
                            width / 2 + previewShorterEdge / 2, height);
                }
            } else if (remainingSpaceAlongLongerEdge < mNavigationBarHeight) {
                if (barSize < mBottomBarMinHeight) {
                    barSize = mBottomBarMinHeight;
                }
                previewLongerEdge = shorterEdge * previewAspectRatio;
                config.mBottomBarOverlay = true;
                if (landscape) {
                    config.mPreviewRect.set(0, width - previewLongerEdge, width, height);
                    config.mBottomBarRect.set(width - barSize, 0, width, height);
                } else {
                    config.mPreviewRect.set(0, height - previewLongerEdge, width, height);
                    config.mBottomBarRect.set(0, height - barSize, width, height);
                }
            } else if (remainingSpaceAlongLongerEdge < mBottomBarCompatibilityHeight) {
                if (barSize < mBottomBarMinHeight) {
                    barSize = mBottomBarMinHeight;
                }
                previewLongerEdge = shorterEdge * previewAspectRatio;
                config.mBottomBarOverlay = true;
                if (landscape) {
                    config.mPreviewRect.set(0, width - previewLongerEdge, width, height);
                    config.mBottomBarRect.set(width - mNavigationBarHeight - barSize, 0, width - mNavigationBarHeight, height);
                } else {
                    config.mPreviewRect.set(0, height - previewLongerEdge, width, height);
                    config.mBottomBarRect.set(0, height - mNavigationBarHeight - barSize, width, height - mNavigationBarHeight);
                }
            } else {
                previewLongerEdge = shorterEdge * previewAspectRatio;
                if (barSize < mBottomBarMinHeight) {
                    barSize = mBottomBarMinHeight;
                    config.mBottomBarOverlay = true;
                } else {
                    config.mBottomBarOverlay = false;
                }
                if (landscape) {
                    if (config.mBottomBarOverlay) {
                        config.mPreviewRect.set(0, width - previewLongerEdge, width, height);
                    } else {
                        config.mPreviewRect.set(0, width - barSize - previewLongerEdge, width - barSize, height);
                    }
                    config.mBottomBarRect.set(width - barSize, 0, width, height);
                } else {
                    if (config.mBottomBarOverlay) {
                        config.mPreviewRect.set(0, height - previewLongerEdge, width, height);
                    } else {
                        config.mPreviewRect.set(0, height - barSize - previewLongerEdge, width, height - barSize);
                    }
                    config.mBottomBarRect.set(0, height - barSize, width, height);
                }
            }
        }

        if (rotation >= 180) {
            // Rotate 180 degrees.
            Matrix rotate = new Matrix();
            rotate.setRotate(180, width / 2, height / 2);

            rotate.mapRect(config.mPreviewRect);
            rotate.mapRect(config.mBottomBarRect);
        }

        // Round the rect first to avoid rounding errors later on.
        round(config.mBottomBarRect);
        round(config.mPreviewRect);

        return config;
    }
}
