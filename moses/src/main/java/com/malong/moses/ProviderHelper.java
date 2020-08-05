package com.malong.moses;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.malong.moses.utils.Closeables;
import com.malong.moses.utils.Utils;

import java.util.List;

public class ProviderHelper {
    public static final String TAG = "【ProviderHelper】";
    @SuppressWarnings("PointlessBooleanExpression")
    private static boolean DEBUG = Constants.DEBUG & true;

    public static void insertInfo(Context context, Request info) {
        if (!TextUtils.isEmpty(info.destination_uri)) {

        }

    }


    public static void updateProcess(Context context, Request info) {
        Uri uri = Utils.generateDownloadUri(context,info.id);
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_CURRENT_BYTES, info.current_bytes);
        int update = context.getContentResolver().update(uri,
                values,
                Constants._ID + "=?",
                new String[]{String.valueOf(info.id)}
        );
        Uri destUri = Utils.getDownloadBaseUri(context).buildUpon().appendPath(String.valueOf(info.id))
                .appendQueryParameter(Constants.KEY_PROCESS, String.valueOf(info.current_bytes))
                .appendQueryParameter(Constants.KEY_LENGTH, String.valueOf(info.total_bytes))
                .fragment(Constants.KEY_PROCESS_CHANGE).build();
        context.getContentResolver().notifyChange(destUri, null);
    }

    // 查询下载条目
    @Nullable
    public static Request queryDownloadInfo(Context context, int downloadId) {
        Request info = null;
        Cursor cursor = context.getContentResolver().query(Utils.getDownloadBaseUri(context),
                new String[]{"*"},
                Constants._ID + "=?",
                new String[]{String.valueOf(downloadId)}, null, null
        );

        List<Request> infoList = Request.readDownloadInfos(context, cursor);
        Closeables.closeSafely(cursor);
        if (infoList.size() > 0) {
            info = infoList.get(0);
        }
        return info;
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
    public static int queryStatus(Context context, int id) {
        Cursor cursor = context.getContentResolver().query(Utils.generateDownloadUri(context, id),
                new String[]{Constants.COLUMN_STATUS},
                Constants._ID + "=?",
                new String[]{String.valueOf(id)}, null, null
        );
        if (cursor == null || !cursor.moveToFirst()) {
            return Request.STATUS_NONE;
        }
        int status = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.COLUMN_STATUS));
        Closeables.closeSafely(cursor);
        return status;
    }

    // 查询状态
    public static int queryStatus(Context context, Request info) {
        int id = info.id;
        if (id <= 0) {
            // 没有设置id，表示需要根据内容查找对应的ID
            Request downloadTask = queryOldDownload(context, info);
            if (downloadTask != null) {
                id= downloadTask.id;
            }
        }
        // 用id去查内容
        Cursor cursor = context.getContentResolver().query(Utils.generateDownloadUri(context,id),
                new String[]{Constants.COLUMN_STATUS},
                Constants._ID + "=?",
                new String[]{String.valueOf(id)}, null, null
        );
        if (cursor == null || !cursor.moveToFirst()) {
            return Request.STATUS_NONE;// 还没有 return none 表示没有记录
        }
        int status = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.COLUMN_STATUS));
        Closeables.closeSafely(cursor);
        return status;
    }

    // 更新状态
    public static int updateStatus(Context context, int status, Request info) {
        if (DEBUG) Log.d(TAG, "更新状态ID:" + info.id + ";状态" + info.status + "变为" + status);
        Uri uri = Utils.generateDownloadUri(context, info.id);
        info.status = status;
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_STATUS, info.status);
        return context.getContentResolver().update(uri, values, Constants._ID + "=?"
                , new String[]{String.valueOf(info.id)});
    }

    // 更新状态
    public static int updateStatus(Context context, int oldStatus, int newStatus) {
        if (DEBUG) Log.d(TAG, "更新状态 updateStatus():" + "状态" + oldStatus + "变为" + newStatus);
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_STATUS, oldStatus);
        return context.getContentResolver()
                .update(Utils.getDownloadBaseUri(context),
                        values, Constants.COLUMN_STATUS + "=?"
                        , new String[]{String.valueOf(newStatus)});
    }

    // 更新，不含status & 下载进度
    public static int updateDownloadInfoPortion(Context context, Request info) {
        Uri uri = Utils.generateDownloadUri(context, info.id);
        ContentValues values = info.info2ContentValues();
        values.remove(Constants.COLUMN_STATUS);
        values.remove(Constants.COLUMN_CURRENT_BYTES);
        return context.getContentResolver().update(uri, values, Constants._ID + "=?"
                , new String[]{String.valueOf(info.id)});
    }

    // 更新etag
    public static int updateEtag(Context context, String etag, Request info) {
        Uri uri = Utils.generateDownloadUri(context, info.id);
        info.etag = etag;
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_ETAG, info.etag);
        return context.getContentResolver().update(uri, values, Constants._ID + "=?"
                , new String[]{String.valueOf(info.id)});
    }

    public static Request queryDownloadInfo(Context context, Uri uri) {
        int downloadId = Utils.getDownloadId(context, uri);
        Cursor cursor = context.getContentResolver().query(uri,
                new String[]{"*"},
                Constants._ID + "=?",
                new String[]{String.valueOf(downloadId)}, null, null
        );
        List<Request> downloadInfos = Request.readDownloadInfos(context, cursor);
        Closeables.closeSafely(cursor);
        if (downloadInfos.size() > 0) {
            return downloadInfos.get(0);
        }
        return null;
    }

    // 状态改变
    public static int onStatusChange(Context context, Request info) {
        Uri uri = Utils.generateDownloadUri(context, info.id);
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_STATUS, info.status);
        return context.getContentResolver().update(uri,
                values,
                Constants._ID + "=?",
                new String[]{String.valueOf(info.id)}
        );
    }


