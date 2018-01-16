package com.plus.camera;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.android.camera.Storage;

import java.io.File;

public class Thumbnail extends com.android.camera.Thumbnail {
    private static final String TAG = "Thumbnail";
    private Bitmap mBitmap;
    private Uri mThumbnailUri;

    private static final String BUCKET_ID = String.valueOf(Storage.DIRECTORY.toLowerCase().hashCode());

    private static class Media {
        public Media(long id, int orientation, long dateTaken, Uri uri, String path) {
            this.id = id;
            this.orientation = orientation;
            this.dateTaken = dateTaken;
            this.uri = uri;
            this.path = path;
        }

        boolean isValid() {
            return new File(path).exists();
        }

        public final long id;
        public final int orientation;
        public final long dateTaken;
        public final Uri uri;
        public final String path;
    }

    private Thumbnail(Uri uri, Bitmap bitmap, int orientation) {
        mThumbnailUri = uri;
        mBitmap = rotateImage(bitmap, orientation);
        if (mBitmap == null)
            throw new IllegalArgumentException("null bitmap");
    }

    public Uri getUri() {
        return mThumbnailUri;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public static Thumbnail getLastThumbnail(ContentResolver resolver) {
        Media image = getLastImageMedia(resolver);
        Media video = getLastVideoMedia(resolver);
        if (image == null && video == null) {
            return null;
        }

        Media lastMedia;
        // If there is only image or video, get its thumbnail. If both exist,
        // get the thumbnail of the one that is newer.
        if (image == null) {
            lastMedia = video;
        } else if (video == null) {
            lastMedia = image;
        } else if (image.dateTaken >= video.dateTaken) {
            lastMedia = image;
        } else {
            lastMedia = video;
        }

        return generateMediaThumbnail(resolver, lastMedia);
    }

    private static Media getLastImageMedia(ContentResolver resolver) {
        Uri baseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        Uri query = baseUri.buildUpon().appendQueryParameter("limit", "1").build();
        String[] projection = new String[] {
                MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.ORIENTATION,
                MediaStore.Images.ImageColumns.DATE_TAKEN, MediaStore.MediaColumns.DATA
        };
        String selection = MediaStore.Images.ImageColumns.MIME_TYPE + "='image/jpeg' AND "
                + MediaStore.Images.ImageColumns.BUCKET_ID + '='
                + BUCKET_ID;
        String order = MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC,"
                + MediaStore.Images.ImageColumns._ID + " DESC";

        Cursor cursor = null;
        try {
            cursor = resolver.query(query, projection, selection, null, order);
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                return new Media(id, cursor.getInt(1), cursor.getLong(2),
                        ContentUris.withAppendedId(baseUri, id), cursor.getString(3));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private static Media getLastVideoMedia(ContentResolver resolver) {
        Uri baseUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        Uri query = baseUri.buildUpon().appendQueryParameter("limit", "1").build();
        String[] projection = new String[] {
                MediaStore.Video.VideoColumns._ID, MediaStore.MediaColumns.DATA,
                MediaStore.Video.VideoColumns.DATE_TAKEN
        };
        String selection = MediaStore.Video.VideoColumns.BUCKET_ID + '=' + BUCKET_ID;
        String order = MediaStore.Video.VideoColumns.DATE_TAKEN + " DESC,"
                + MediaStore.Video.VideoColumns._ID + " DESC";

        Cursor cursor = null;
        try {
            cursor = resolver.query(query, projection, selection, null, order);
            if (cursor != null && cursor.moveToFirst()) {
                Log.d(TAG, "getLastVideoThumbnail: " + cursor.getString(1));
                long id = cursor.getLong(0);
                return new Media(id, 0, cursor.getLong(2),
                        ContentUris.withAppendedId(baseUri, id),
                        cursor.getString(1));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public static Thumbnail getThumbnailByUri(ContentResolver resolver, Uri uri) {
        Media media = getMediaByUri(resolver, uri);
        return generateMediaThumbnail(resolver, media);
    }

    private static Media getMediaByUri(ContentResolver resolver, Uri uri) {
        int id = Integer.valueOf(uri.getLastPathSegment());
        Uri query = uri.buildUpon().appendQueryParameter("limit", "1").build();

        String selection = MediaStore.MediaColumns._ID + '=' + id;

        Cursor cursor = null;
        try {
            String mimeType = resolver.getType(query);
            if (mimeType == null) return null;

            String order;
            String[] projection;
            if (mimeType.startsWith("video/")) {
                order = MediaStore.Video.VideoColumns.DATE_TAKEN + " DESC," +
                        MediaStore.Video.VideoColumns._ID + " DESC";
                projection = new String[] {
                        MediaStore.Video.VideoColumns.DATE_TAKEN, MediaStore.MediaColumns.DATA
                };
                cursor = resolver.query(query, projection, selection, null, order);
                if (cursor != null && cursor.moveToFirst()) {
                    return new Media(id, 0, cursor.getLong(0),
                            uri, cursor.getString(1));
                }
            } else if (mimeType.startsWith("image/")) {
                order = MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC," +
                        MediaStore.Images.ImageColumns._ID + " DESC";
                projection = new String[] {
                        MediaStore.Images.ImageColumns.ORIENTATION,
                        MediaStore.Images.ImageColumns.DATE_TAKEN, MediaStore.MediaColumns.DATA
                };
                cursor = resolver.query(query, projection, selection, null, order);
                if (cursor != null && cursor.moveToFirst()) {
                    return new Media(id, cursor.getInt(0), cursor.getLong(1),
                            uri, cursor.getString(2));
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private static Thumbnail generateMediaThumbnail(ContentResolver resolver, Media media) {
        if (media == null) return null;
        String mimeType = resolver.getType(media.uri);
        if (mimeType == null) return null;

        if (mimeType.startsWith("video/")) {
            Bitmap bitmap = MediaStore.Video.Thumbnails.getThumbnail(resolver, media.id,
                    MediaStore.Video.Thumbnails.MINI_KIND, null);
            if (media.isValid()) {
                return createThumbnail(media.uri, bitmap, media.orientation);
            }
        } else if (mimeType.startsWith("image/")) {
            Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(resolver, media.id,
                    MediaStore.Images.Thumbnails.MINI_KIND, null);
            if (media.isValid()) {
                return createThumbnail(media.uri, bitmap, media.orientation);
            }
        }
        return null;
    }

    private static Thumbnail createThumbnail(Uri uri, Bitmap bitmap, int orientation) {
        if (bitmap == null) {
            Log.e(TAG, "Failed to create thumbnail from null bitmap");
            return null;
        }
        try {
            return new Thumbnail(uri, bitmap, orientation);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to construct thumbnail", e);
            return null;
        }
    }

    private static Bitmap rotateImage(Bitmap bitmap, int orientation) {
        if (orientation != 0) {
            // We only rotate the thumbnail once even if we get OOM.
            Matrix m = new Matrix();
            m.setRotate(orientation, bitmap.getWidth() * 0.5f, bitmap.getHeight() * 0.5f);

            try {
                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                        bitmap.getHeight(), m, true);
                // If the rotated bitmap is the original bitmap, then it
                // should not be recycled.
                if (rotated != bitmap)
                    bitmap.recycle();
                return rotated;
            } catch (Throwable t) {
                Log.w(TAG, "Failed to rotate thumbnail", t);
            }
        }
        return bitmap;
    }

}
