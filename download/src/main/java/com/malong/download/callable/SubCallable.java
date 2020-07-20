package com.malong.download.callable;

import android.content.Context;
import android.util.Log;

import com.malong.download.BuildConfig;
import com.malong.download.CancelableThread;
import com.malong.download.Constants;
import com.malong.download.DownloadInfo;
import com.malong.download.Http;
import com.malong.download.HttpInfo;
import com.malong.download.ProviderHelper;
import com.malong.download.partial.PartialInfo;
import com.malong.download.partial.PartialProviderHelper;
import com.malong.download.utils.Closeables;

import java.io.File;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.util.concurrent.Callable;

public class SubCallable implements Callable<PartialInfo> {
    public static final String TAG = "【SubCallable】";
    @SuppressWarnings("PointlessBooleanExpression")
    private static boolean DEBUG = Constants.DEBUG & true;
    private PartialInfo mInfo;
    private Context mContext;
    private CancelableThread mThread;

    public SubCallable(Context context, PartialInfo info) {
        mContext = context;
        mInfo = info;

    }

    @Override
    public PartialInfo call() throws Exception {
        if (DEBUG) Log.d(TAG, "call()执行");
        mThread = (CancelableThread) Thread.currentThread();
        //noinspection ResultOfMethodCallIgnored
        new File(mInfo.destination_path).mkdirs();
        File destFile = new File(mInfo.destination_path + mInfo.fileName);// 输出文件
        // 文件存在
        if (destFile.exists()) {
            Log.d(TAG, "destFile.length():" + destFile.length());
        }

        HttpInfo httpInfo = new HttpInfo();
        httpInfo.download_url = mInfo.download_url;
        httpInfo.destination_uri = mInfo.destination_uri;
        httpInfo.destination_path = mInfo.destination_path;
        httpInfo.fileName = mInfo.fileName;
//        httpInfo.status = mInfo.status;
        httpInfo.method = DownloadInfo.METHOD_PARTIAL;
        httpInfo.total_bytes = mInfo.total_bytes;
        httpInfo.current_bytes = mInfo.current_bytes;
        httpInfo.start_index = mInfo.start_index + mInfo.current_bytes;// 续传
        httpInfo.end_index = mInfo.end_index;
        Http http = new Http(mContext, httpInfo);

        InputStream is = null;
        RandomAccessFile raf = null;
        try {
            is = http.getDownloadStream();
            if (is == null) {
                Log.d(TAG, "http.getCode():" + http.getCode());
                mInfo.status = DownloadInfo.STATUS_FAIL;
                PartialProviderHelper.onPartialStatusChange(mContext, mInfo);
                return mInfo;
            }
            raf = new RandomAccessFile(destFile, "rw");
            raf.seek( mInfo.start_index + mInfo.current_bytes);
            final int defaultBufferSize = 1024 * 3;
            byte[] buf = new byte[defaultBufferSize];
            long size = mInfo.current_bytes;
            int len;
            while ((len = is.read(buf)) > 0) {
                // 响应task。cancel()
                Log.d(TAG, "isInterrupted():" + mThread.cancel);
                if (mThread.cancel) {
                    mThread.cancel = false;
                    if (BuildConfig.DEBUG) Log.d(TAG, "任务被取消");
                    return mInfo;
                }
                raf.write(buf, 0, len);
                size += len;
                mInfo.current_bytes = size;
                PartialProviderHelper.updatePartialProcess(mContext, mInfo);
            }
            // 下载完成
            mInfo.status = DownloadInfo.STATUS_SUCCESS;
            PartialProviderHelper.onPartialStatusChange(mContext, mInfo);
        } catch (InterruptedIOException e) {
            // 下载被取消,finally会执行
        } catch (Exception e) {
            e.printStackTrace();
            mInfo.status = DownloadInfo.STATUS_FAIL;
            PartialProviderHelper.onPartialStatusChange(mContext, mInfo);
        } finally {
            Closeables.closeSafely(is);
            http.close();
            Closeables.closeSafely(raf);
        }
        return null;
    }

}
