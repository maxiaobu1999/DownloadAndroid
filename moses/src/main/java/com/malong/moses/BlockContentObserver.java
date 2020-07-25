package com.malong.moses;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class BlockContentObserver extends ContentObserver {
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
     */
    //@param uri     URI content://com.norman.malong.downloads/my_downloads/3
    public BlockContentObserver(Context context) {
        super(new Handler(Looper.getMainLooper()));
        mContext = context;
//        DownloadInfo downloadInfo = DownloadHelper.queryDownloadInfo(mContext, uri);
//        mLastState = DownloadInfo.STATUS_PENDING;
    }

    public BlockContentObserver(Handler handler) {
        super(handler);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        if (DEBUG) Log.d(TAG, "onChange():" + uri.toString());
        if (Constants.KEY_BLOCK_PROCESS_CHANGE.equals(uri.getFragment())) {
            // 进度改变
            try {
                @SuppressWarnings("ConstantConditions")
                long current_bytes = Long.parseLong(
                        uri.getQueryParameter(Constants.KEY_PROCESS));
                long length = Long.parseLong(
                        uri.getQueryParameter(Constants.KEY_LENGTH));
                onProcessChange(uri,current_bytes,length);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if (Constants.KEY_BLOCK_STATUS_CHANGE.equals(uri.getFragment())) {
            // 状态改变
            try {
                @SuppressWarnings("ConstantConditions")
                int status = Integer.parseInt(uri.getQueryParameter(Constants.KEY_STATUS));
                if (status != mLastState) {
                    if (DEBUG) {
                        Log.d(TAG, "状态发生改变：当前状态=" + status);
                    }
                    mLastState = status;
                    onStatusChange(uri,status);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


//
//
//        DownloadInfo downloadInfo = DownloadHelper.queryDownloadInfo(mContext, uri);
//        if (downloadInfo == null) {
//            return;
//        }
//        if (DEBUG) {
//            Log.d(TAG, "downloadInfo.status:" + downloadInfo.status);
//        }
//        if (downloadInfo.status == DownloadInfo.STATUS_RUNNING) {
//            if (DEBUG) {
//                Log.d(TAG, "当前进度=" + downloadInfo.current_bytes);
//            }
//            onProcessChange(downloadInfo.current_bytes);
//        }
//        if (downloadInfo.status != mLastState) {
//            if (DEBUG) {
//                Log.d(TAG, "状态发生改变：当前状态=" + downloadInfo.status);
//            }
//            mLastState = downloadInfo.status;
//            onStatusChange(downloadInfo.status);
//        }
    }

    /** 进度变更回调 */
    public void onProcessChange(Uri uri,long cur,long length) {

    }
    /** 状态变更回调 */
    public void onStatusChange(Uri uri,int status) {

    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
    }
}
