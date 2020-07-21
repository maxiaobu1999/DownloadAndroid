package com.malong.download.callable;

import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.malong.download.CancelableThread;
import com.malong.download.Constants;
import com.malong.download.DownloadContentObserver;
import com.malong.download.DownloadInfo;
import com.malong.download.Http;
import com.malong.download.HttpInfo;
import com.malong.download.ProviderHelper;
import com.malong.download.partial.PartialInfo;
import com.malong.download.partial.PartialProviderHelper;
import com.malong.download.utils.Utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * 分片
 * 1\每个分片的进度
 * 2、
 */
public class PartialCallable implements Callable<DownloadInfo> {
    public static final String TAG = "【PartialCallable】";
    private static boolean DEBUG = Constants.DEBUG & true;
    private DownloadInfo mInfo;
    private Context mContext;
    //    private ExecutorService mExecutorService;
    @SuppressWarnings("ConstantConditions")
    private Handler mHandler;

    private long[] mProcessList;
    private int doneNum;
    private List<ContentObserver> observers = new ArrayList<>();
    private CancelableThread mThread;


    public PartialCallable(Context context, DownloadInfo info, ExecutorService executorService) {
        mContext = context;
        mInfo = info;
//        mExecutorService = executorService;
        mProcessList = new long[info.separate_num];
    }


    @Override
    public DownloadInfo call() throws Exception {
        Log.d(TAG, "PartialCallable:call()执行");
        Log.d(TAG, "mInfo.separate_num:" + mInfo.separate_num);
        mThread = (CancelableThread) Thread.currentThread();
        Log.d(TAG, "Looper.myLooper():" + Looper.myLooper());
        if (Looper.myLooper() == null)
            Looper.prepare();
        Looper looper = Looper.myLooper();
        if (looper != null) mHandler = new Handler(looper);
        List<PartialInfo> partialInfos = PartialProviderHelper.queryPartialInfoList(mContext, mInfo.id);
        if (partialInfos.size() > 0) {
            // 分片的续传
            for (PartialInfo partialInfo : partialInfos) {
                Log.d(TAG, "partialInfo.status:" + partialInfo.status);
                if (partialInfo.status == PartialInfo.STATUS_SUCCESS) {
                    doneNum++;// 下载完成的不再处理，记录为完成
                } else {
                    PartialProviderHelper.updatePartialStutas(mContext,
                            PartialInfo.STATUS_PENDING, partialInfo);
                    final ContentObserver observer = new PartialObserver(mHandler);
                    mContext.getContentResolver().registerContentObserver(
                            Utils.generatePartialBUri(mContext, partialInfo.id)
                            , true, observer);
                    observers.add(observer);
                }
            }
        } else {
            // 重新下载
            HttpInfo httpInfo = new HttpInfo();
            httpInfo.download_url = mInfo.download_url;
            httpInfo.destination_uri = mInfo.destination_uri;
            httpInfo.destination_path = mInfo.destination_path;
            httpInfo.fileName = mInfo.fileName;
//        httpInfo.status = mInfo.status;
            httpInfo.method = mInfo.method;
            httpInfo.total_bytes = mInfo.total_bytes;
            httpInfo.current_bytes = 0;// 这里是为了获取长度

            // 先获取文件长度
            Http http = new Http(mContext, httpInfo);
            mInfo.total_bytes = http.fetchLength();
            mInfo.etag = http.getETag();
            ProviderHelper.updateDownloadInfoPortion(mContext, mInfo);
            long partSize = mInfo.total_bytes / mInfo.separate_num;
            for (int i = 0; i < mInfo.separate_num; i++) {
                long start = i * partSize;
                long end = start + partSize - 1;
                if (i == mInfo.separate_num - 1) {
                    end = mInfo.total_bytes;
                }

                PartialInfo info = new PartialInfo();
                info.status = PartialInfo.STATUS_PENDING;// 设置成待下载
                info.start_index = start;
                info.end_index = end;

                info.download_id = mInfo.id;
                info.num = i;
                info.current_bytes = 0;
                info.total_bytes = mInfo.total_bytes;
                info.download_url = mInfo.download_url;
                info.destination_uri = mInfo.destination_uri;
                info.destination_path = mInfo.destination_path;
                info.fileName = mInfo.fileName;

                Uri insert = PartialProviderHelper.insert(mContext, info);

                final ContentObserver observer = new PartialObserver(mHandler);
                mContext.getContentResolver().registerContentObserver(insert
                        , true, observer);
                observers.add(observer);
            }
        }


        Looper.loop();
        return mInfo;
    }

    public void updateProcess(Uri uri, long cur) {
        String s = uri.getQueryParameter(Constants.KEY_PARTIAL_NUM);
        if (!TextUtils.isEmpty(s)) {
            int partialNum = Integer.parseInt(s);
            mProcessList[partialNum] = cur;
            long curProcess = 0;
            for (long atomicLong : mProcessList) {
                curProcess += atomicLong;
            }
            mInfo.current_bytes = curProcess;
            ProviderHelper.updateProcess(mContext, mInfo);
            Log.d(TAG, "size:" + mInfo.current_bytes);
        }
    }


    class PartialObserver extends DownloadContentObserver {

        public PartialObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onProcessChange(Uri uri, long cur) {
            super.onProcessChange(uri, cur);
            Log.d(PartialCallable.TAG, "进度发生改变：" + uri.toString() + "当前进度=" + cur);
            updateProcess(uri, cur);
        }

        @Override
        public void onStatusChange(Uri uri, int status) {
            Log.d(PartialCallable.TAG, "状态发生改变：当前状态" + uri.toString() + "=" + status);
            if (status == PartialInfo.STATUS_SUCCESS) {
                mContext.getContentResolver().unregisterContentObserver(this);
                doneNum++;
                if (doneNum == mInfo.separate_num) {
                    // 都完成
                    ProviderHelper.updateStatus(mContext, DownloadInfo.STATUS_SUCCESS, mInfo);
                    quitLooper();
                }
            } else if (status == PartialInfo.STATUS_STOP
                    || status == PartialInfo.STATUS_CANCEL) {// 停止
                Log.d(TAG, "onStatusChange停止");
                for (ContentObserver observer : observers) {
                    mContext.getContentResolver().unregisterContentObserver(observer);

                }
//                ProviderHelper.updateStatus(mContext, DownloadInfo.STATUS_STOP, mInfo);
                quitLooper();
            }
        }
    }

    private void quitLooper() {
        Looper looper = Looper.myLooper();
        if (looper != null) {
            mThread.cancel = false;
            looper.quit();
            // TODO: 2020-07-20 looper不能复用，这么应该会有坑，看看有什么问题在说
            try {
                // 清除ThreadLocal里的looper，否则再次prepare会报错。tip这里不会抛异常，但是会直接结束任务
                @SuppressWarnings("JavaReflectionMemberAccess")
                Field field = looper.getClass().getDeclaredField("sThreadLocal");
                field.setAccessible(true);
                Object ob = field.get(looper);
                if (ob instanceof ThreadLocal) {
                    ThreadLocal<?> threadLocal = (ThreadLocal<?>)ob ;
                    threadLocal.set(null);
                }

//                // 防止引用消息队列，应该没用，写着玩吧。
//                @SuppressWarnings("JavaReflectionMemberAccess")
//                Field fieldThread = looper.getClass().getDeclaredField("mThread");
//                fieldThread.setAccessible(true);
//                fieldThread.set(looper,null);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
