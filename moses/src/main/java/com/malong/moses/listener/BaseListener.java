package com.malong.moses.listener;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.malong.moses.Constants;
import com.malong.moses.DownloadTask;
import com.malong.moses.utils.Utils;

public class BaseListener implements Listener {
    public static final String TAG = "【BaseListener】";
    @SuppressWarnings("PointlessBooleanExpression")
    private static boolean DEBUG = Constants.DEBUG & false;
    Context mContext;
    private ContentObserver mObserver;

    /** 回调在主线 */
    public void register(Context context, int taskId) {
        if (mContext != null && mObserver != null) {
            // 解决重复注册监听
            unregister();
        }
        mContext = context;
        mObserver = new InnerObserver();
        Uri uri = Utils.generateDownloadUri(context, taskId);
        mContext.getContentResolver().registerContentObserver(uri, false, mObserver);
    }

    public void unregister() {
        if (mContext != null && mObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mObserver);
            mObserver = null;
            mContext = null;
        }
    }

    class InnerObserver extends ContentObserver {
        /** 上一次下载状态 **/
        private int mLastState = -1;

        InnerObserver() {
            super(new Handler(Looper.getMainLooper()));
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (Constants.KEY_STATUS_CHANGE.equals(uri.getFragment())) {
                // 状态改变
                try {
                    @SuppressWarnings("ConstantConditions")
                    int status = Integer.parseInt(uri.getQueryParameter(Constants.KEY_STATUS));
                    if (status != mLastState) {
                        if (DEBUG) {
                            Log.d(TAG, "状态发生改变：当前状态=" + status);
                        }
                        mLastState = status;
                        if (status == DownloadTask.STATUS_RUNNING) {
                            onStart();
                        } else if (status == DownloadTask.STATUS_PAUSE) {
                            onPause();
                        } else if (status == DownloadTask.STATUS_SUCCESS) {
                            onSuccess();
                        } else if (status == DownloadTask.STATUS_FAIL) {
                            onCancel();
                        } else if (status == DownloadTask.STATUS_CANCEL) {
                            onCancel();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (DEBUG) Log.d(TAG, "onChange():" + uri.toString());
            if (Constants.KEY_PROCESS_CHANGE.equals(uri.getFragment())) {
                // 进度改变
                try {
                    @SuppressWarnings("ConstantConditions")
                    long current_bytes = Long.parseLong(
                            uri.getQueryParameter(Constants.KEY_PROCESS));
                    @SuppressWarnings("ConstantConditions")
                    long length = Long.parseLong(
                            uri.getQueryParameter(Constants.KEY_LENGTH));
                    onProgress(current_bytes, length);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }


        }
    }


    @Override
    public void onStart() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onSuccess() {

    }


    @Override
    public void onFail() {

    }

    @Override
    public void onProgress(long cur,long length) {

    }
}
