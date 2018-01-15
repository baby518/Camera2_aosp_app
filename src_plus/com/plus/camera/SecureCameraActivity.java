package com.plus.camera;

// Use a different activity for secure camera only. So it can have a different
// task affinity from others. This makes sure non-secure camera activity is not
// started in secure lock screen.
public class SecureCameraActivity extends CameraActivity {
}
