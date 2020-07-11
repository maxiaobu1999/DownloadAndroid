package com.malong.download;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

public class DownloadContentObserver extends ContentObserver {
    public static final String TAG = "【DownloadContentObs】";
    @SuppressWarnings("PointlessBooleanExpression")
    private static boolean DEBUG = Constants.DEBUG & false;

    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public DownloadContentObserver(Handler handler) {
        super(handler);
    }


    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        if (DEBUG) Log.d(TAG, "onChange(2)uri=" + uri.toString());


    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
    }
}
