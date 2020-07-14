package com.malong.download;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.malong.download.utils.Closeables;
import com.malong.download.utils.Utils;

import java.util.List;

public class DownloadHelper {
    private volatile static DownloadHelper sInstance;

    private DownloadHelper() {
    }

    public static DownloadHelper getInstance() {
        if (sInstance == null) {
            synchronized (DownloadHelper.class) {
                if (sInstance == null) {
                    sInstance = new DownloadHelper();
                }
            }
        }
        return sInstance;
    }

    // 下载
    public Uri download(Context context, DownloadInfo info) {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = DownloadInfo.info2ContentValues(info);
        Uri downloadUri = null;
        // 检查之前下载过
        Cursor cursor = null;
        if (!TextUtils.isEmpty(info.destination_uri) && !TextUtils.isEmpty(info.fileName)) {
            cursor = resolver.query(Utils.getDownloadBaseUri(context),
                    new String[]{"*"},
                    Constants.COLUMN_DOWNLOAD_URL + "=? AND "
                            + Constants.COLUMN_DESTINATION_URI + "=? AND "
                            + Constants.COLUMN_FILE_NAME + "=?",
                    new String[]{info.destination_uri, info.fileName}, null, null);
        }
        if (!TextUtils.isEmpty(info.destination_path) && !TextUtils.isEmpty(info.fileName)) {
            cursor = resolver.query(Utils.getDownloadBaseUri(context),
                    new String[]{"*"},
                    Constants.COLUMN_DOWNLOAD_URL + "=? AND "
                            + Constants.COLUMN_DESTINATION_PATH + "=? AND "
                            + Constants.COLUMN_FILE_NAME + "=?",
                    new String[]{info.destination_path, info.fileName}, null, null);
        }
        List<DownloadInfo> infoList = DownloadInfo.readDownloadInfos(context, cursor);
        Closeables.closeSafely(cursor);

        for (DownloadInfo item : infoList) {
            downloadUri = Utils.generateDownloadUri(context, item.id);
            // 之前下载过，
            if (downloadUri != null) {
                //检查是否正在下载
                if (item.status == DownloadInfo.STATUS_RUNNING) {
                    // 1、正在下载，无需重复操作
                    return Utils.generateDownloadUri(context, item.id);
                } else {
                    // 2、更新状态为PENDING，别的不改
                    info.status = DownloadInfo.STATUS_PENDING;
                    values = new ContentValues();
                    values.put(Constants.COLUMN_STATUS, info.status);
                    resolver.update(Utils.generateDownloadUri(context, item.id),
                            values, Constants._ID + "=?",
                            new String[]{String.valueOf(item.id)});
                    return Utils.generateDownloadUri(context, item.id);
                }
            }
        }

        // 3、没有旧的新增
        downloadUri = resolver.insert(Utils.getDownloadBaseUri(context), values);
        Closeables.closeSafely(cursor);
        return downloadUri;
    }

    // 停止
    public void stop(Context context, DownloadInfo info) {
        ProviderHelper.updateStutas(context, DownloadInfo.STATUS_STOP, info);
    }

    // 继续下载
    public void resume(Context context, DownloadInfo info) {
        ProviderHelper.updateStutas(context, DownloadInfo.STATUS_PENDING, info);
    }




    public int delete(Context context, DownloadInfo info) {
        ContentResolver resolver = context.getContentResolver();
        int deleteNum = resolver.delete(Utils.getDownloadBaseUri(context),
                Constants._ID + "=?",
                new String[]{String.valueOf(info.id)});
        return deleteNum;
    }


    // 查询下载状态
    public static int queryStatus(Context context, Uri uri) {
        int downloadId = Utils.getDownloadId(context, uri);
        Cursor cursor = context.getContentResolver().query(uri,
                new String[]{Constants.COLUMN_STATUS},
                Constants._ID + "=?",
                new String[]{String.valueOf(downloadId)}, null, null
        );
        if (cursor == null || !cursor.moveToFirst()) {
            return -1;
        }
        int status = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.COLUMN_STATUS));
        Closeables.closeSafely(cursor);

        return status;
    }

    //
    public static DownloadInfo queryDownloadInfo(Context context, Uri uri) {
        int downloadId = Utils.getDownloadId(context, uri);
        Cursor cursor = context.getContentResolver().query(uri,
                new String[]{"*"},
                Constants._ID + "=?",
                new String[]{String.valueOf(downloadId)}, null, null
        );
        List<DownloadInfo> downloadInfos = DownloadInfo.readDownloadInfos(context, cursor);
        Closeables.closeSafely(cursor);
        if (downloadInfos.size() > 0) {
            return downloadInfos.get(0);
        }
        return null;
    }

}
