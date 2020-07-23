package com.malong.moses;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.malong.moses.partial.PartialInfo;
import com.malong.moses.partial.PartialProviderHelper;
import com.malong.moses.utils.FileUtils;
import com.malong.moses.utils.Utils;

import java.util.List;

public class Download {
    public static final String TAG = "【DownloadManager】";
    @SuppressWarnings("PointlessBooleanExpression")
    private static boolean DEBUG = Constants.DEBUG & true;
    private volatile static Download sInstance;

    private Download() {
    }

    public static Download getInstance() {
        if (sInstance == null) {
            synchronized (Download.class) {
                if (sInstance == null) {
                    sInstance = new Download();
                }
            }
        }
        return sInstance;
    }

    // 下载
    @Nullable
    public static DownloadTask doDownload(Context context, DownloadTask info) {
        if (DEBUG) Log.d(TAG, "doDownload（）调用");

        Uri downloadUri;
        DownloadTask oldInfo = ProviderHelper.queryOldDownload(context, info);
        if (oldInfo == null) {
            // 3、没有旧的新增
            downloadUri = ProviderHelper.insert(context, info);
        } else {
            // 之前下载过，
            downloadUri = Utils.generateDownloadUri(context, oldInfo.id);
            //检查是否正在下载
            if (oldInfo.status == DownloadTask.STATUS_RUNNING) {
                // 1、正在下载/下载完成，无需重复操作
            } else if (oldInfo.status == DownloadTask.STATUS_SUCCESS) {
                // 已经下载成功过，检查文件是否存在
                if (FileUtils.checkFileExist(info.destination_path, info.fileName)) {
                    // 存在，更新状态为STATUS_SUCCESS，别的不改
                    ProviderHelper.updateStatus(context, DownloadTask.STATUS_SUCCESS, oldInfo);
                } else {
                    // 文件不存在了，需要重新下载
                    return reDownload(context, info);
                }
            } else {
                // 2、更新状态为PENDING，别的不改
                ProviderHelper.updateStatus(context, DownloadTask.STATUS_PENDING, oldInfo);
            }
        }
        return ProviderHelper.queryDownloadInfo(context, downloadUri);
    }

    // 停止
    public static void pauseDownload(Context context, DownloadTask info) {
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
    public static void resumeDownload(Context context, DownloadTask info) {
        if (DEBUG) {
            Log.d(TAG, "resumeDownload（）调用");
        }
        ProviderHelper.updateStatus(context, DownloadTask.STATUS_PENDING, info);
    }


    // 取消
    public static int cancelDownload(Context context, DownloadTask info) {
        int deleteNum;
        if (info.id < 0) {
            DownloadTask oldInfo = ProviderHelper.queryOldDownload(context, info);
            deleteNum = ProviderHelper.delete(context, oldInfo);
        } else {
            deleteNum = ProviderHelper.delete(context, info);
        }
        // 删除分片
        PartialProviderHelper.delete(context, info);
        return deleteNum;
    }


    // 删除 删除记录，删除文件
    public static int deleteDownload(Context context, DownloadTask info) {
        int cancel = cancelDownload(context, info);// 删除表数据
        FileUtils.deleteFile(info.destination_path + info.fileName);// 删除文件
        return cancel;
    }


    // 重新下载
    public static DownloadTask reDownload(Context context, DownloadTask info) {
        int cancel = cancelDownload(context, info);// 删除表数据
        FileUtils.deleteFile(info.destination_path + info.fileName);// 删除文件
        return doDownload(context, info);
    }

    // 获取下载项的状态
    public static int getTaskStatus(Context context, DownloadTask info) {
        return ProviderHelper.queryStutas(context, info);
    }

    /**
     * 更新下载内容，之前下载过则返回旧的，没有则返回参数的信息
     */
    @NonNull
    public static DownloadTask convertDownloadInfo(Context context, DownloadTask info) {
        DownloadTask downloadTask;
        if (info.id <= 0) {
            // 没有设置ID
            downloadTask = ProviderHelper.queryOldDownload(context, info);
        } else {
            // 用ID查信息
            downloadTask = ProviderHelper.queryDownloadInfo(context, info.id);
        }
        if (downloadTask != null) info = downloadTask;
        return info;
    }


}
