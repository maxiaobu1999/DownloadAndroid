package com.malong.download;

import android.content.Context;
import android.util.Log;

import com.malong.download.utils.Closeables;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Http {
    public static final String TAG = "【DownloadCallable】";
    DownloadInfo mInfo;
    Context mContext;
    private HttpURLConnection conn;
    /** 下载的长度 */
    private int contentLength = -1;
    /** 状态码 */
    private int code = -1;

    public Http(Context context, DownloadInfo info) {
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

            int start = 0;
            int end = 0;
            if (mInfo.method == DownloadInfo.METHOD_BREAKPOINT
                    && mInfo.current_bytes != 0
                    && mInfo.total_bytes > mInfo.current_bytes) {
                // 部分下载，需要添加Range请求头
                String range = "byte=s" + mInfo.current_bytes + "-" + mInfo.total_bytes;
                conn.setRequestProperty("Range", range);// 指定下载文件的指定位置
            }

            conn.connect();
            if (conn.getResponseCode() == 200/*HttpStatus.SC_OK*/
                    || conn.getResponseCode() == 206) {// 206大文件拆分状态码。腾讯云断点续传时的返回码
                contentLength = conn.getContentLength();
                is = conn.getInputStream();
            } else {
                Log.d(TAG, "conn.getResponseCode():" + conn.getResponseCode());
                Log.d(TAG, conn.getResponseMessage());
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


    public long fetchLength() {
        long length = -1;
        HttpURLConnection conn = null;
        try {
            URL imageUrl = new URL(mInfo.download_url);
            conn = (HttpURLConnection) (imageUrl.openConnection());
            conn.setConnectTimeout(10000); // SUPPRESS CHECKSTYLE
            conn.setReadTimeout(10000); // SUPPRESS CHECKSTYLE
            conn.connect();
            if (conn.getResponseCode() == 200/*HttpStatus.SC_OK*/
                    || conn.getResponseCode() == 206) {// 206大文件拆分状态码。腾讯云断点续传时的返回码
                length = conn.getContentLength();
            } else {
                Log.d(TAG, "conn.getResponseCode():" + conn.getResponseCode());
                Log.d(TAG, conn.getResponseMessage());
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (conn != null)
                conn.disconnect();
        }
        return length;

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
