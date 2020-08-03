package com.malong.moses.connect;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.malong.moses.Constants;
import com.malong.moses.Request;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class Connection {
    public static final String TAG = "【Connection】";
    @SuppressWarnings("PointlessBooleanExpression")
    private static boolean DEBUG = Constants.DEBUG & true;
    private HttpInfo mInfo;
    private Context mContext;
    private HttpURLConnection conn;

    public Connection(Context context, HttpInfo info) {
        mContext = context;
        mInfo = info;
    }

    @Nullable
    public ResponseInfo getResponseInfo() {
        HttpURLConnection connection = null;
        try {
            URL download_url = new URL(mInfo.download_url);
            Log.d(TAG, "地址：" + mInfo.download_url);
            connection = (HttpURLConnection) (download_url.openConnection());
            connection.setConnectTimeout(10 * 1000);// 设置连接超时时间
            connection.setReadTimeout(5 * 60 * 1000);// 设置读取超时时间
            // 设置请求参数，即具体的 HTTP 方法
            connection.setRequestMethod("GET");// 只需要响应体(用HEAD不好使，腾讯处理不了这个请求方式)
            // fix 使用HttpClient时遇到的 java.net.SocketException: Socket closed异常.原因未知
            connection.setDoInput(true);// 使用 URL 连接进行输入，默认情况下是true;
            connection.setUseCaches(false);// 忽略缓存
            connection.setRequestProperty("Connection", "Keep-Alive");// 维持长连接
            connection.setRequestProperty("Content-type", "application/octet-stream");// 维持长连接
            connection.connect();
            // 检查响应码
            if (connection.getResponseCode() != 200/*HttpStatus.SC_OK*/
                    && connection.getResponseCode() != 206) {// 206大文件拆分状态码。腾讯云断点续传时的返回码
                if (DEBUG) Log.e(TAG, "获取响应头失败,响应码=" + connection.getResponseCode()
                        + "，链接地址=" + mInfo.download_url);

                Log.d(TAG, connection.getResponseMessage());
                Map<String, List<String>> headerFields = connection.getHeaderFields();
                for (Map.Entry<String, List<String>> map : headerFields.entrySet()) {
                    Log.d(TAG, map.getKey() + "+++++" + map.getValue());
                }
                return null;
            }

            ResponseInfo info = new ResponseInfo();
            info.acceptRanges = connection.getHeaderField("Accept-Ranges");
            info.server = connection.getHeaderField("Server");
            info.contentType = connection.getHeaderField("Content-Type");
            info.eTag = connection.getHeaderField("ETag");
            if (!TextUtils.isEmpty(info.eTag))
                info.eTag = info.eTag.replace("\"", "");// 获取缓存校验,若有""移除
            try {
                info.contentLength = Long.parseLong(connection.getHeaderField("Content-Length"));
            } catch (NumberFormatException e) {
                if (DEBUG) {
                    Log.d(TAG, "获取Content-Length失败");
                    e.printStackTrace();
                }
            }
            return info;
        } catch (MalformedURLException e) {
            if (DEBUG) {
                Log.e(TAG, " url解析失败，资源地址有问题，链接地址=" + mInfo.download_url);
                e.printStackTrace();
            }
        } catch (ProtocolException e) {
            if (DEBUG) {
                Log.e(TAG, " 连接服务端失败，检查网络状态/服务器，链接地址=" + mInfo.download_url);
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    public InputStream getInputStream() {
        conn = null;
        InputStream is = null;
        try {
            URL imageUrl = new URL(mInfo.download_url);
            conn = (HttpURLConnection) (imageUrl.openConnection());
            conn.setConnectTimeout(500);// 设置连接超时时间
            conn.setReadTimeout(500);// 设置读取超时时间
            // 处理局部下载，添加Range请求头
            if (mInfo.method == Request.METHOD_BREAKPOINT
                    && mInfo.current_bytes != 0
                    && mInfo.total_bytes > mInfo.current_bytes) {
                // 续传
                String range = "bytes=" + mInfo.current_bytes + "-" + mInfo.total_bytes;
                conn.setRequestProperty("Range", range);// 指定下载文件的指定位置
            } else if (mInfo.method == Request.METHOD_PARTIAL) {
                // 分片
                String range = "bytes=" + mInfo.start_index + "-" + mInfo.end_index;
                if (DEBUG) Log.d(TAG, "range=" + range);
                conn.setRequestProperty("Range", range);// 指定下载文件的指定位置
            }
            conn.connect();
            // 检查响应码
            if (conn.getResponseCode() != 200/*HttpStatus.SC_OK*/
                    && conn.getResponseCode() != 206) {// 206大文件拆分状态码。腾讯云断点续传时的返回码
                if (DEBUG) Log.e(TAG, "获取响应流失败,响应码=" + conn.getResponseCode()
                        + "，链接地址=" + mInfo.download_url);
                return null;
            }
            is = conn.getInputStream();
        } catch (Exception e) {
            if (DEBUG) {
                Log.e(TAG, "获取响应头失败,链接地址=" + mInfo.download_url);
                e.printStackTrace();
            }
            // getInputStream();是最后一句应该不需要close
//            Closeables.closeSafely(is);
//            is = null;
        }
        return is;
    }

    public void close() {
        if (null != conn) {
            conn.disconnect();
        }
    }
}
