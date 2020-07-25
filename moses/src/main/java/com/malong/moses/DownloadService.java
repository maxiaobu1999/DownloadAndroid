package com.malong.moses;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.malong.moses.callable.BreakpointCallable;
import com.malong.moses.callable.DownloadCallable;
import com.malong.moses.callable.PartialCallable;
import com.malong.moses.callable.SubCallable;
import com.malong.moses.partial.PartialInfo;
import com.malong.moses.partial.PartialProviderHelper;
import com.malong.moses.utils.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
// https://developer.android.com/training/testing/integration-testing/service-testing?hl=zh-cn
// 单测

public class DownloadService extends Service {
    public static final String TAG = "【DownloadService】";
    @SuppressWarnings("PointlessBooleanExpression")
    private static boolean DEBUG = Constants.DEBUG & true;


    private Context mContext;
    private boolean first = true;
    /** 工作线程 */
    private HandlerThread mWorkThread;
    /** 工作线程Handler */
    private WorkHandler mWorkHandler;
    /** 下载线程池 */
    private ExecutorService mExecutor;
    /** 任务集合 <下载id,下载任务> */
    private HashMap<Integer, FutureTask> mTaskMap = new HashMap<>();
    private HashMap<Integer, FutureTask> mPartialTaskMap = new HashMap<>();

    public DownloadService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.d(TAG, "onCreate()执行");
        mContext = this;
        first = true;

