package com.malong.download;

import android.content.Context;
import android.util.Log;

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

    public long getContentLengh() {
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
