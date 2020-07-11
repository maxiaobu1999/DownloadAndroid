package com.malong.download;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.malong.download.utils.Closeables;
import com.malong.download.utils.Utils;

public class ProviderHelper {
    public static void insertInfo(Context context, DownloadInfo info) {
        if (!TextUtils.isEmpty(info.description_uri)) {

        }

    }


    public static void updateProcess(Context context, DownloadInfo info) {
        Uri uri = Utils.getDownloadUri(context, info.id);
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_CURRENT_BYTES, info.current_bytes);
        int update = context.getContentResolver().update(uri,
                values,
                Constants._ID + "=?",
                new String[]{String.valueOf(info.id)}
        );
        if (update > 0) {
            context.getContentResolver().notifyChange(uri, null);

        }

    }

    // 查询下载进度
    public static long queryProcess(Context context, Uri uri) {
        int downloadId = Utils.getDownloadId(context, uri);
        Cursor cursor = context.getContentResolver().query(uri,
                new String[]{Constants.COLUMN_CURRENT_BYTES},
                Constants._ID + "=?",
                new String[]{String.valueOf(downloadId)}, null, null
        );
        if (cursor == null || !cursor.moveToFirst()) {
            return -1;
        }
        long l = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.COLUMN_CURRENT_BYTES));
        Closeables.closeSafely(cursor);

        return l;
    }

    // 下载完成
    public static long onSuccess(Context context, DownloadInfo info) {
        Uri uri = Utils.getDownloadUri(context, info.id);
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_STATUS, info.status);
        int update = context.getContentResolver().update(uri,
                values,
                Constants._ID + "=?",
                new String[]{String.valueOf(info.id)}
        );
        if (update > 0) {
            context.getContentResolver().notifyChange(uri, null);
        }
        return update;
    }


//    /** 检查合法性 */
//    public static void checkInfo(Context context, DownloadInfo downloadInfo) {
//        downloadInfo
//    }
}
