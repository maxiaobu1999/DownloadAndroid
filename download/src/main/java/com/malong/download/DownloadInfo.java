package com.malong.download;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DownloadInfo {
    /** 过去没有开始下载 */
    public static final int STATUS_PENDING = 190;
    /** 正在下载 */
    public static final int STATUS_RUNNING = 192;
    /** 停止 */
    public static final int STATUS_STOP = 193;
    /** 下载完成 */
    public static final int STATUS_SUCCESS = 200;
    /** 下载失败 */
    public static final int STATUS_FAIL = 300;

//    /**
//     * This request couldn't be parsed. This is also used when processing
//     * requests with unknown/unsupported URI schemes.
//     */
//    public static final int STATUS_BAD_REQUEST = 400;
//
//    /**
//     * This download can't be performed because the content type cannot be
//     * handled.
//     */
//    public static final int STATUS_NOT_ACCEPTABLE = 406;
//
//    /**
//     * This download cannot be performed because the length cannot be
//     * determined accurately. This is the code for the HTTP error "Length
//     * Required", which is typically used when making requests that require
//     * a content length but don't have one, and it is also used in the
//     * client when a response is received whose length cannot be determined
//     * accurately (therefore making it impossible to know when a download
//     * completes).
//     */
//    public static final int STATUS_LENGTH_REQUIRED = 411;
//
//    /**
//     * This download was interrupted and cannot be resumed.
//     * This is the code for the HTTP error "Precondition Failed", and it is
//     * also used in situations where the client doesn't have an ETag at all.
//     */
//    public static final int STATUS_PRECONDITION_FAILED = 412;
//
//    /** 下载被取消 */
//    public static final int STATUS_CANCELED = 490;
//
//    /**
//     * This download has completed with an error.
//     * Warning: there will be other status values that indicate errors in
//     * the future. Use isStatusError() to capture the entire category.
//     */
//    public static final int STATUS_UNKNOWN_ERROR = 491;
//
//    /**
//     * This download couldn't be completed because of a storage issue.
//     * Typically, that's because the filesystem is missing or full.
//     * Use the more specific {@link #STATUS_INSUFFICIENT_SPACE_ERROR}
//     * and {@link #STATUS_DEVICE_NOT_FOUND_ERROR} when appropriate.
//     */
//    public static final int STATUS_FILE_ERROR = 492;
//
//    /**
//     * This download couldn't be completed because of an HTTP
//     * redirect response that the download manager couldn't
//     * handle.
//     */
//    public static final int STATUS_UNHANDLED_REDIRECT = 493;
//
//    /**
//     * This download couldn't be completed because of an
//     * unspecified unhandled HTTP code.
//     */
//    public static final int STATUS_UNHANDLED_HTTP_CODE = 494;
//
//    /**
//     * This download couldn't be completed because of an
//     * error receiving or processing data at the HTTP level.
//     */
//    public static final int STATUS_HTTP_DATA_ERROR = 495;
//
//    /**
//     * This download couldn't be completed because of an
//     * HttpException while setting up the request.
//     */
//    public static final int STATUS_HTTP_EXCEPTION = 496;
//
//    /**
//     * This download couldn't be completed because there were
//     * too many redirects.
//     */
//    public static final int STATUS_TOO_MANY_REDIRECTS = 497;
//
//    /**
//     * This download couldn't be completed due to insufficient storage
//     * space.  Typically, this is because the SD card is full.
//     */
//    public static final int STATUS_INSUFFICIENT_SPACE_ERROR = 498;
//
//    /**
//     * This download couldn't be completed because no external storage
//     * device was found.  Typically, this is because the SD card is not
//     * mounted.
//     */
//    public static final int STATUS_DEVICE_NOT_FOUND_ERROR = 499;


    /** 下载完成不保存记录 */
    public static final int COMPLETE_NOT_SAVE = 1;


    /** 下载方式：删除旧文件，重新下载 */
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
    /** 下载数据的MIME类型 TEXT */
    public String mime_type;

    /** 分片数量 */
    public int separate_num;


    /** 请求头 json */
    public String header;

    /** 断点续传 range 起始位置 */
    public long mRangeStartByte = 0;
    /** 断点续传 range 结束位置 */
    public long mRangeEndByte = -1;

    @NonNull
    public static List<DownloadInfo> readDownloadInfos(Context context, @Nullable Cursor cursor) {
        ArrayList<DownloadInfo> infoList = new ArrayList<>();
        if (cursor == null) {
            return infoList;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            DownloadInfo info = new DownloadInfo();
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
            if (cursor.getColumnIndex(Constants.COLUMN_TOTAL_BYTES) != -1)
                info.total_bytes = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.COLUMN_TOTAL_BYTES));
            if (cursor.getColumnIndex(Constants.COLUMN_CURRENT_BYTES) != -1)
                info.current_bytes = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.COLUMN_CURRENT_BYTES));

            if (cursor.getColumnIndex(Constants.COLUMN_SEPARATE_NUM) != -1)
                info.separate_num = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.COLUMN_SEPARATE_NUM));
            infoList.add(info);
        }
        return infoList;
    }


    public static ContentValues info2ContentValues(DownloadInfo info) {
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


        return values;
    }
}
