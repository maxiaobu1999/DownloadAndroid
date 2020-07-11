package com.malong.download;

import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

public class DownloadInfo {
    /** 过去没有开始下载 */
    public static final int STATUS_PENDING = 190;
    /** 正在下载 */
    public static final int STATUS_RUNNING = 192;
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
    public static final int METHOD_CONTINUE = 1;

    /** id主键 */
    public int id;
    /** 下载链接 */
    public String download_url;
    /** 保存地址uri */
    public String description_uri;
    /** 保存地址路径 */
    public String description_filepath;
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

    /** 断点续传 range 起始位置 */
    public long mRangeStartByte = 0;
    /** 断点续传 range 结束位置 */
    public long mRangeEndByte = -1;

    public static List<DownloadInfo> readDownloadInfos(Context context, Cursor cursor) {
        ArrayList<DownloadInfo> downloadInfos = new ArrayList<>();
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            DownloadInfo info = new DownloadInfo();
            info.id = cursor.getInt(cursor.getColumnIndexOrThrow(Constants._ID));
            info.status = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.COLUMN_STATUS));
            info.download_url = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_DOWNLOAD_URL));
            info.description_uri = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_DESTINATION_URI));
            info.description_filepath = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_DESTINATION_PATH));
            info.fileName = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_FILE_NAME));
            info.method = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.COLUMN_METHOD));
            info.total_bytes = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.COLUMN_TOTAL_BYTES));
            info.current_bytes = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.COLUMN_CURRENT_BYTES));
            downloadInfos.add(info);
        }
        return downloadInfos;
    }
}
