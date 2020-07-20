package com.malong.download.partial;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.malong.download.Constants;
import com.malong.download.DownloadInfo;
import com.malong.download.utils.Closeables;
import com.malong.download.utils.Utils;

import java.util.List;

public class PartialProviderHelper {
    public static final String TAG = "【PartialProviderHelper】";
    @SuppressWarnings("PointlessBooleanExpression")
    private static boolean DEBUG = Constants.DEBUG & true;


    // 更新，不含status & 下载进度
    public static int updateDownloadInfoPortion(Context context, DownloadInfo info) {
        Uri uri = Utils.generateDownloadUri(context, info.id);
        ContentValues values = info.info2ContentValues();
        values.remove(Constants.COLUMN_STATUS);
        values.remove(Constants.COLUMN_CURRENT_BYTES);
        return context.getContentResolver().update(uri, values, Constants._ID + "=?"
                , new String[]{String.valueOf(info.id)});
    }


    // 查询下载条目
    @Nullable
    public static PartialInfo queryPartialInfo(Context context, int partailId) {
        PartialInfo info = null;
        Cursor cursor = context.getContentResolver().query(Utils.getPartialBaseUri(context),
                new String[]{"*"},
                Constants._ID + "=?",
                new String[]{String.valueOf(partailId)}, null, null
        );

        List<PartialInfo> infoList = PartialInfo.readPartialInfos(context, cursor);
        Closeables.closeSafely(cursor);
        if (infoList.size() > 0) {
            info = infoList.get(0);
        }
        return info;
    }

    // 查询下载条目
    @NonNull
    public static List<PartialInfo> queryPartialInfoList(Context context, int downloadId) {
        Cursor cursor = context.getContentResolver().query(Utils.getPartialBaseUri(context),
                new String[]{"*"},
                Constants.PARTIAL_DOWNLOAD_ID + "=?",
                new String[]{String.valueOf(downloadId)}, null, null
        );

        List<PartialInfo> infoList = PartialInfo.readPartialInfos(context, cursor);
        Closeables.closeSafely(cursor);
        return infoList;
    }

    // 状态改变
    public static int onPartialStatusChange(Context context, PartialInfo info) {
        Uri uri = Utils.generatePartialBUri(context, info.id);
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

    // 更新状态
    public static int updatePartialStutas(Context context, int status, PartialInfo info) {
        if (DEBUG) Log.d(TAG, "ID:" + info.id + ";状态" + info.status + "变为" + status);
        Uri uri = Utils.generatePartialBUri(context, info.id);
        info.status = status;
        ContentValues values = new ContentValues();
        values.put(Constants.PARTIAL_STATUS, info.status);
        return context.getContentResolver().update(uri, values, Constants._ID + "=?"
                , new String[]{String.valueOf(info.id)});
    }

    public static void updatePartialProcess(Context context, PartialInfo info) {
        Uri uri = Utils.generatePartialBUri(context, info.id);
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_CURRENT_BYTES, info.current_bytes);
        int update = context.getContentResolver().update(uri,
                values,
                Constants._ID + "=?",
                new String[]{String.valueOf(info.id)}
        );
    }

}
