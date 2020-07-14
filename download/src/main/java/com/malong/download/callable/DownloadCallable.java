package com.malong.download.callable;

import android.content.Context;
import android.util.Log;

import com.malong.download.Constants;
import com.malong.download.DownloadInfo;
import com.malong.download.Http;
import com.malong.download.ProviderHelper;
import com.malong.download.utils.Closeables;
import com.malong.download.utils.FileUtils;
import com.malong.download.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.Callable;

/** 普通下载 */
public class DownloadCallable implements Callable<DownloadInfo> {
    public static final String TAG = "【DownloadCallable】";
    private static boolean DEBUG = Constants.DEBUG;
    DownloadInfo mInfo;
    Context mContext;

    public DownloadCallable(Context context, DownloadInfo info) {
        mContext = context;
        mInfo = info;
    }

    @Override
    public DownloadInfo call() {
        if (DEBUG) Log.d(TAG, "call()执行");
        // 删除掉过去下载的文件（eg：下一半的重新下载）
        File destFile = new File(mInfo.destination_path + mInfo.fileName);// 输出文件
        if (destFile.exists()) {
            FileUtils.deleteFile(destFile);
        }
        Http http = new Http(mContext, mInfo);
        // 下载内容的长度
        mInfo.total_bytes = http.getContentLength();
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
            os = Utils.getOutputStream(mContext, mInfo);
            final int defaultBufferSize = 1024 * 3;
            byte[] buf = new byte[defaultBufferSize];
            long size = 0;
            int len;
            while ((len = is.read(buf)) > 0) {
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

    private void onSuccess() {
        mInfo.status = DownloadInfo.STATUS_SUCCESS;

    }

    private void onFail() {
        mInfo.status = DownloadInfo.STATUS_FAIL;

    }
}
