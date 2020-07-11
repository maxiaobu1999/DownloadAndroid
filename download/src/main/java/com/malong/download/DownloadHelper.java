package com.malong.download;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.malong.download.utils.Closeables;
import com.malong.download.utils.Utils;

public class DownloadHelper {

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

}
