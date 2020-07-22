package com.malong.moses.callable;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.malong.moses.CancelableThread;
import com.malong.moses.Constants;
import com.malong.moses.DownloadTask;
import com.malong.moses.ProviderHelper;
import com.malong.moses.connect.Connection;
import com.malong.moses.connect.HttpInfo;
import com.malong.moses.connect.ResponseInfo;
import com.malong.moses.utils.Closeables;
import com.malong.moses.utils.FileUtils;
import com.malong.moses.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.Callable;

/** 普通下载 删除旧文件，重新下载 。服务端不支持局部下载时使用 */
public class DownloadCallable implements Callable<DownloadTask> {
    public static final String TAG = "【DownloadCallable】";
    @SuppressWarnings("PointlessBooleanExpression")
    private static boolean DEBUG = Constants.DEBUG & true;
    private DownloadTask mInfo;
    private Context mContext;

    public DownloadCallable(Context context, DownloadTask info) {
        mContext = context;
        mInfo = info;
    }

    @Override
    public DownloadTask call() {
        if (DEBUG) Log.d(TAG, "call()执行");
        CancelableThread mThread = (CancelableThread) Thread.currentThread();

        HttpInfo httpInfo = new HttpInfo();
        httpInfo.download_url = mInfo.download_url;
        httpInfo.destination_uri = mInfo.destination_uri;
        httpInfo.destination_path = mInfo.destination_path;
        httpInfo.fileName = mInfo.fileName;
        httpInfo.method = mInfo.method;

        Connection connection = new Connection(mContext, httpInfo);
        ResponseInfo responseInfo = connection.getResponseInfo();
        if (responseInfo == null) {// 下载失败
            ProviderHelper.updateStatus(mContext, DownloadTask.STATUS_FAIL, mInfo);
            return mInfo;
        }
        if (!TextUtils.isEmpty(responseInfo.contentType)) {
            mInfo.mime_type = responseInfo.contentType;
        }
        if (!TextUtils.isEmpty(responseInfo.eTag)) {
            mInfo.etag = responseInfo.eTag;
        }
        if (responseInfo.contentLength != 0) {
            mInfo.total_bytes = responseInfo.contentLength;
        }
        ProviderHelper.updateDownloadInfoPortion(mContext, mInfo);// 持久化下载信息



        // TODO: 2020-07-21 适配URI
        // 删除掉过去下载的文件（eg：下一半的重新下载）
        File destFile = new File(mInfo.destination_path + mInfo.fileName);// 输出文件
        if (destFile.exists()) {
            if (DEBUG) Log.d(TAG, "删除残留文件：" + destFile.getAbsolutePath());
            FileUtils.deleteFile(destFile);
        }
        // 请求服务器，获取输入流
        InputStream is = connection.getInputStream();
        if (is == null) {
            ProviderHelper.updateStatus(mContext, DownloadTask.STATUS_FAIL, mInfo);
            return mInfo;
        }


        FileOutputStream os = null;
        try {
            os = Utils.getOutputStream(mContext, mInfo);
            if (os == null) {
                if (DEBUG) Log.e(TAG, "下载失败,存储路径不可写");
                ProviderHelper.updateStatus(mContext, DownloadTask.STATUS_FAIL, mInfo);
                return mInfo;
            }
            final int defaultBufferSize = 1024 * 3;
            byte[] buf = new byte[defaultBufferSize];
            long size = 0;
            int len;
            while ((len = is.read(buf)) > 0) {
                // 响应task.cancelDownload()
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
            mInfo.status = DownloadTask.STATUS_SUCCESS;
            ProviderHelper.onStatusChange(mContext, mInfo);
        } catch (InterruptedIOException e) {
            // task被取消,finally会执行
        } catch (Exception e) {
            mInfo.status = DownloadTask.STATUS_FAIL;
            ProviderHelper.onStatusChange(mContext, mInfo);
            e.printStackTrace();
        } finally {
            Closeables.closeSafely(is);
            connection.close();
            Closeables.closeSafely(os);
        }
        return mInfo;
    }

}