        mWorkThread = new HandlerThread("DownloadService[" + "工作线程" + "]");
        mWorkThread.setPriority(Thread.MIN_PRIORITY);
        mWorkThread.start();
        mWorkHandler = new WorkHandler(mWorkThread.getLooper());
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull Runnable r) {
                return new CancelableThread(r);
            }
        };
        mExecutor = Executors.newFixedThreadPool(10, threadFactory);
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (DEBUG) Log.d(TAG, "onBind()执行");
        throw new UnsupportedOperationException("下载服务禁用bind形式，进行绑定");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "onStartCommand()执行");
        mWorkHandler.dispatchMessage(Message.obtain(mWorkHandler, 0, intent));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy()执行");
        // 清理FutureTask
        for (Map.Entry<Integer, FutureTask> item : mTaskMap.entrySet()) {
            FutureTask task = item.getValue();
            if (!task.isDone()) {
                task.cancel(true);
            }
        }
        mExecutor.shutdownNow();
        super.onDestroy();
    }

    private void parseDownload(Bundle bundle) {
        if (DEBUG) Log.d(TAG, " parseDownload()执行");
        int status = bundle.getInt(Constants.KEY_STATUS, -1);
        int id = bundle.getInt(Constants.KEY_ID, -1);
        DownloadTask info = ProviderHelper.queryDownloadInfo(mContext, id);
        if (status == DownloadTask.STATUS_PENDING) {
            //  添加
            if (info == null) {// ID无效
                return;
            }
            Callable<DownloadTask> callable = null;
            if (info.method == DownloadTask.METHOD_COMMON) {
                // 删除旧文件，重新下载
                callable = new DownloadCallable(mContext, info);
            } else if (info.method == DownloadTask.METHOD_BREAKPOINT) {
                // 断点续传
                callable = new BreakpointCallable(mContext, info);
            } else if (info.method == DownloadTask.METHOD_PARTIAL) {
                // 分片下载
                callable = new PartialCallable(mContext, info);
            }
            if (callable != null) {
                // 状态变为正在下载
                ProviderHelper.updateStatus(mContext, DownloadTask.STATUS_RUNNING, info);
                FutureTask<DownloadTask> futureTask = new FutureTask<>(callable);
                mTaskMap.put(info.id, futureTask);
                if (DEBUG) {
                    Log.d(TAG, "启动call：uri=" +
                            Utils.generateDownloadUri(mContext, info.id).toString());
                }
                mExecutor.execute(futureTask);
            }
        } else if (status == DownloadTask.STATUS_PAUSE) {
            // 停止
            if (info == null) {// ID无效
                return;
            }

            // 停止主任务
            FutureTask task = mTaskMap.get(info.id);
            if (task != null) {
                boolean cancel = task.cancel(true);
                if (DEBUG) Log.d(TAG, "info.id:停止" + info.id + cancel);
                mTaskMap.remove(info.id);
            }
//             状态变为停止
//            ProviderHelper.updateStatus(mContext, DownloadInfo.STATUS_PAUSE, info);
        } else if (status == DownloadTask.STATUS_CANCEL) {

            // 删除任务
            if (mTaskMap.containsKey(id)) {
                FutureTask task = mTaskMap.get(id);
                if (task != null) {
                    task.cancel(true);
                    mTaskMap.remove(id);
                }
            }
        }
    }

    private void parsePartial(Bundle bundle) {
        if (DEBUG) Log.d(TAG, "parsePartial() 执行");
        int status = bundle.getInt(Constants.KEY_STATUS, -1);
        int id = bundle.getInt(Constants.KEY_ID, -1);
        PartialInfo info = PartialProviderHelper.queryPartialInfo(mContext, id);
        if (status == PartialInfo.STATUS_PENDING) {
//            if (info == null) {// ID无效
//
//                return;
//            }
            Callable<PartialInfo> callable = new SubCallable(mContext, info);
//            if (info.method == DownloadInfo.METHOD_COMMON) {
//                // 删除旧文件，重新下载
//                callable = new DownloadCallable(mContext, info);
//            } else if (info.method == DownloadInfo.METHOD_BREAKPOINT) {
//                // 断点续传
//                callable = new BreakpointCallable(mContext, info);
//            } else if (info.method == DownloadInfo.METHOD_PARTIAL) {
//                // 断点续传
//                callable = new PartialCallable(mContext, info, mExecutor);
//            }
            if (callable != null) {
                if (DEBUG) {
                    Log.d(TAG, "开始下载任务：" +
                            Utils.generatePartialBUri(mContext, info.id).toString());
                }
                // 状态变为正在下载
                PartialProviderHelper.updatePartialStutas(mContext, DownloadTask.STATUS_RUNNING, info);
                FutureTask<PartialInfo> futureTask = new FutureTask<>(callable);
                mPartialTaskMap.put(info.id, futureTask);
                mExecutor.execute(futureTask);
            }
        } else if (status == PartialInfo.STATUS_STOP) {
            // 停止分片任务,
            PartialInfo partialInfo = PartialProviderHelper.queryPartialInfo(mContext, id);
            FutureTask task = mPartialTaskMap.get(partialInfo.id);
            if (task != null) {
                boolean cancel = task.cancel(true);
                if (DEBUG) Log.d(TAG, "partialInfo.id:停止" + partialInfo.id + cancel);
            }
        } else if (status == PartialInfo.STATUS_CANCEL) {
            // 停止分片任务,
            List<PartialInfo> partialInfoList = PartialProviderHelper
                    .queryPartialInfoList(mContext, id);
            for (PartialInfo partialInfo : partialInfoList) {
                FutureTask task = mPartialTaskMap.get(partialInfo.id);
                if (task != null) {
                    boolean cancel = task.cancel(true);
                    if (DEBUG) Log.d(TAG, "PartialTask:停止" + partialInfo.id + cancel);
                }
            }
        }
    }

    // 添加 删除 暂停 继续下载
    private void update(@Nullable Intent intent) {
        if (Constants.DEBUG&&intent!=null) {
            int status = 0;
            int id = 0;
            String uri = "";
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                status = bundle.getInt(Constants.KEY_STATUS, -1);
                id = bundle.getInt(Constants.KEY_ID, -1);
                uri = bundle.getString(Constants.KEY_URI, "");

            }
            Log.d(TAG, "update调用：status=" + status + "；id=" + id + "uri=" + uri);
        }
        onFirstStart();
        if (intent == null) {
            return;
        }
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }
        Uri uri = Uri.parse(bundle.getString(Constants.KEY_URI));
        if (Utils.getDownloadAuthority(mContext).equals(uri.getAuthority())) {
            parseDownload(bundle);
        } else if (Utils.getPartialAuthority(mContext).equals(uri.getAuthority())) {
            parsePartial(bundle);
        }


    }


    // 首次启动
    private void onFirstStart() {
        if (!first) return;
        if (DEBUG) Log.d(TAG, " onFirstStart()执行");
        first = false;
        // 首次启动 处理下一半的任务,running 2 pending
        int update = ProviderHelper.updateStatus(mContext,
                DownloadTask.STATUS_PENDING, DownloadTask.STATUS_RUNNING);
        if (DEBUG) Log.d(TAG, "onFirstStart():有【" + update
                + "】个STATUS_RUNNING变为STATUS_PENDING");

    }

    class WorkHandler extends Handler {
        WorkHandler(@NonNull Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == 0) {
                if (msg.obj instanceof Intent) {
                    update((Intent) msg.obj);
                }
            }
            super.handleMessage(msg);
        }
    }

}
