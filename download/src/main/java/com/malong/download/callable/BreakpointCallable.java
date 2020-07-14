package com.malong.download.callable;

import android.content.Context;
import android.util.Log;

import com.malong.download.Constants;
import com.malong.download.DownloadInfo;
import com.malong.download.Http;
import com.malong.download.ProviderHelper;
import com.malong.download.utils.Closeables;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.Callable;

/** 断点续传下载 */
public class BreakpointCallable implements Callable<DownloadInfo> {
    public static final String TAG = "【DownloadCallable】";
    private static boolean DEBUG = Constants.DEBUG;
    DownloadInfo mInfo;
    Context mContext;

    public BreakpointCallable(Context context, DownloadInfo info) {
        mContext = context;
        mInfo = info;
    }

    @Override
    public DownloadInfo call() {
        Log.d(TAG, "mInfo.id:" + mInfo.id);
        //noinspection ResultOfMethodCallIgnored
        new File( mInfo.destination_path).mkdirs();
        File destFile = new File(mInfo.destination_path+mInfo.fileName);// 输出文件
        // 文件存在
        if (destFile.exists()) {
            Log.d(TAG, "destFile.length():" + destFile.length());
        }
        Log.d(TAG, "mInfo.current_bytes:" + mInfo.current_bytes);
        Log.d(TAG, "mInfo.total_bytes:" + mInfo.total_bytes);
        Http http = new Http(mContext, mInfo);
        // 下载内容的长度
        if (mInfo.total_bytes != 0) {
            mInfo.total_bytes = http.getContentLength();
        }
        InputStream is = null;
        FileOutputStream os = null;
        try {
            is = http.getDownloadStream();
            if (is == null) {
                Log.d(TAG, "http.getCode():" + http.getCode());
                mInfo.status = DownloadInfo.STATUS_FAIL;
                ProviderHelper.onStatusChange(mContext, mInfo);
                return mInfo;
            }
            os = new FileOutputStream(destFile);
            final int defaultBufferSize = 1024 * 3;
            byte[] buf = new byte[defaultBufferSize];
            long size = mInfo.current_bytes;
            int len;
            while ((len = is.read(buf)) > 0) {
                os.write(buf, 0, len);
                size += len;
                mInfo.current_bytes = size;
                ProviderHelper.updateProcess(mContext, mInfo);
            }
            os.flush();
            // 下载完成
            mInfo.status = DownloadInfo.STATUS_SUCCESS;
            ProviderHelper.onStatusChange(mContext, mInfo);
        } catch (InterruptedIOException e) {
            // 下载被取消,finally会执行
        } catch (Exception e) {
            e.printStackTrace();
            mInfo.status = DownloadInfo.STATUS_FAIL;
            ProviderHelper.onStatusChange(mContext, mInfo);
        } finally {
            Closeables.closeSafely(is);
            http.close();
            Closeables.closeSafely(os);
        }
        return mInfo;
    }

    private void onSuccess() {
        mInfo.status = DownloadInfo.STATUS_SUCCESS;

    }

    private void onFail() {
        mInfo.status = DownloadInfo.STATUS_FAIL;

    }
}
