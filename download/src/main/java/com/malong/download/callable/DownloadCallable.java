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
import com.malong.download.utils.Closeables;
import com.malong.download.utils.FileUtils;
import com.malong.download.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.Callable;

/** 普通下载 删除旧文件，重新下载 。服务端不支持局部下载时使用 */
public class DownloadCallable implements Callable<DownloadInfo> {
    public static final String TAG = "【DownloadCallable】";
    @SuppressWarnings("PointlessBooleanExpression")
    private static boolean DEBUG = Constants.DEBUG & true;
    DownloadInfo mInfo;
    Context mContext;
    CancelableThread mThread;

    public DownloadCallable(Context context, DownloadInfo info) {
        mContext = context;
        mInfo = info;
    }

    @Override
    public DownloadInfo call() {
        if (DEBUG) Log.d(TAG, "call()执行");
        mThread = (CancelableThread) Thread.currentThread();
        // 删除掉过去下载的文件（eg：下一半的重新下载）
        File destFile = new File(mInfo.destination_path + mInfo.fileName);// 输出文件
        if (destFile.exists()) {
            if (BuildConfig.DEBUG) Log.d(TAG, "删除残留文件：" + destFile.getAbsolutePath());
            FileUtils.deleteFile(destFile);
        }
        HttpInfo httpInfo = new HttpInfo();
        httpInfo.download_url = mInfo.download_url;
        httpInfo.destination_uri = mInfo.destination_uri;
        httpInfo.destination_path = mInfo.destination_path;
        httpInfo.fileName = mInfo.fileName;
//        httpInfo.status = mInfo.status;
        httpInfo.method = mInfo.method;
        httpInfo.total_bytes = mInfo.total_bytes;
        httpInfo.current_bytes = mInfo.current_bytes;
        Http http = new Http(mContext, httpInfo);
        // 下载内容的长度
        InputStream is = null;
        FileOutputStream os = null;
        try {
            is = http.getDownloadStream();

            if (mInfo.total_bytes <=0) {
                mInfo.total_bytes = http.getContentLength();
            }
            mInfo.etag = http.getETag();
            ProviderHelper.updateDownloadInfoPortion(mContext, mInfo);


            if (DEBUG) Log.d(TAG, "http.getCode():" + http.getCode());
            if (is == null) {
                mInfo.status = DownloadInfo.STATUS_FAIL;
                ProviderHelper.onStatusChange(mContext, mInfo);
                return mInfo;
            }
            os = Utils.getOutputStream(mContext, mInfo);
            final int defaultBufferSize = 1024 * 3;
            byte[] buf = new byte[defaultBufferSize];
            long size = 0;
            int len;
            while ((len = is.read(buf)) > 0) {
                // 响应task。cancel()
                Log.d(TAG, "isInterrupted():" + mThread.cancel);
                if ( mThread.cancel) {
                    mThread.cancel = false;
                    return mInfo;
                }
                os.write(buf, 0, len);
                size += len;
                mInfo.current_bytes = size;
                Log.d(TAG, "size:" + size);
                ProviderHelper.updateProcess(mContext, mInfo);
            }
            os.flush();
            // 下载完成
            mInfo.status = DownloadInfo.STATUS_SUCCESS;
            ProviderHelper.onStatusChange(mContext, mInfo);
        } catch (InterruptedIOException e) {
            // task被取消,finally会执行
        } catch (Exception e) {
            mInfo.status = DownloadInfo.STATUS_FAIL;
            ProviderHelper.onStatusChange(mContext, mInfo);
            e.printStackTrace();
        } finally {
            Closeables.closeSafely(is);
            http.close();
            Closeables.closeSafely(os);
        }
        return mInfo;
    }

}
