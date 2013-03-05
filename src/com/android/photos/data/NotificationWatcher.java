/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.photos.data;

import android.net.Uri;

import com.android.photos.data.PhotoProvider.ChangeNotification;

import java.util.HashSet;
import java.util.Set;

/**
 * Used for capturing notifications from PhotoProvider without relying on
 * ContentResolver. MockContentResolver does not allow sending notification to
 * ContentObservers, so PhotoProvider allows this alternative for testing.
 */
public class NotificationWatcher implements ChangeNotification {
    private Set<Uri> mUris = new HashSet<Uri>();

    @Override
    public void notifyChange(Uri uri) {
        mUris.add(uri);
    }

    public boolean isNotified(Uri uri) {
        return mUris.contains(uri);
    }

    public int notificationCount() {
        return mUris.size();
    }

    public void reset() {
        mUris.clear();
    }
}
