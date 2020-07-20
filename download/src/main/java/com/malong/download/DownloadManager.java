package com.malong.download;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.malong.download.utils.Closeables;
import com.malong.download.utils.FileUtils;
import com.malong.download.utils.Utils;

import java.util.List;

public class DownloadManager {
    public static final String TAG = "【DownloadManager】";
    @SuppressWarnings("PointlessBooleanExpression")
    private static boolean DEBUG = Constants.DEBUG & true;
    private volatile static DownloadManager sInstance;

    private DownloadManager() {
    }

    public static DownloadManager getInstance() {
        if (sInstance == null) {
            synchronized (DownloadManager.class) {
                if (sInstance == null) {
                    sInstance = new DownloadManager();
                }
            }
        }
        return sInstance;
    }

    // 下载
    @Nullable
    public Uri download(Context context, DownloadInfo info) {
        if (DEBUG) {
            Log.d(TAG, "download（）调用");
        }
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = info.info2ContentValues();
        Uri downloadUri;
        // 检查之前下载过
        Cursor cursor = null;
        if (!TextUtils.isEmpty(info.destination_uri) && !TextUtils.isEmpty(info.fileName)) {
            cursor = resolver.query(Utils.getDownloadBaseUri(context),
                    new String[]{"*"},
                    Constants.COLUMN_DOWNLOAD_URL + "=? AND "
                            + Constants.COLUMN_DESTINATION_URI + "=? AND "
                            + Constants.COLUMN_FILE_NAME + "=?",
                    new String[]{info.download_url, info.destination_uri, info.fileName}, null, null);
        }
        if (!TextUtils.isEmpty(info.destination_path) && !TextUtils.isEmpty(info.fileName)) {
            cursor = resolver.query(Utils.getDownloadBaseUri(context),
                    new String[]{"*"},
                    Constants.COLUMN_DOWNLOAD_URL + "=? AND "
                            + Constants.COLUMN_DESTINATION_PATH + "=? AND "
                            + Constants.COLUMN_FILE_NAME + "=?",
                    new String[]{info.download_url, info.destination_path, info.fileName}, null, null);
        }
        List<DownloadInfo> infoList = DownloadInfo.readDownloadInfos(context, cursor);
        Closeables.closeSafely(cursor);

        if (infoList.size() > 0) {
            // 之前下载过，
            for (DownloadInfo item : infoList) {
                downloadUri = Utils.generateDownloadUri(context, item.id);
                if (downloadUri != null) {
                    //检查是否正在下载
                    if (item.status == DownloadInfo.STATUS_RUNNING
                    || item.status==DownloadInfo.STATUS_SUCCESS) {
                        // 1、正在下载，无需重复操作
                        return Utils.generateDownloadUri(context, item.id);
                    } else {
                        // 2、更新状态为PENDING，别的不改
                        info.id = item.id;
                        ProviderHelper.updateStutas(context, DownloadInfo.STATUS_PENDING, info);
                        return Utils.generateDownloadUri(context, item.id);
                    }
                }
            }
        } else {
            // 3、没有旧的新增
            downloadUri = resolver.insert(Utils.getDownloadBaseUri(context), values);
            Closeables.closeSafely(cursor);
            return downloadUri;
        }
        return null;
    }

    // 停止
    public void stop(Context context, DownloadInfo info) {
        if (DEBUG) {
            Log.d(TAG, "stop（）调用");
        }
        ProviderHelper.updateStutas(context, DownloadInfo.STATUS_STOP, info);
    }

    // 继续下载
    public void resume(Context context, DownloadInfo info) {
        if (DEBUG) {
            Log.d(TAG, "resume（）调用");
        }
        ProviderHelper.updateStutas(context, DownloadInfo.STATUS_PENDING, info);
    }


    // 取消
    public int cancel(Context context, DownloadInfo info) {
        ContentResolver resolver = context.getContentResolver();
        int deleteNum = resolver.delete(Utils.getDownloadBaseUri(context),
                Constants._ID + "=?",
                new String[]{String.valueOf(info.id)});
        // 删除分片
        resolver.delete(Utils.getPartialBaseUri(context),
                Constants.PARTIAL_DOWNLOAD_ID + "=?",
                new String[]{String.valueOf(info.id)});
        return deleteNum;
    }


    // 删除 删除记录，删除文件
    public int delete(Context context, DownloadInfo info) {
        int cancel = cancel(context, info);// 删除表数据
        FileUtils.deleteFile(info.destination_path + info.fileName);// 删除文件
        return cancel;
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


}
