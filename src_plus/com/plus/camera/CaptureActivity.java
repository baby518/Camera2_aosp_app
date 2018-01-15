package com.plus.camera;

// Use a different activity for capture intents, so it can have a different
// task affinity from others. This makes sure the regular camera activity is not
// reused for IMAGE_CAPTURE or VIDEO_CAPTURE intents from other activities.
public class CaptureActivity extends CameraActivity {
}
