package com.malong.moses.callable;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.malong.moses.BlockContentObserver;
import com.malong.moses.CancelableThread;
import com.malong.moses.Constants;
import com.malong.moses.DownloadContentObserver;
import com.malong.moses.DownloadTask;
import com.malong.moses.ProviderHelper;
import com.malong.moses.connect.Connection;
import com.malong.moses.connect.HttpInfo;
import com.malong.moses.connect.ResponseInfo;
import com.malong.moses.partial.PartialInfo;
import com.malong.moses.partial.PartialProviderHelper;
import com.malong.moses.utils.Utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * 分片
 * 1\每个分片的进度
 * 2、
 */
public class PartialCallable implements Callable<DownloadTask> {
    public static final String TAG = "【PartialCallable】";
    @SuppressWarnings("PointlessBooleanExpression")
    private static boolean DEBUG = Constants.DEBUG & true;
    private DownloadTask mInfo;
    private Context mContext;
    private Handler mHandler;

    /** 保存各分片的进度 */
    private long[] mProcessList;
    private int doneNum;
    private List<ContentObserver> observers = new ArrayList<>();
    private CancelableThread mThread;


    public PartialCallable(Context context, DownloadTask info) {
        mContext = context;
        mInfo = info;
        mProcessList = new long[info.separate_num];
    }


    @Override
    public DownloadTask call() {
        if (DEBUG) Log.d(TAG, "call()执行");
        mThread = (CancelableThread) Thread.currentThread();
        Looper.prepare();
        Looper looper = Looper.myLooper();
        if (looper != null) mHandler = new Handler(looper);

        List<PartialInfo> partialInfos =
                PartialProviderHelper.queryPartialInfoList(mContext, mInfo.id);
        if (partialInfos.size() == 0) {
            // 新下载
            HttpInfo httpInfo = new HttpInfo();
            httpInfo.download_url = mInfo.download_url;
            httpInfo.destination_uri = mInfo.destination_uri;
            httpInfo.destination_path = mInfo.destination_path;
            httpInfo.fileName = mInfo.fileName;
            httpInfo.method = mInfo.method;

            // 获取响应头
            Connection connection = new Connection(mContext, httpInfo);
            ResponseInfo responseInfo = connection.getResponseInfo();
            if (responseInfo == null) {// 下载失败
                ProviderHelper.updateStatus(mContext, DownloadTask.STATUS_FAIL, mInfo);
                return mInfo;
            }
            if (TextUtils.isEmpty(responseInfo.acceptRanges)) {
                if (DEBUG) Log.e(TAG,
                        "无法下载，响应头没有 Accept-Ranges 不支持断点续传。下载地址：" + mInfo.download_url);
                ProviderHelper.updateStatus(mContext, DownloadTask.STATUS_FAIL, mInfo);
                return mInfo;
            }
            if (responseInfo.contentLength != 0) {
                mInfo.total_bytes = responseInfo.contentLength;
            } else {
                if (DEBUG) Log.e(TAG,
                        "无法下载，响应头 Content-Length 获取不到文件size。下载地址：" + mInfo.download_url);
                ProviderHelper.updateStatus(mContext, DownloadTask.STATUS_FAIL, mInfo);
                return mInfo;
            }
            if (!TextUtils.isEmpty(responseInfo.contentType)) {
                mInfo.mime_type = responseInfo.contentType;
            }
            if (!TextUtils.isEmpty(responseInfo.eTag)) {
                mInfo.etag = responseInfo.eTag;
            }
            ProviderHelper.updateDownloadInfoPortion(mContext, mInfo);// 持久化下载信息


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
                info.total_bytes = end - start;
                info.download_url = mInfo.download_url;
                info.destination_uri = mInfo.destination_uri;
                info.destination_path = mInfo.destination_path;
                info.fileName = mInfo.fileName;

                Uri insert = PartialProviderHelper.insert(mContext, info);

                final ContentObserver observer = new PartialObserver(mHandler);
                assert insert != null;
                mContext.getContentResolver().registerContentObserver(insert
                        , true, observer);
                observers.add(observer);
            }
        } else {
            // 分片的续传
            for (PartialInfo partialInfo : partialInfos) {
                if (DEBUG) Log.d(TAG, "partialInfo.status:" + partialInfo.status);
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
        }

        // notify block  prepare listener
        Uri blockPrepareUri= Utils.getDownloadBaseUri(mContext).buildUpon()
                .appendPath(String.valueOf(mInfo.id))
                .fragment(Constants.KEY_BLOCK_PREPARE).build();
        mContext.getContentResolver().notifyChange(blockPrepareUri,null);


        Looper.loop();
        return mInfo;
    }

    private void updateProcess(Uri uri) {
        String s = uri.getQueryParameter(Constants.KEY_PARTIAL_NUM);
        if (!TextUtils.isEmpty(s)) {
            @SuppressWarnings("ConstantConditions")
            long cur = Long.parseLong(uri.getQueryParameter(Constants.KEY_PROCESS));
            int partialIndex = Integer.parseInt(s);
            mProcessList[partialIndex] = cur;
            long curProcess = 0;
            for (long atomicLong : mProcessList) {
                curProcess += atomicLong;
            }
            mInfo.current_bytes = curProcess;
            if (DEBUG) Log.d(TAG, "总进度:" + mInfo.current_bytes);

            ProviderHelper.updateProcess(mContext, mInfo);
            // 通知host uri observer
            Uri downloadUri = Utils.getDownloadBaseUri(mContext).buildUpon()
                    .appendPath(String.valueOf(mInfo.id))
                    .appendQueryParameter(Constants.KEY_PROCESS, uri.getQueryParameter(Constants.KEY_PROCESS))
                    .appendQueryParameter(Constants.KEY_LENGTH, uri.getQueryParameter(Constants.KEY_LENGTH))
                    .appendQueryParameter(Constants.KEY_PARTIAL_NUM, uri.getQueryParameter(Constants.KEY_PARTIAL_NUM))
                    .fragment(Constants.KEY_BLOCK_PROCESS_CHANGE).build();
            if (DEBUG) Log.d(TAG, downloadUri.toString());
            mContext.getContentResolver().notifyChange(downloadUri, null);
        }
    }


    class PartialObserver extends BlockContentObserver {

        PartialObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onProcessChange(Uri uri, long cur, long length) {
            super.onProcessChange(uri, cur, length);
            if (DEBUG) Log.d(PartialCallable.TAG, "进度发生改变：" + uri.toString() + "当前进度=" + cur);
            updateProcess(uri);
        }

        @Override
        public void onStatusChange(Uri uri, int status) {
            if (DEBUG) Log.d(PartialCallable.TAG, "状态发生改变：当前状态" + uri.toString() + "=" + status);
            if (status == PartialInfo.STATUS_SUCCESS) {
                mContext.getContentResolver().unregisterContentObserver(this);
                doneNum++;
                if (doneNum == mInfo.separate_num) {
                    // 都完成
                    ProviderHelper.updateStatus(mContext, DownloadTask.STATUS_SUCCESS, mInfo);
                    quitLooper();
                }
            } else if (status == PartialInfo.STATUS_STOP
                    || status == PartialInfo.STATUS_CANCEL) {// 停止
                if (DEBUG) Log.d(TAG, "onStatusChange停止");
                for (ContentObserver observer : observers) {
                    mContext.getContentResolver().unregisterContentObserver(observer);

                }
//                ProviderHelper.updateStatus(mContext, DownloadInfo.STATUS_PAUSE, mInfo);
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
                    ThreadLocal<?> threadLocal = (ThreadLocal<?>) ob;
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
