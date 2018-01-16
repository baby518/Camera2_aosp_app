package com.plus.camera.app;

public interface AppController extends com.android.camera.app.AppController {
    boolean isSecureCamera();
    boolean isFilmstripSupported();
}
