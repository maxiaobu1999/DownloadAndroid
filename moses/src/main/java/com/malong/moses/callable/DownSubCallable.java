package com.malong.moses.callable;

import android.content.Context;
import android.util.Log;

import com.malong.moses.CancelableThread;
import com.malong.moses.Constants;
import com.malong.moses.Request;
import com.malong.moses.block.BlockInfo;
import com.malong.moses.block.BlockProviderHelper;
import com.malong.moses.connect.Connection;
import com.malong.moses.connect.HttpInfo;
import com.malong.moses.utils.Closeables;

import java.io.File;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.util.concurrent.Callable;

public class DownSubCallable implements Callable<BlockInfo> {
    public static final String TAG = "【DownSubCallable】";
    @SuppressWarnings("PointlessBooleanExpression")
    private static boolean DEBUG = Constants.DEBUG & true;
    private BlockInfo mInfo;
    private Context mContext;

    public DownSubCallable(Context context, BlockInfo info) {
        mContext = context;
        mInfo = info;

    }

    @Override
    public BlockInfo call() {
        if (DEBUG) Log.d(TAG, "call()执行");
        CancelableThread mThread = (CancelableThread) Thread.currentThread();
        //noinspection ResultOfMethodCallIgnored
        new File(mInfo.destination_path).mkdirs();
        File destFile = new File(mInfo.destination_path + mInfo.fileName);// 输出文件
        HttpInfo httpInfo = new HttpInfo();
        httpInfo.download_url = mInfo.download_url;
        httpInfo.destination_uri = mInfo.destination_uri;
        httpInfo.destination_path = mInfo.destination_path;
        httpInfo.fileName = mInfo.fileName;
        httpInfo.method = Request.METHOD_PARTIAL;
        httpInfo.total_bytes = mInfo.total_bytes;
        httpInfo.current_bytes = mInfo.current_bytes;
        httpInfo.start_index = mInfo.start_index + mInfo.current_bytes;// 续传
        httpInfo.end_index = mInfo.end_index;
        // 已经下载完的
        if (httpInfo.start_index >= httpInfo.end_index) {
            BlockProviderHelper.updatePartialStatus(mContext, BlockInfo.STATUS_SUCCESS, mInfo);
            return mInfo;
        }

        Connection connection = new Connection(mContext, httpInfo);
        // 请求服务器，获取输入流
        InputStream is = connection.getInputStream();
        if (is == null) {
            BlockProviderHelper.updatePartialStatus(mContext, BlockInfo.STATUS_FAIL, mInfo);
            return mInfo;
        }
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(destFile, "rw");
            raf.seek(mInfo.start_index + mInfo.current_bytes);
            final int defaultBufferSize = 4096;
            byte[] buf = new byte[defaultBufferSize];
            long size = mInfo.current_bytes;
            int len;
            long mBytesNotified = 0;
            long mTimeLastNotification = 0;
            while ((len = is.read(buf)) > 0) {
                // 响应task。cancelDownload()
                if (mThread.cancel) {
                    mThread.cancel = false;
                    if (DEBUG) Log.d(TAG, "任务被取消");
                    return mInfo;
                }
                raf.write(buf, 0, len);
                size += len;
                mInfo.current_bytes = size;
//                if (DEBUG) Log.d(TAG, " mInfo.current_bytes=" + mInfo.current_bytes);
                long now = System.currentTimeMillis();
                if (mInfo.current_bytes - mBytesNotified
                        > mInfo.min_progress_step
                        && now - mTimeLastNotification
                        > mInfo.min_progress_time) {
                    mBytesNotified = mInfo.current_bytes;
                    mTimeLastNotification = now;
                    BlockProviderHelper.updatePartialProcess(mContext, mInfo);
                }
            }
            // 下载完成
            BlockProviderHelper.updatePartialProcess(mContext, mInfo);
            BlockProviderHelper.updatePartialStatus(mContext, Request.STATUS_SUCCESS, mInfo);
        } catch (InterruptedIOException e) {
            // 下载被取消,finally会执行
        } catch (Exception e) {
            e.printStackTrace();
            BlockProviderHelper.updatePartialStatus(mContext, Request.STATUS_FAIL, mInfo);
        } finally {
            Closeables.closeSafely(is);
            connection.close();
            Closeables.closeSafely(raf);
        }
        return null;
    }
//    /**
//     * Report download progress through the database if necessary.
//     *
//     * @param state      state
//     * @param innerState innerState
//     */
//    private void reportProgress(State state, InnerState innerState) {
//        long now = mISystemFacade.currentTimeMillis();
//        if (innerState.mBytesSoFar - innerState.mBytesNotified
//                > Constants.MIN_PROGRESS_STEP
//                && now - innerState.mTimeLastNotification
//                > Constants.MIN_PROGRESS_TIME) {
//            ContentValues values = new ContentValues();
//            values.put(Downloads.Impl.COLUMN_CURRENT_BYTES, innerState.mBytesSoFar);
//            mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
//            innerState.mBytesNotified = innerState.mBytesSoFar;
//            innerState.mTimeLastNotification = now;
//        }
//    }

}
