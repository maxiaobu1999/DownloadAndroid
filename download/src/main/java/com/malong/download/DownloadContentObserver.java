package com.malong.download;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class DownloadContentObserver extends ContentObserver {
    public static final String TAG = "【DownloadContentObs】";
    @SuppressWarnings("PointlessBooleanExpression")
    private static boolean DEBUG = Constants.DEBUG & false;
    private Context mContext;
    /** 上一次下载状态 **/
    private int mLastState = -1;

    /**
     * 构造函数
     *
     * @param context context
     * @param uri     URI content://com.norman.malong.downloads/my_downloads/3
     */
    public DownloadContentObserver(Context context, Uri uri) {
        super(new Handler(Looper.getMainLooper()));
        mContext = context;
//        DownloadInfo downloadInfo = DownloadHelper.queryDownloadInfo(mContext, uri);
//        mLastState = DownloadInfo.STATUS_PENDING;
        if (DEBUG) {
            Log.w(TAG, "new DownloadObserver(" + uri + ")");
        }
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        DownloadInfo downloadInfo = DownloadHelper.queryDownloadInfo(mContext, uri);
        if (downloadInfo == null) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "downloadInfo.status:" + downloadInfo.status);
        }
        if (downloadInfo.status == DownloadInfo.STATUS_RUNNING) {
            if (DEBUG) {
                Log.d(TAG, "当前进度=" + downloadInfo.current_bytes);
            }
            onProcessChange(downloadInfo.current_bytes);
        }
        if (downloadInfo.status != mLastState) {
            if (DEBUG) {
                Log.d(TAG, "状态发生改变：当前状态=" + downloadInfo.status);
            }
            mLastState = downloadInfo.status;
            onStatusChange(downloadInfo.status);
        }
    }

    public void onProcessChange(long cur) {

    }

    public void onStatusChange(int status) {

    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
    }
}
