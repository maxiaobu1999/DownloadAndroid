package com.malong.moses.block;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.malong.moses.Constants;
import com.malong.moses.Request;
import com.malong.moses.utils.Closeables;
import com.malong.moses.utils.Utils;

import java.util.List;

public class BlockProviderHelper {
    public static final String TAG = "【BlockProviderHelper】";
    @SuppressWarnings("PointlessBooleanExpression")
    private static boolean DEBUG = Constants.DEBUG & true;


    // 更新，不含status & 下载进度
    public static int updateDownloadInfoPortion(Context context, Request info) {
        Uri uri = Utils.generateDownloadUri(context, info.id);
        ContentValues values = info.info2ContentValues();
        values.remove(Constants.COLUMN_STATUS);
        values.remove(Constants.COLUMN_CURRENT_BYTES);
        return context.getContentResolver().update(uri, values, Constants._ID + "=?"
                , new String[]{String.valueOf(info.id)});
    }


    // 查询下载条目
    @Nullable
    public static BlockInfo queryPartialInfo(Context context, int partailId) {
        BlockInfo info = null;
        Cursor cursor = context.getContentResolver().query(Utils.getPartialBaseUri(context),
                new String[]{"*"},
                Constants._ID + "=?",
                new String[]{String.valueOf(partailId)}, null, null
        );

        List<BlockInfo> infoList = BlockInfo.readPartialInfos(context, cursor);
        Closeables.closeSafely(cursor);
        if (infoList.size() > 0) {
            info = infoList.get(0);
        }
        return info;
    }

    // 查询下载条目
    @NonNull
    public static List<BlockInfo> queryPartialInfoList(Context context, int downloadId) {
        Cursor cursor = context.getContentResolver().query(Utils.getPartialBaseUri(context),
                new String[]{"*"},
                Constants.PARTIAL_DOWNLOAD_ID + "=?",
                new String[]{String.valueOf(downloadId)}, null, null
        );

        List<BlockInfo> infoList = BlockInfo.readPartialInfos(context, cursor);
        Closeables.closeSafely(cursor);
        return infoList;
    }


    // 更新状态
    public static int updatePartialStatus(Context context, int status, BlockInfo info) {
        if (DEBUG)
            Log.d(TAG, "updatePartialStatus();ID:" + info.id +
                    ";状态" + info.status + "变为" + status);

        Uri uri = Utils.generatePartialBUri(context, info.id);
        info.status = status;
        ContentValues values = new ContentValues();
        values.put(Constants.PARTIAL_STATUS, info.status);
        int update = context.getContentResolver().update(uri, values, Constants._ID + "=?"
                , new String[]{String.valueOf(info.id)});
        if (update == 0) {
            if (DEBUG) Log.d(TAG, "updatePartialStatus();update == 0更新失败");
        }

        // 状态发生改变通知 service
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.KEY_ID, info.id);
        bundle.putInt(Constants.KEY_STATUS, status);
        bundle.putString(Constants.KEY_URI,
                Utils.generatePartialBUri(context, info.id).toString());
        Utils.startDownloadService(context, bundle);
        // 通知host
        Uri blockUri = Utils.getPartialBaseUri(context).buildUpon()
                .appendPath(String.valueOf(info.id))
//                .appendQueryParameter(Constants.KEY_ID, String.valueOf(info.id))
                .appendQueryParameter(Constants.KEY_STATUS, String.valueOf(status))
                .appendQueryParameter(Constants.KEY_PARTIAL_NUM, String.valueOf(info.num))
                .fragment(Constants.KEY_BLOCK_STATUS_CHANGE).build();
        context.getContentResolver().notifyChange(blockUri, null);
        // 通知host的监听
        Uri hostUri = Utils.getDownloadBaseUri(context).buildUpon()
                .appendPath(String.valueOf(info.download_id))
                .appendQueryParameter(Constants.KEY_STATUS, String.valueOf(status))
                .appendQueryParameter(Constants.KEY_PARTIAL_NUM, String.valueOf(info.num))
                .fragment(Constants.KEY_BLOCK_STATUS_CHANGE).build();
        context.getContentResolver().notifyChange(hostUri, null);
        Log.d(TAG, "通知host的监听"+hostUri.toString());
        return update;
    }

    /** 修改分片进度 */
    public static void updatePartialProcess(Context context, BlockInfo info) {
        Uri uri = Utils.generatePartialBUri(context, info.id);
        ContentValues values = new ContentValues();
        values.put(Constants.PARTIAL_CURRENT_BYTES, info.current_bytes);
        int update = context.getContentResolver().update(uri,
                values,
                Constants._ID + "=?",
                new String[]{String.valueOf(info.id)}
        );
        Uri destUri = Utils.getPartialBaseUri(context).buildUpon()
                .appendPath(String.valueOf(info.id))
                .appendQueryParameter(Constants.KEY_PARTIAL_NUM, String.valueOf(info.num))
                .appendQueryParameter(Constants.KEY_PROCESS, String.valueOf(info.current_bytes))
                .appendQueryParameter(Constants.KEY_LENGTH, String.valueOf(info.total_bytes))
                .fragment(Constants.KEY_BLOCK_PROCESS_CHANGE).build();
        context.getContentResolver().notifyChange(destUri, null);
    }


    /** 插入新条目，主键无意义都会自增 */
    @Nullable
    public static Uri insert(Context context, BlockInfo info) {
        ContentValues values = BlockInfo.info2ContentValues(info);
        Uri affectedUri = context.getContentResolver()
                .insert(Utils.getPartialBaseUri(context), values);
        if (affectedUri == null) {
            if (DEBUG) Log.e(TAG, "insert()：插入失败");
            return null;
        }

        Utils.notifyChange(context, affectedUri, null);
        // 通知service
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.KEY_ID, Utils.getPartialId(context, affectedUri));
        bundle.putInt(Constants.KEY_STATUS, Request.STATUS_PENDING);// 新增一定是PENDING
        bundle.putString(Constants.KEY_URI, affectedUri.toString());
        Utils.startDownloadService(context, bundle);
        return affectedUri;
    }

    /** 删除新条目 */
    public static int delete(Context context, Request info) {
        ContentResolver resolver = context.getContentResolver();
        return resolver.delete(Utils.getPartialBaseUri(context),
                Constants.PARTIAL_DOWNLOAD_ID + "=?",
                new String[]{String.valueOf(info.id)});
    }

}
