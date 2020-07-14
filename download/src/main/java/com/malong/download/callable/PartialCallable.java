package com.malong.download.callable;

import android.content.Context;
import android.util.Log;

import com.malong.download.DownloadInfo;
import com.malong.download.Http;

import java.io.File;
import java.util.concurrent.Callable;

/** 分片 */
public class PartialCallable implements Callable<DownloadInfo> {
    public static final String TAG = "【PartialCallable】";
    DownloadInfo mInfo;
    Context mContext;

    public PartialCallable(Context context, DownloadInfo info) {
        mContext = context;
        mInfo = info;
    }

    class SubCallable implements Callable {
        SubCallable(Context context, DownloadInfo info) {

        }
        @Override
        public Object call() throws Exception {
            return null;
        }
    }

    @Override
    public DownloadInfo call() throws Exception {
        Log.d(TAG, "PartialCallable:call()执行");
        Log.d(TAG, "mInfo.separate_num:" + mInfo.separate_num);
        // 先获取文件长度
        Http http = new Http(mContext, mInfo);
        mInfo.total_bytes = http.fetchLength();
        Log.d(TAG, "mInfo.total_bytes:" + mInfo.total_bytes);
        long partSize = mInfo.total_bytes / mInfo.separate_num+1;
        long index = 0;
        for (int i = 0; i < mInfo.separate_num; i++) {
            long start = i * partSize;
            long end = start + partSize - 1;
            if (i == mInfo.separate_num - 1) {
                end = mInfo.total_bytes;
            }





        }








//            HttpURLConnection connection = null;
//            int startIndex = 100;
//            int endIndex = 200;
//            InputStream inputStream = null;
//            FileOutputStream outputStream = null;
        String filepath = mInfo.destination_path;
//            File parentDir = new File(filepath).getParentFile();
//            assert parentDir != null;
//            //noinspection ResultOfMethodCallIgnored
//            parentDir.mkdirs();
        File destFile = new File(filepath);// 输出文件
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
