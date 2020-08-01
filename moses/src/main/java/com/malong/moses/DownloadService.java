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

import com.malong.moses.block.BlockInfo;
import com.malong.moses.block.BlockProviderHelper;
import com.malong.moses.callable.DownBlockCallable;
import com.malong.moses.callable.DownBreakpointCallable;
import com.malong.moses.callable.DownCallable;
import com.malong.moses.callable.DownSubCallable;
import com.malong.moses.utils.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
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
//    /** 下载线程池 */
//    private ThreadPoolExecutor mExecutor;
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
//        mExecutor = new ThreadPoolExecutor(4, 20,
//                3000, TimeUnit.MILLISECONDS,
//                new LinkedBlockingDeque<>(), threadFactory);
//        mExecutor.allowCoreThreadTimeOut(true);// 核心线程池可回收
        mWorkHandler.sendMessageDelayed(Message.obtain(mWorkHandler, 1), 5000);

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
//        mExecutor.shutdownNow();
        super.onDestroy();
    }

    private void parseDownload(Bundle bundle) {
        if (DEBUG) Log.d(TAG, " parseDownload()执行");
        int status = bundle.getInt(Constants.KEY_STATUS, -1);
        int id = bundle.getInt(Constants.KEY_ID, -1);
        Request info = ProviderHelper.queryDownloadInfo(mContext, id);
        if (status == Request.STATUS_PENDING) {
            //  添加
            if (info == null) {// ID无效
                return;
            }
            Callable<Request> callable = null;
            if (info.method == Request.METHOD_COMMON) {
                // 删除旧文件，重新下载
                callable = new DownCallable(mContext, info);
            } else if (info.method == Request.METHOD_BREAKPOINT) {
                // 断点续传
                callable = new DownBreakpointCallable(mContext, info);
            } else if (info.method == Request.METHOD_PARTIAL) {
                // 分片下载
                callable = new DownBlockCallable(mContext, info);
            }
            if (callable != null) {
                // 状态变为正在下载
                ProviderHelper.updateStatus(mContext, Request.STATUS_RUNNING, info);
                FutureTask<Request> futureTask = new FutureTask<>(callable);
                mTaskMap.put(info.id, futureTask);
                if (DEBUG) {
                    Log.d(TAG, "启动call：uri=" +
                            Utils.generateDownloadUri(mContext, info.id).toString());
                }
                if (MosesConfig.serial) {
                    MosesExecutors.equeue(futureTask);// 串行下载
                } else {
                    MosesExecutors.execute(futureTask);
                }
            }
        } else if (status == Request.STATUS_PAUSE) {
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
        } else if (status == Request.STATUS_CANCEL) {

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
        BlockInfo info = BlockProviderHelper.queryPartialInfo(mContext, id);
        if (status == BlockInfo.STATUS_PENDING) {
//            if (info == null) {// ID无效
//
//                return;
//            }
            Callable<BlockInfo> callable = new DownSubCallable(mContext, info);
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
                BlockProviderHelper.updatePartialStutas(mContext, Request.STATUS_RUNNING, info);
                FutureTask<BlockInfo> futureTask = new FutureTask<>(callable);
                mPartialTaskMap.put(info.id, futureTask);
                MosesExecutors.execute(futureTask);
            }
        } else if (status == BlockInfo.STATUS_STOP) {
            // 停止分片任务,
            BlockInfo partialInfo = BlockProviderHelper.queryPartialInfo(mContext, id);
            FutureTask task = mPartialTaskMap.get(partialInfo.id);
            if (task != null) {
                boolean cancel = task.cancel(true);
                if (DEBUG) Log.d(TAG, "partialInfo.id:停止" + partialInfo.id + cancel);
            }
        } else if (status == BlockInfo.STATUS_CANCEL) {
            // 停止分片任务,
            List<BlockInfo> partialInfoList = BlockProviderHelper
                    .queryPartialInfoList(mContext, id);
            for (BlockInfo partialInfo : partialInfoList) {
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
        if (Constants.DEBUG && intent != null) {
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
                Request.STATUS_PENDING, Request.STATUS_RUNNING);
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
            } else if (msg.what == 1) {
                if (MosesExecutors.isIdle()) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "没有下载任务,退出服务");
                    stopSelf();
                } else {
                    mWorkHandler.sendMessageDelayed(Message.obtain(mWorkHandler, 1), 5000);
                }
            }
        }
    }

}
