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
        if (!TextUtils.isEmpty(info.destination_uri)) {

        }

    }


    public static void updateProcess(Context context, DownloadInfo info) {
        Uri uri = Utils.generateDownloadUri(context, info.id);
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_CURRENT_BYTES, info.current_bytes);
        int update = context.getContentResolver().update(uri,
                values,
                Constants._ID + "=?",
                new String[]{String.valueOf(info.id)}
        );
        if (update > 0) {
//            context.getContentResolver().notifyChange(uri, null);

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

    // 查询状态
    public static int queryStutas(Context context, int id) {
        Cursor cursor = context.getContentResolver().query(Utils.generateDownloadUri(context,id),
                new String[]{Constants.COLUMN_STATUS},
                Constants._ID + "=?",
                new String[]{String.valueOf(id)}, null, null
        );
        if (cursor == null || !cursor.moveToFirst()) {
            return -1;
        }
        int status = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.COLUMN_STATUS));
        Closeables.closeSafely(cursor);
        return status;
    }
    // 更新状态
    public static int updateStutas(Context context,int status, DownloadInfo info) {
        Uri uri = Utils.generateDownloadUri(context, info.id);
        info.status =status;
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_STATUS, info.status);
        return context.getContentResolver().update(uri, values, Constants._ID + "=?"
                , new String[]{String.valueOf(info.id)});
    }

    // 状态改变
    public static int onStatusChange(Context context, DownloadInfo info) {
        Uri uri = Utils.generateDownloadUri(context, info.id);
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_STATUS, info.status);
        int update = context.getContentResolver().update(uri,
                values,
                Constants._ID + "=?",
                new String[]{String.valueOf(info.id)}
        );
//        if (update > 0) {
//            context.getContentResolver().notifyChange(uri, null);
//        }
        return update;
    }


//    /** 检查合法性 */
//    public static void checkInfo(Context context, DownloadInfo downloadInfo) {
//        downloadInfo
//    }
}
