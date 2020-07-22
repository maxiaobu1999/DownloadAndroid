package com.malong.download.callable;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.malong.download.CancelableThread;
import com.malong.download.Constants;
import com.malong.download.DownloadInfo;
import com.malong.download.ProviderHelper;
import com.malong.download.connect.Connection;
import com.malong.download.connect.HttpInfo;
import com.malong.download.connect.ResponseInfo;
import com.malong.download.utils.Closeables;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.Callable;

/** 断点续传下载 */
public class BreakpointCallable implements Callable<DownloadInfo> {
    public static final String TAG = "【BreakpointCallable】";
    private static boolean DEBUG = Constants.DEBUG;
    private DownloadInfo mInfo;
    private Context mContext;

    public BreakpointCallable(Context context, DownloadInfo info) {
        mContext = context;
        mInfo = info;
    }

    @Override
    public DownloadInfo call() {
        if (DEBUG) Log.d(TAG, "call()执行");
        CancelableThread mThread = (CancelableThread) Thread.currentThread();

        HttpInfo httpInfo = new HttpInfo();
        httpInfo.download_url = mInfo.download_url;
        httpInfo.destination_uri = mInfo.destination_uri;
        httpInfo.destination_path = mInfo.destination_path;
        httpInfo.fileName = mInfo.fileName;
        httpInfo.method = mInfo.method;
        // 获取响应头
        Connection connection = new Connection(mContext, httpInfo);
        ResponseInfo responseInfo = connection.getResponseInfo();
        if (responseInfo == null) {// 下载失败
            ProviderHelper.updateStatus(mContext, DownloadInfo.STATUS_FAIL, mInfo);
            return mInfo;
        }
        if (TextUtils.isEmpty(responseInfo.acceptRanges)) {
            if (DEBUG) Log.e(TAG,
                    "无法下载，响应头没有 Accept-Ranges 不支持断点续传。下载地址：" + mInfo.download_url);
            ProviderHelper.updateStatus(mContext, DownloadInfo.STATUS_FAIL, mInfo);
            return mInfo;
        }
        if (responseInfo.contentLength != 0) {
            mInfo.total_bytes = responseInfo.contentLength;
        }else {
            if (DEBUG) Log.e(TAG,
                    "无法下载，响应头 Content-Length 获取不到文件size。下载地址：" + mInfo.download_url);
            ProviderHelper.updateStatus(mContext, DownloadInfo.STATUS_FAIL, mInfo);
            return mInfo;
        }
        if (!TextUtils.isEmpty(responseInfo.contentType)) {
            mInfo.mime_type = responseInfo.contentType;
        }
        if (!TextUtils.isEmpty(responseInfo.eTag)) {
            mInfo.etag = responseInfo.eTag;
        }
        ProviderHelper.updateDownloadInfoPortion(mContext, mInfo);// 持久化下载信息

        // 补全请求信息
        httpInfo.total_bytes = mInfo.total_bytes;
        httpInfo.current_bytes = mInfo.current_bytes;

        // 请求服务器，获取输入流
        InputStream is = connection.getInputStream();
        if (is == null) {
            ProviderHelper.updateStatus(mContext, DownloadInfo.STATUS_FAIL, mInfo);
            return mInfo;
        }

        // 获取输出流
        //noinspection ResultOfMethodCallIgnored
        new File(mInfo.destination_path).mkdirs();
        File destFile = new File(mInfo.destination_path + mInfo.fileName);// 输出文件
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(destFile, true);
            final int defaultBufferSize = 1024 * 3;
            byte[] buf = new byte[defaultBufferSize];
            long size = mInfo.current_bytes;
            int len;
            while ((len = is.read(buf)) > 0) {
                // 响应task。cancel()
                Log.d(TAG, "isInterrupted():" + mThread.cancel);
                if (mThread.cancel) {
                    mThread.cancel = false;
                    return mInfo;
                }
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
            connection.close();
            Closeables.closeSafely(os);
        }
        return mInfo;

    }

}
