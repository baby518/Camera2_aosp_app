package com.plus.camera.app;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.android.camera.app.AppController;
import com.android.camera2.R;

public class FilmstripBottomPanel extends com.android.camera.app.FilmstripBottomPanel
        implements CameraAppUI.BottomPanelExtended {
    private CameraAppUI.BottomPanelExtended.Listener mExtendedListener;
    private View mInfoButton;

    public FilmstripBottomPanel(AppController controller, ViewGroup bottomControlsLayout) {
        super(controller, bottomControlsLayout);
        setupInfoButton();
    }

    private void setupInfoButton() {
        mInfoButton = (ImageButton) mLayout.findViewById(R.id.filmstrip_bottom_control_info);
        mInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mExtendedListener != null) {
                    mExtendedListener.onAction(R.id.action_details);
                }
            }
        });
    }

    @Override
    public void setExtendedListener(CameraAppUI.BottomPanelExtended.Listener listener) {
        mExtendedListener = listener;
    }
}
