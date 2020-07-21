package com.malong.download;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.malong.download.partial.PartialInfo;
import com.malong.download.partial.PartialProviderHelper;
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
        if (DEBUG) Log.d(TAG, "download（）调用");

        Uri downloadUri;
        DownloadInfo oldInfo = ProviderHelper.queryOldDownload(context, info);
        if (oldInfo == null) {
            // 3、没有旧的新增
            downloadUri = ProviderHelper.insert(context, info);
            return downloadUri;
        } else {
            // 之前下载过，
            downloadUri = Utils.generateDownloadUri(context, oldInfo.id);
            //检查是否正在下载
            if (oldInfo.status == DownloadInfo.STATUS_RUNNING
                    || oldInfo.status == DownloadInfo.STATUS_SUCCESS) {
                // 1、正在下载/下载完成，无需重复操作
                return downloadUri;
            } else {
                // 2、更新状态为PENDING，别的不改
                ProviderHelper.updateStatus(context, DownloadInfo.STATUS_PENDING, oldInfo);
                return downloadUri;
            }
        }

    }

    // 停止
    public void stop(Context context, DownloadInfo info) {
        if (DEBUG) {
            Log.d(TAG, "stop（）调用");
        }
        ProviderHelper.updateStatus(context, DownloadInfo.STATUS_STOP, info);
        // 变更分片状态
        List<PartialInfo> partialInfoList = PartialProviderHelper
                .queryPartialInfoList(context, info.id);
        for (PartialInfo partialInfo : partialInfoList) {
            PartialProviderHelper.updatePartialStutas(
                    context, PartialInfo.STATUS_STOP, partialInfo);
        }
    }

    // 继续下载
    public void resume(Context context, DownloadInfo info) {
        if (DEBUG) {
            Log.d(TAG, "resume（）调用");
        }
        ProviderHelper.updateStatus(context, DownloadInfo.STATUS_PENDING, info);
    }


    // 取消
    public int cancel(Context context, DownloadInfo info) {
        int deleteNum = ProviderHelper.delete(context, info);
        // 删除分片
        PartialProviderHelper.delete(context, info);
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
