package com.malong.moses;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Request {
    /** 啥记录没有 */
    public static final int STATUS_NONE = 0;
    /** 准备开始下载 */
    public static final int STATUS_PENDING = 100;
    /** 正在下载 */
    public static final int STATUS_RUNNING = 110;
    /** 暂停 */
    public static final int STATUS_PAUSE = 120;
    /** 下载完成 */
    public static final int STATUS_SUCCESS = 200;
    /** 下载失败 */
    public static final int STATUS_FAIL = 300;
    /** 取消 删除记录 保留文件 */
    public static final int STATUS_CANCEL = 999;

//    /**
//     * This request couldn't be parsed. This is also used when processing
//     * requests with unknown/unsupported URI schemes.
//     */
//    public static final int STATUS_BAD_REQUEST = 400;
//
//    /**
//     * This doDownload can't be performed because the content type cannot be
//     * handled.
//     */
//    public static final int STATUS_NOT_ACCEPTABLE = 406;
//
//    /**
//     * This doDownload cannot be performed because the length cannot be
//     * determined accurately. This is the code for the HTTP error "Length
//     * Required", which is typically used when making requests that require
//     * a content length but don't have one, and it is also used in the
//     * client when a response is received whose length cannot be determined
//     * accurately (therefore making it impossible to know when a doDownload
//     * completes).
//     */
//    public static final int STATUS_LENGTH_REQUIRED = 411;
//
//    /**
//     * This doDownload was interrupted and cannot be resumed.
//     * This is the code for the HTTP error "Precondition Failed", and it is
//     * also used in situations where the client doesn't have an ETag at all.
//     */
//    public static final int STATUS_PRECONDITION_FAILED = 412;
//
//    /** 下载被取消 */
//    public static final int STATUS_CANCELED = 490;
//
//    /**
//     * This doDownload has completed with an error.
//     * Warning: there will be other status values that indicate errors in
//     * the future. Use isStatusError() to capture the entire category.
//     */
//    public static final int STATUS_UNKNOWN_ERROR = 491;
//
//    /**
//     * This doDownload couldn't be completed because of a storage issue.
//     * Typically, that's because the filesystem is missing or full.
//     * Use the more specific {@link #STATUS_INSUFFICIENT_SPACE_ERROR}
//     * and {@link #STATUS_DEVICE_NOT_FOUND_ERROR} when appropriate.
//     */
//    public static final int STATUS_FILE_ERROR = 492;
//
//    /**
//     * This doDownload couldn't be completed because of an HTTP
//     * redirect response that the doDownload manager couldn't
//     * handle.
//     */
//    public static final int STATUS_UNHANDLED_REDIRECT = 493;
//
//    /**
//     * This doDownload couldn't be completed because of an
//     * unspecified unhandled HTTP code.
//     */
//    public static final int STATUS_UNHANDLED_HTTP_CODE = 494;
//
//    /**
//     * This doDownload couldn't be completed because of an
//     * error receiving or processing data at the HTTP level.
//     */
//    public static final int STATUS_HTTP_DATA_ERROR = 495;
//
//    /**
//     * This doDownload couldn't be completed because of an
//     * HttpException while setting up the request.
//     */
//    public static final int STATUS_HTTP_EXCEPTION = 496;
//
//    /**
//     * This doDownload couldn't be completed because there were
//     * too many redirects.
//     */
//    public static final int STATUS_TOO_MANY_REDIRECTS = 497;
//
//    /**
//     * This doDownload couldn't be completed due to insufficient storage
//     * space.  Typically, this is because the SD card is full.
//     */
//    public static final int STATUS_INSUFFICIENT_SPACE_ERROR = 498;
//
//    /**
//     * This doDownload couldn't be completed because no external storage
//     * device was found.  Typically, this is because the SD card is not
//     * mounted.
//     */
//    public static final int STATUS_DEVICE_NOT_FOUND_ERROR = 499;


    /** 下载完成不保存记录 */
    public static final int COMPLETE_NOT_SAVE = 1;


    /** 下载方式：删除旧文件，重新下载 。服务端不支持局部下载时使用 */
    public static final int METHOD_COMMON = 0;
    /** 下载方式：断点续传 */
    public static final int METHOD_BREAKPOINT = 1;

    /** 下载方式：分片下载 */
    public static final int METHOD_PARTIAL = 2;

    /** id主键 */
    public int id;
    /** 下载链接 */
    public String download_url;
    /** 保存地址uri */
    public String destination_uri;
    /** 保存地址路径 */
    public String destination_path;
    /** 保存地址路径 */
    public String fileName;


    /** 任务状态： */
    public int status;
    /** 下载方式 : 重新下载 断点续传 差量下载 分片下载 */
    public int method;
    /** 下载完成 : 不保存记录 下载中心可见 不可见 */
    public int complete;
    /** 下载的文件总大小 BIGINT */
    public long total_bytes;
    /** 当前下载的文件大小 BIGINT */
    public long current_bytes;
    /** 进度阀值 */
    public long min_progress_time = MosesConfig.MIN_PROGRESS_TIME;
    /** 进度阀值 */
    public long min_progress_step = MosesConfig.MIN_PROGRESS_STEP;
    /** 下载数据的MIME类型 TEXT */
    public String mime_type;

    /** 缓存校验md5 TEXT */
    public String etag;

    /** 分片数量 */
    public int separate_num;


    /** 请求头 json */
    public String header;

    /** 断点续传 range 起始位置 */
    public long mRangeStartByte = 0;
    /** 断点续传 range 结束位置 */
    public long mRangeEndByte = -1;

    @NonNull
    public static List<Request> readDownloadInfos(Context context, @Nullable Cursor cursor) {
        ArrayList<Request> infoList = new ArrayList<>();
        if (cursor == null) {
            return infoList;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            Request info = new Request();
            if (cursor.getColumnIndex(Constants._ID) != -1)
                info.id = cursor.getInt(cursor.getColumnIndexOrThrow(Constants._ID));
            if (cursor.getColumnIndex(Constants.COLUMN_STATUS) != -1)
                info.status = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.COLUMN_STATUS));
            if (cursor.getColumnIndex(Constants.COLUMN_DOWNLOAD_URL) != -1)
                info.download_url = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_DOWNLOAD_URL));
            if (cursor.getColumnIndex(Constants.COLUMN_DESTINATION_URI) != -1)
                info.destination_uri = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_DESTINATION_URI));
            if (cursor.getColumnIndex(Constants.COLUMN_DESTINATION_PATH) != -1)
                info.destination_path = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_DESTINATION_PATH));
            if (cursor.getColumnIndex(Constants.COLUMN_FILE_NAME) != -1)
                info.fileName = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_FILE_NAME));
            if (cursor.getColumnIndex(Constants.COLUMN_MIME_TYPE) != -1)
                info.mime_type = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_MIME_TYPE));
            if (cursor.getColumnIndex(Constants.COLUMN_METHOD) != -1)
                info.method = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.COLUMN_METHOD));
            if (cursor.getColumnIndex(Constants.COLUMN_HEADER) != -1)
                info.header = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_HEADER));
            if (cursor.getColumnIndex(Constants.COLUMN_ETAG) != -1)
                info.etag = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_ETAG));
            if (cursor.getColumnIndex(Constants.COLUMN_TOTAL_BYTES) != -1)
                info.total_bytes = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.COLUMN_TOTAL_BYTES));
            if (cursor.getColumnIndex(Constants.COLUMN_CURRENT_BYTES) != -1)
                info.current_bytes = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.COLUMN_CURRENT_BYTES));
            info.min_progress_step = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.COLUMN_MIN_PROGRESS_STEP));
            info.min_progress_time = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.COLUMN_MIN_PROGRESS_TIME));

            if (cursor.getColumnIndex(Constants.COLUMN_SEPARATE_NUM) != -1)
                info.separate_num = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.COLUMN_SEPARATE_NUM));
            infoList.add(info);
        }
        return infoList;
    }


    public ContentValues info2ContentValues() {
        Request info = this;
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_STATUS, info.status);
        values.put(Constants.COLUMN_DOWNLOAD_URL, info.download_url);
        if (!TextUtils.isEmpty(info.download_url))
            values.put(Constants.COLUMN_DESTINATION_URI, info.destination_uri);
        if (!TextUtils.isEmpty(info.destination_path))
            values.put(Constants.COLUMN_DESTINATION_PATH, info.destination_path);
        values.put(Constants.COLUMN_FILE_NAME, info.fileName);
        values.put(Constants.COLUMN_METHOD, info.method);
        values.put(Constants.COLUMN_HEADER, info.header);
        if (info.total_bytes != 0)
            values.put(Constants.COLUMN_TOTAL_BYTES, info.total_bytes);
        if (info.current_bytes != 0)
            values.put(Constants.COLUMN_CURRENT_BYTES, info.current_bytes);
        if (!TextUtils.isEmpty(info.mime_type))
            values.put(Constants.COLUMN_MIME_TYPE, info.mime_type);
        if (!TextUtils.isEmpty(info.etag))
            values.put(Constants.COLUMN_ETAG, info.etag);
        if (min_progress_time != 0) {
            values.put(Constants.COLUMN_MIN_PROGRESS_TIME, info.min_progress_time);
        }
        if (min_progress_step != 0) {
            values.put(Constants.COLUMN_MIN_PROGRESS_STEP, info.min_progress_step);
        }

        values.put(Constants.COLUMN_SEPARATE_NUM, info.separate_num);

        return values;
    }


    public static class Builder {
        private Request mInfo;

        public Builder() {
            mInfo = new Request();
            // 设置默认值
            mInfo.destination_uri = "";
            mInfo.destination_path = "";

            mInfo.status = Request.STATUS_NONE;
            mInfo.method = Request.METHOD_COMMON;
            mInfo.total_bytes = 0;
            mInfo.current_bytes = 0;
        }

        public Request build() {
            // 下载地址必须有
            if (TextUtils.isEmpty(mInfo.download_url)) {
                throw new RuntimeException("下载地址必须有");
            }
            if (TextUtils.isEmpty(mInfo.destination_uri) &&
                    TextUtils.isEmpty(mInfo.destination_path)) {
                throw new RuntimeException("没有设置保存路径，uri或者path必须有一个");
            }
            // 文件名
            if (TextUtils.isEmpty(mInfo.fileName)) {
                throw new RuntimeException("没有设置文件名");
            }
            if (!TextUtils.isEmpty(mInfo.destination_uri)
                    && mInfo.method == Request.METHOD_BREAKPOINT) {

            }
            return mInfo;
        }


        /** 指定下载地址 */
        public Builder setDownloadUrl(@NonNull String url) {
            if (TextUtils.isEmpty(url)) {
                throw new RuntimeException("下载地址不能为空");
            }
            mInfo.download_url = url;
            return this;
        }

        /** 设置文件路径 */
        public Builder setDescription_path(@NonNull String path) {
            if (TextUtils.isEmpty(path)) {
                throw new RuntimeException("保存路径不能为空");
            }
            // 保证以【/】结尾
            if (!path.endsWith(File.separator)) {
                path = path + File.separator;
            }
            mInfo.destination_path = path;
            return this;
        }

        /** 设置文件uri */
        public Builder setDescription_uri(@NonNull Uri uri) {
            if (TextUtils.isEmpty(uri.toString())) {
                throw new RuntimeException("保存Uri不能为空");
            }
            mInfo.destination_uri = uri.toString();
            return this;
        }

        /** 设置文件名 */
        public Builder setFileName(@NonNull String name) {
            if (TextUtils.isEmpty(name)) {
                throw new RuntimeException("文件名不能为空");
            }
            mInfo.fileName = name;
            return this;
        }

        // 设置下载方式
        public Builder setMethod(int method) {
            mInfo.method = method;
            return this;
        }

        // 设置分片数量
        public Builder setSeparate_num(int separate_num) {
            mInfo.separate_num = separate_num;
            return this;
        }

        /** 设置进度通知的间隔阀值 */
        public Builder setMin_progress_time(int min_progress_time) {
            mInfo.min_progress_time = min_progress_time;
            return this;
        }

        /** 设置进度通知的字节阀值 */
        public Builder setMin_progress_step(int min_progress_step) {
            mInfo.min_progress_step = min_progress_step;
            return this;
        }


    }

}

