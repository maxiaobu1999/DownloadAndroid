package com.malong.moses;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.malong.moses.partial.PartialInfo;
import com.malong.moses.partial.PartialProviderHelper;
import com.malong.moses.utils.FileUtils;
import com.malong.moses.utils.Utils;

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
    public Uri doDownload(Context context, DownloadTask info) {
        if (DEBUG) Log.d(TAG, "doDownload（）调用");

        Uri downloadUri;
        DownloadTask oldInfo = ProviderHelper.queryOldDownload(context, info);
        if (oldInfo == null) {
            // 3、没有旧的新增
            downloadUri = ProviderHelper.insert(context, info);
            return downloadUri;
        } else {
            // 之前下载过，
            downloadUri = Utils.generateDownloadUri(context, oldInfo.id);
            //检查是否正在下载
            if (oldInfo.status == DownloadTask.STATUS_RUNNING) {
                // 1、正在下载/下载完成，无需重复操作
                return downloadUri;
            } else if (oldInfo.status == DownloadTask.STATUS_SUCCESS) {
                // 已经下载成功过，检查文件是否存在
                if (FileUtils.checkFileExist(info.destination_path, info.fileName)) {
                    // 存在，更新状态为STATUS_SUCCESS，别的不改
                    ProviderHelper.updateStatus(context, DownloadTask.STATUS_SUCCESS, oldInfo);
                } else {
                    // 文件不存在了，需要重新下载
                    downloadUri = reDownload(context, info);
                }
            } else {
                // 2、更新状态为PENDING，别的不改
                ProviderHelper.updateStatus(context, DownloadTask.STATUS_PENDING, oldInfo);
                return downloadUri;
            }
            return downloadUri;
        }
    }

    // 停止
    public void pauseDownload(Context context, DownloadTask info) {
        if (DEBUG) {
            Log.d(TAG, "pauseDownload（）调用");
        }
        ProviderHelper.updateStatus(context, DownloadTask.STATUS_PAUSE, info);
        // 变更分片状态
        List<PartialInfo> partialInfoList = PartialProviderHelper
                .queryPartialInfoList(context, info.id);
        for (PartialInfo partialInfo : partialInfoList) {
            PartialProviderHelper.updatePartialStutas(
                    context, PartialInfo.STATUS_STOP, partialInfo);
        }
    }

    // 继续下载
    public void resumeDownload(Context context, DownloadTask info) {
        if (DEBUG) {
            Log.d(TAG, "resumeDownload（）调用");
        }
        ProviderHelper.updateStatus(context, DownloadTask.STATUS_PENDING, info);
    }


    // 取消
    public int cancelDownload(Context context, DownloadTask info) {
        int deleteNum = ProviderHelper.delete(context, info);
        // 删除分片
        PartialProviderHelper.delete(context, info);
        return deleteNum;
    }


    // 删除 删除记录，删除文件
    public int deleteDownload(Context context, DownloadTask info) {
        int cancel = cancelDownload(context, info);// 删除表数据
        FileUtils.deleteFile(info.destination_path + info.fileName);// 删除文件
        return cancel;
    }


    // 重新下载
    public Uri reDownload(Context context, DownloadTask info) {
        int cancel = cancelDownload(context, info);// 删除表数据
        FileUtils.deleteFile(info.destination_path + info.fileName);// 删除文件
        return doDownload(context, info);
    }

}