//    /** 检查合法性 */
//    public static void checkInfo(Context context, DownloadInfo downloadInfo) {
//        downloadInfo
//    }

    // url 查 info
    public static List<Request> queryByUrl(Context context, String url) {
        Cursor query = context.getContentResolver()
                .query(Utils.getDownloadBaseUri(context),
                        new String[]{"*"},
                        Constants.COLUMN_DOWNLOAD_URL + "=?",
                        new String[]{url}, null, null);
        List<Request> list = Request.readDownloadInfos(context, query);
        Closeables.closeSafely(query);
        return list;
    }


    /**
     * 查找之前是否下载过的信息
     *
     * @param context 上下文
     * @param info    下载的信息
     * @return 对应的条目信息
     */
    @Nullable
    public static Request queryOldDownload(Context context, Request info) {
        ContentResolver resolver = context.getContentResolver();
        // 检查之前下载过
        Cursor cursor = null;
        if (!TextUtils.isEmpty(info.destination_uri) && !TextUtils.isEmpty(info.fileName)) {
            cursor = resolver.query(Utils.getDownloadBaseUri(context),
                    new String[]{"*"},
                    Constants.COLUMN_DOWNLOAD_URL + "=? AND "
                            + Constants.COLUMN_DESTINATION_URI + "=? AND "
                            + Constants.COLUMN_FILE_NAME + "=?",
                    new String[]{info.download_url, info.destination_uri, info.fileName},
                    null, null);
        }
        if (!TextUtils.isEmpty(info.destination_path) && !TextUtils.isEmpty(info.fileName)) {
            cursor = resolver.query(Utils.getDownloadBaseUri(context),
                    new String[]{"*"},
                    Constants.COLUMN_DOWNLOAD_URL + "=? AND "
                            + Constants.COLUMN_DESTINATION_PATH + "=? AND "
                            + Constants.COLUMN_FILE_NAME + "=?",
                    new String[]{info.download_url, info.destination_path, info.fileName},
                    null, null);
        }
        List<Request> infoList = Request.readDownloadInfos(context, cursor);
        Closeables.closeSafely(cursor);

        if (infoList.size() == 1) {
            // 之前下载过，
            return infoList.get(0);
        } else if (infoList.size() > 1) {
            if (DEBUG) Log.e(TAG, "checkHasDownload():出现infoList.size() > 1情况");
        }
        // 没有下载过
        return null;
    }

    /**
     * 插入新条目，主键无意义都会自增
     */
    @Nullable
    public static Uri insert(Context context, Request info) {
        ContentValues values = info.info2ContentValues();
        Uri downloadUri = context.getContentResolver()
                .insert(Utils.getDownloadBaseUri(context), values);
        if (downloadUri == null) {
            if (DEBUG) Log.e(TAG, "insert()：插入失败");
            return null;
        }
        Utils.notifyChange(context, downloadUri, null);
        // 通知service
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.KEY_ID, Utils.getDownloadId(context, downloadUri));
        bundle.putInt(Constants.KEY_STATUS, Request.STATUS_PENDING);// 新增一定是PENDING
        bundle.putString(Constants.KEY_URI, downloadUri.toString());
        if (DEBUG) Log.d(TAG, "insert(）新增下载:" + downloadUri.toString());
        Utils.startDownloadService(context, bundle);
        return downloadUri;
    }

    /**
     * 删除新条目
     */
    public static int delete(Context context, Request info) {
        ContentResolver resolver = context.getContentResolver();
        int deleteNum = resolver.delete(Utils.getDownloadBaseUri(context),
                Constants._ID + "=?",
                new String[]{String.valueOf(info.id)});
        return deleteNum;
    }


}
