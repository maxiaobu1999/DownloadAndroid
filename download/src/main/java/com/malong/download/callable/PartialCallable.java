package com.malong.download.callable;

import android.content.Context;
import android.util.Log;

import com.malong.download.DownloadInfo;
import com.malong.download.Http;
import com.malong.download.ProviderHelper;
import com.malong.download.utils.Closeables;

import java.io.File;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

/** 分片 */
public class PartialCallable implements Callable<DownloadInfo> {
    public static final String TAG = "【PartialCallable】";
    DownloadInfo mInfo;
    Context mContext;
    ExecutorService mExecutorService;

    public PartialCallable(Context context, DownloadInfo info, ExecutorService executorService) {
        mContext = context;
        mInfo = info;
        mExecutorService = executorService;
    }

    class SubCallable implements Callable<DownloadInfo> {
        public static final String TAG1 = "【SubCallable】";
        //        DownloadInfo mInfo1;
        Context mContext1;
        long start;
        long end;

        SubCallable(Context context, DownloadInfo info, long start, long end) {
            mContext1 = context;
//            mInfo1 = info;
            this.start = start;
            this.end = end;
        }

        @Override
        public DownloadInfo call() throws Exception {
            Log.d(TAG1, "call()执行");
            Log.d(TAG1, "start:" + start);
            Log.d(TAG1, "end:" + end);
            //noinspection ResultOfMethodCallIgnored
            new File(mInfo.destination_path).mkdirs();
            File destFile = new File(mInfo.destination_path + mInfo.fileName);// 输出文件
            // 文件存在
            if (destFile.exists()) {
                Log.d(TAG, "destFile.length():" + destFile.length());
            }


            Http http = new Http(mContext1, mInfo, start, end);

            InputStream is = null;
            RandomAccessFile raf = null;
            try {
                is = http.getDownloadStream();
                if (is == null) {
                    Log.d(TAG, "http.getCode():" + http.getCode());
                    mInfo.status = DownloadInfo.STATUS_FAIL;
                    ProviderHelper.onStatusChange(mContext, mInfo);
                    return mInfo;
                }
                raf = new RandomAccessFile(destFile, "rw");
//                long l = FileUtils.copyStream(is, raf, 0);
                raf.seek(start);
                final int defaultBufferSize = 1024 * 3;
                byte[] buf = new byte[defaultBufferSize];
                long size = 0;
                int len;
                while ((len = is.read(buf)) > 0) {
                    raf.write(buf, 0, len);
                    size += len;
                    updateProcess(len);
                }

//                mInfo.current_bytes += len;
//                ProviderHelper.updateProcess(mContext, mInfo);
//                Log.d(TAG, "size:" + size);


//                final int defaultBufferSize = 1024 * 3;
//                byte[] buf = new byte[defaultBufferSize];
//                long size = mInfo.current_bytes;
//                int len;
//                while ((len = is.read(buf)) > 0) {
//                    raf.write(buf, 0, len);
//                    size += len;
//                    mInfo.current_bytes = size;
//                    ProviderHelper.updateProcess(mContext, mInfo);
//                }
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
                Closeables.closeSafely(raf);
            }


            return null;
        }
    }

    public synchronized void updateProcess(int add) {
        mInfo.current_bytes += add;
        ProviderHelper.updateProcess(mContext, mInfo);
        Log.d(TAG, "size:" + mInfo.current_bytes);
    }

    @Override
    public DownloadInfo call() throws Exception {
        Log.d(TAG, "PartialCallable:call()执行");
        Log.d(TAG, "mInfo.separate_num:" + mInfo.separate_num);
        // 先获取文件长度
        Http http = new Http(mContext, mInfo);
        mInfo.total_bytes = http.fetchLength();
        Log.d(TAG, "mInfo.total_bytes:" + mInfo.total_bytes);
        long partSize = mInfo.total_bytes / mInfo.separate_num;
        for (int i = 0; i < mInfo.separate_num; i++) {
            long start = i * partSize;
            long end = start + partSize - 1;
            if (i == mInfo.separate_num - 1) {
                end = mInfo.total_bytes;
            }

            Callable<DownloadInfo> callable = new SubCallable(mContext, mInfo, start, end);
            FutureTask<DownloadInfo> task = new FutureTask<>(callable);
            mExecutorService.submit(task);


        }


//            HttpURLConnection connection = null;
//            int startIndex = 100;
//            int endIndex = 200;
//            InputStream inputStream = null;
//            FileOutputStream outputStream = null;
//            File parentDir = new File(filepath).getParentFile();
//            assert parentDir != null;
//            //noinspection ResultOfMethodCallIgnored
//            parentDir.mkdirs();
//
//
////            long size = 0;
//        HttpURLConnection conn = null;
//        InputStream is = null;
//        RandomAccessFile os = null;
//
//        try {
//            URL imageUrl = new URL(mInfo.download_url);
//            conn = (HttpURLConnection) (imageUrl.openConnection());
//            conn.setConnectTimeout(10000); // SUPPRESS CHECKSTYLE
//            conn.setReadTimeout(10000); // SUPPRESS CHECKSTYLE
//
////
////                if (destFile.exists()) {
////                    // 文件存在
////                    // 断点续传
////                    Log.d(TAG, "文件存在");
////                    long length = destFile.length();
////                    Log.d(TAG, "文件length:" + length);
////
//
//
////                conn.setRequestProperty("Range", "bytes=" + 100 + "-" + 200);// 指定下载文件的指定位置
//
////                }
//
//
//            conn.connect();
//            if (conn.getResponseCode() == 200/*HttpStatus.SC_OK*/
//                    || conn.getResponseCode() == 206) {// 206大文件拆分状态码。腾讯云断点续传时的返回码
//                is = conn.getInputStream();
//                if (null != is) {
//                    os = new RandomAccessFile(destFile, "rw");
//                    long l = FileUtils.copyStream(is, os, 0);
//                    Log.d(TAG, "l:" + l);
//                }
//            } else {
//                Log.d(TAG, "conn.getResponseCode():" + conn.getResponseCode());
//                Log.d(TAG, conn.getResponseMessage());
//            }
//
//        } catch (InterruptedIOException | OutOfMemoryError | MalformedURLException e) {
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            Closeables.closeSafely(is);
//            if (null != conn) {
//                conn.disconnect();
//            }
//            Closeables.closeSafely(os);
//        }

        return null;
    }
}
