package com.malong.moses.partial;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.malong.moses.Constants;

import java.util.ArrayList;
import java.util.List;

/** 分片实体类 */
public class PartialInfo {


    /** 过去没有开始下载 */
    public static final int STATUS_PENDING = 100;
    /** 正在下载 */
    public static final int STATUS_RUNNING = 110;
    /** 停止 */
    public static final int STATUS_STOP = 120;
    /** 下载完成 */
    public static final int STATUS_SUCCESS = 200;
    /** 下载失败 */
    public static final int STATUS_FAIL = 300;
    /** 下载被删除 */
    public static final int STATUS_CANCEL = 999;



    public int id;
    public int download_id;
    public int status;
    /** 分片索引 INTEGER */
    public int num;
    /** 当前进度 BIGINT */
    public long current_bytes;
    /** 分片总大小 BIGINT */
    public long total_bytes;
    /** 分片起点 BIGINT */
    public long start_index;
    /** 分片终点点 BIGINT */
    public long end_index;
    /** 下载地址 TEXT 必须 */
    public String download_url;
    /** 保存地址 uri TEXT nullable */
    public String destination_uri;
    /** 保存地址 文件路径（不一定有，ROM P 媒体文件夹可能获取不到文件的实际路径） TEXT */
    public String destination_path;
    /** 文件的名称，没有起一个 TEXT 必须 */
    public String fileName;


    @NonNull
    public static List<PartialInfo> readPartialInfos(Context context, @Nullable Cursor cursor) {
        ArrayList<PartialInfo> infoList = new ArrayList<>();
        if (cursor == null) {
            return infoList;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            PartialInfo info = new PartialInfo();
            if (cursor.getColumnIndex(Constants._ID) != -1)
                info.id = cursor.getInt(cursor.getColumnIndexOrThrow(Constants._ID));
            if (cursor.getColumnIndex(Constants.PARTIAL_DOWNLOAD_ID) != -1)
                info.download_id = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.PARTIAL_DOWNLOAD_ID));
            if (cursor.getColumnIndex(Constants.PARTIAL_STATUS) != -1)
                info.status = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.PARTIAL_STATUS));
            if (cursor.getColumnIndex(Constants.PARTIAL_NUM) != -1)
                info.num = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.PARTIAL_NUM));
            if (cursor.getColumnIndex(Constants.PARTIAL_TOTAL_BYTES) != -1)
                info.total_bytes = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.PARTIAL_TOTAL_BYTES));
            if (cursor.getColumnIndex(Constants.PARTIAL_CURRENT_BYTES) != -1)
                info.current_bytes = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.PARTIAL_CURRENT_BYTES));
            if (cursor.getColumnIndex(Constants.PARTIAL_START_INDEX) != -1)
                info.start_index = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.PARTIAL_START_INDEX));

            if (cursor.getColumnIndex(Constants.PARTIAL_END_INDEX) != -1)
                info.end_index = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.PARTIAL_END_INDEX));


            if (cursor.getColumnIndex(Constants.PARTIAL_DOWNLOAD_URL) != -1)
                info.download_url = cursor.getString(cursor.getColumnIndexOrThrow(Constants.PARTIAL_DOWNLOAD_URL));
            if (cursor.getColumnIndex(Constants.PARTIAL_DESTINATION_URI) != -1)
                info.destination_uri = cursor.getString(cursor.getColumnIndexOrThrow(Constants.PARTIAL_DESTINATION_URI));
            if (cursor.getColumnIndex(Constants.PARTIAL_DESTINATION_PATH) != -1)
                info.destination_path = cursor.getString(cursor.getColumnIndexOrThrow(Constants.PARTIAL_DESTINATION_PATH));
            if (cursor.getColumnIndex(Constants.PARTIAL_FILE_NAME) != -1)
                info.fileName = cursor.getString(cursor.getColumnIndexOrThrow(Constants.PARTIAL_FILE_NAME));

            infoList.add(info);
        }
        return infoList;
    }


    public static ContentValues info2ContentValues(PartialInfo info) {
        ContentValues values = new ContentValues();
        values.put(Constants.PARTIAL_DOWNLOAD_ID, info.download_id);
        values.put(Constants.PARTIAL_STATUS, info.status);
        values.put(Constants.PARTIAL_NUM, info.num);
        if (info.total_bytes != 0)
            values.put(Constants.PARTIAL_TOTAL_BYTES, info.total_bytes);
        if (info.current_bytes != 0)
            values.put(Constants.PARTIAL_CURRENT_BYTES, info.current_bytes);
        if (info.start_index != 0)
            values.put(Constants.PARTIAL_START_INDEX, info.start_index);
        if (info.end_index != 0)
            values.put(Constants.PARTIAL_END_INDEX, info.end_index);
        values.put(Constants.PARTIAL_DOWNLOAD_URL, info.download_url);
        if (!TextUtils.isEmpty(info.download_url))
            values.put(Constants.PARTIAL_DESTINATION_URI, info.destination_uri);
        if (!TextUtils.isEmpty(info.destination_path))
            values.put(Constants.PARTIAL_DESTINATION_PATH, info.destination_path);
        values.put(Constants.PARTIAL_FILE_NAME, info.fileName);
        return values;
    }

}
