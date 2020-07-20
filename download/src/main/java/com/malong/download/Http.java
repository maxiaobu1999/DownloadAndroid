package com.malong.download;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Http {
    public static final String TAG = "【Http】";
    @SuppressWarnings("PointlessBooleanExpression")
    private static boolean DEBUG = Constants.DEBUG & true;
    HttpInfo mInfo;
    Context mContext;
    private HttpURLConnection conn;
    /** 下载的长度 */
    private long contentLength;
    /** 状态码 */
    private int code = -1;
    /** 响应头ETag : "4df4d61142e773a16769473cf2654b71" */
    private String mETag;
//    long start = 0;
//    long end = 0;

    public Http(Context context, HttpInfo info) {
        mContext = context;
        mInfo = info;
    }

    public InputStream getDownloadStream() {
        conn = null;
        InputStream is = null;
        try {
            URL imageUrl = new URL(mInfo.download_url);
            conn = (HttpURLConnection) (imageUrl.openConnection());
            conn.setConnectTimeout(10000); // SUPPRESS CHECKSTYLE
            conn.setReadTimeout(10000); // SUPPRESS CHECKSTYLE


            // 续传
            if (mInfo.method == DownloadInfo.METHOD_BREAKPOINT
                    && mInfo.current_bytes != 0
                    && mInfo.total_bytes > mInfo.current_bytes) {
                // 部分下载，需要添加Range请求头
                String range = "bytes=" + mInfo.current_bytes + "-" + mInfo.total_bytes;
                conn.setRequestProperty("Range", range);// 指定下载文件的指定位置
            }

            // 分片
            if (mInfo.method == DownloadInfo.METHOD_PARTIAL) {
                String range = "bytes=" + mInfo.start_index + "-" + mInfo.end_index;
                if (DEBUG) Log.d(TAG, "range=" + range);
                conn.setRequestProperty("Range", range);// 指定下载文件的指定位置
            }

            conn.connect();
            code = conn.getResponseCode();
            if (BuildConfig.DEBUG) Log.d(TAG, "conn.getResponseCode():" + code);
            if (conn.getResponseCode() == 200/*HttpStatus.SC_OK*/
                    || conn.getResponseCode() == 206) {// 206大文件拆分状态码。腾讯云断点续传时的返回码
                contentLength = conn.getContentLength();
                if (DEBUG) Log.d(TAG, "contentLength:" + contentLength);
                mETag = conn.getHeaderField("ETag").replace("\"", "");// 获取响应
                Log.d(TAG, "+++++" + mETag);
                is = conn.getInputStream();
            }
        } catch (InterruptedIOException | OutOfMemoryError | MalformedURLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return is;
    }

    public long getContentLength() {
        return contentLength;
    }

    public String getETag() {
        return mETag;
    }


    public long fetchLength() {
        HttpURLConnection conn = null;
        try {
            URL imageUrl = new URL(mInfo.download_url);
            conn = (HttpURLConnection) (imageUrl.openConnection());
            conn.setConnectTimeout(10000); // SUPPRESS CHECKSTYLE
            conn.setReadTimeout(10000); // SUPPRESS CHECKSTYLE
            conn.connect();
            if (conn.getResponseCode() == 200/*HttpStatus.SC_OK*/
                    || conn.getResponseCode() == 206) {// 206大文件拆分状态码。腾讯云断点续传时的返回码
                contentLength = conn.getContentLength();
                mETag = conn.getHeaderField("ETag").replace("\"", "");// 获取响应
            } else {
                if (DEBUG) Log.d(TAG, "conn.getResponseCode():" + conn.getResponseCode());
                if (DEBUG) Log.d(TAG, conn.getResponseMessage());
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (conn != null)
                conn.disconnect();
        }
        return contentLength;

    }

    public int getCode() {
        return code;
    }


    public void close() {
        if (null != conn) {
            conn.disconnect();
        }
    }


}
