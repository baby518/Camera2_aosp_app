package com.plus.camera;

import android.content.Intent;

public class PermissionsActivity extends com.android.camera.PermissionsActivity {

    @Override
    protected void handlePermissionsSuccess() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
        finish();
    }
}
