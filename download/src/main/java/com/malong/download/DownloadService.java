package com.malong.download;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.malong.download.callable.DownloadCallable;
import com.malong.download.utils.Utils;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class DownloadService extends Service {
    public static final String TAG = "【DownloadService】";
    private static boolean DEBUG = Constants.DEBUG;
    private Context mContext;
    private boolean first = true;
    /** 工作线程 */
    private HandlerThread mWorkThread;
    /** 工作线程Handler */
    private WorkHandler mWorkHandler;
    /** 下载线程池 */
    private ExecutorService mExecutor;

    public DownloadService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.d(TAG, "onCreate()执行");
        mContext = this;
        mWorkThread = new HandlerThread("PerformanceService[" + "性能检测" + "]");
        mWorkThread.setPriority(Thread.MIN_PRIORITY);
        mWorkThread.start();
        mWorkHandler = new WorkHandler(mWorkThread.getLooper());
        mExecutor = Executors.newFixedThreadPool(3);
        DownloadContentObserver mObserver = new DownloadContentObserver(mWorkHandler);
        // 第一个参数：需要监听的 uri。
        // 第二个参数：为 false 表示精确匹配，即只匹配该 Uri。为 true 表示可以同时匹配其派生的 Uri，
        getContentResolver().registerContentObserver(Utils.getDownloadBaseUri(this),
                true, mObserver);
//        mWorkHandler.dispatchMessage(Message.obtain(mWorkHandler,0));
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (DEBUG) Log.d(TAG, "onBind()执行");
        throw new UnsupportedOperationException("下载服务禁用bind形式，进行绑定");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "onStartCommand()执行");
        mWorkHandler.dispatchMessage(Message.obtain(mWorkHandler, 0));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy()执行");
        super.onDestroy();
    }

    private void update() {
        Log.d(TAG, "++++++++++");
        // 首次启动
        if (first) {
            // 处理下一半的任务

            first = false;
        }

        Cursor cursor = getContentResolver().query(Utils.getDownloadBaseUri(mContext),
                new String[]{"*"},
                Constants.COLUMN_STATUS + "<>?" + " OR " + Constants.COLUMN_STATUS + " IS NULL",
                new String[]{String.valueOf(DownloadInfo.STATUS_SUCCESS)
                },
//                new String[]{String.valueOf(DownloadInfo.STATUS_SUCCESS)},
                null, null);
        List<DownloadInfo> downloadInfos = DownloadInfo.readDownloadInfos(mContext, cursor);
        Log.d(TAG, "++++++++++" + downloadInfos.size());
//        for (int i = 0; i < downloadInfos.size(); i++) {
//            Log.d(TAG, "info.status:" + i+"===="+downloadInfos.get(i).status);
//
//        }
//        int i = 0;
        for (DownloadInfo info : downloadInfos) {
            Log.d(TAG, "info.status:" + info.status);
//            // 删除下载完成 && 不保存记录
//            if (info.status == DownloadInfo.STATUS_SUCCESS
//                    && info.complete == DownloadInfo.COMPLETE_NOT_SAVE) {
//                Utils.deleteItem(mContext, info);
//            }
            // 需要下载
            if (info.status == DownloadInfo.STATUS_PENDING) {
                Log.d(TAG, info.download_url);
                Callable<DownloadInfo> callable;
//                if (info.method == DownloadInfo.METHOD_COMMON) {
                // 删除旧文件，重新下载
                callable = new DownloadCallable(mContext, info);
//                } else if (info.method == DownloadInfo.METHOD_CONTINUE) {
//                    // 断点续传
//                    callable = new DownloadCallable(mContext, info);
//                }
                FutureTask<DownloadInfo> futureTask = new FutureTask<>(callable);

                mExecutor.execute(futureTask);
            }
        }


    }




    class WorkHandler extends Handler {
        WorkHandler(@NonNull Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == 0) {
                // 访问数据库
                update();
            }
            super.handleMessage(msg);
        }
    }


}
