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

/**
 * 下载的监听
 */
public class DownloadBlockListener {
    public static final String TAG = "【DownloadBlockListener】";
    @SuppressWarnings("PointlessBooleanExpression")
    private static boolean DEBUG = Constants.DEBUG & true;
    Context mContext;
    DownloadTask mTask;
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


    public void onProcessChange(long cur, long length) {

    }

    public void onStatusChange(int status) {

    }

    public void onBlockProcessChange(int index, long cur, long length) {

    }

    public void onBlockStatusChange(int index, int status) {

    }


    class InnerObserver extends ContentObserver {
        public InnerObserver() {
            super(new Handler(Looper.getMainLooper()));
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (DEBUG) Log.d(TAG, "onChange():" + uri.toString());
            if (Constants.KEY_PROCESS_CHANGE.equals(uri.getFragment())) {
                // 进度改变
                try {
                    @SuppressWarnings("ConstantConditions")
                    long current_bytes = Long.parseLong(
                            uri.getQueryParameter(Constants.KEY_PROCESS));
                    if (DEBUG) {
                        Log.d(TAG, "总进度改变=" + current_bytes);
                    }
                    @SuppressWarnings("ConstantConditions")
                    long length = Long.parseLong(
                            uri.getQueryParameter(Constants.KEY_LENGTH));
                    onProcessChange(current_bytes, length);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (Constants.KEY_BLOCK_PROCESS_CHANGE.equals(uri.getFragment())) {
                // 分片进度改变
                try {
                    try {
                        @SuppressWarnings("ConstantConditions")
                        int index = Integer.parseInt(uri.getQueryParameter(Constants.KEY_PARTIAL_NUM));
                        @SuppressWarnings("ConstantConditions")
                        long current_bytes = Long.parseLong(
                                uri.getQueryParameter(Constants.KEY_PROCESS));
                        if (DEBUG) {
                            Log.d(TAG, index + "分片进度改变=" + current_bytes);
                        }
                        @SuppressWarnings("ConstantConditions")
                        long length = Long.parseLong(
                                uri.getQueryParameter(Constants.KEY_LENGTH));
                        onBlockProcessChange(index, current_bytes, length);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (Constants.KEY_STATUS_CHANGE.equals(uri.getFragment())) {
                // 状态改变
                try {
                    @SuppressWarnings("ConstantConditions")
                    int status = Integer.parseInt(uri.getQueryParameter(Constants.KEY_STATUS));
                    if (DEBUG) {
                        Log.d(TAG, "状态发生改变：当前状态=" + status);
                    }
                    onStatusChange(status);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (Constants.KEY_BLOCK_STATUS_CHANGE.equals(uri.getFragment())) {
                // 分片状态改变
                try {
                    @SuppressWarnings("ConstantConditions")
                    int status = Integer.parseInt(uri.getQueryParameter(Constants.KEY_STATUS));
                    @SuppressWarnings("ConstantConditions")
                    int index = Integer.parseInt(uri.getQueryParameter(Constants.KEY_PARTIAL_NUM));
                    if (DEBUG) {
                        Log.d(TAG, "分片状态改变：当前状态=" + status);
                    }
                    onBlockStatusChange(index, status);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }

}
