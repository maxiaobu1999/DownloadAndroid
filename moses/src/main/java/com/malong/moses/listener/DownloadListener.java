package com.malong.moses.listener;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import com.malong.moses.DownloadContentObserver;
import com.malong.moses.DownloadTask;
import com.malong.moses.utils.Utils;

/**
 * 下载的监听
 *
 *  stop
 */
public class DownloadListener {
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
        mObserver = new InnerObserver(context);
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


    public void onProcessChange(Uri uri, long cur,long length) {

    }

    public void onStatusChange(Uri uri, int status) {

    }


    class InnerObserver extends DownloadContentObserver {

        public InnerObserver(Context context) {
            super(context);
        }

        public InnerObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onProcessChange(Uri uri, long cur,long length) {
            super.onProcessChange(uri, cur,length);
            DownloadListener.this.onProcessChange(uri, cur,length);
        }

        @Override
        public void onStatusChange(Uri uri, int status) {
            DownloadListener.this.onStatusChange(uri, status);

        }
    }
}
