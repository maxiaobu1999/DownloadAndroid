package com.malong.moses.listener;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.malong.moses.Constants;
import com.malong.moses.Request;
import com.malong.moses.utils.Utils;

public class BlockListener implements Listener {
    public static final String TAG = "【BaseListener】";
    @SuppressWarnings("PointlessBooleanExpression")
    private static boolean DEBUG = Constants.DEBUG & false;
    Context mContext;
    private ContentObserver mObserver;
//    private List<ContentObserver> mBlockObserverList = new ArrayList<>(5);

    /** 回调在主线 */
    public void register(Context context, int taskId) {
        unregister();// 解决重复注册监听
        mContext = context;
        mObserver = new InnerObserver();
        Uri uri = Utils.generateDownloadUri(context, taskId);
        mContext.getContentResolver().registerContentObserver(uri, false, mObserver);
    }

    public void unregister() {
        if (mContext != null && mObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mObserver);
            mObserver = null;
//            for (ContentObserver observer : mBlockObserverList) {
//                mContext.getContentResolver().unregisterContentObserver(observer);
//            }
//            mBlockObserverList.clear();
            mContext = null;
        }
    }

    class InnerObserver extends ContentObserver {
        /** 上一次下载状态 **/
        private int mLastState = -1;

        InnerObserver() {
            super(new Handler(Looper.getMainLooper()));
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (DEBUG) Log.d(TAG, "onChange():" + uri.toString());
            if (Constants.KEY_STATUS_CHANGE.equals(uri.getFragment())) {
                // 状态改变
                try {
                    @SuppressWarnings("ConstantConditions")
                    int status = Integer.parseInt(uri.getQueryParameter(Constants.KEY_STATUS));
                    onStatusChange(status);
                    if (status != mLastState) {
                        if (DEBUG) {
                            Log.d(TAG, "状态发生改变：当前状态=" + status);
                        }
                        mLastState = status;
                        if (status == Request.STATUS_RUNNING) {
                            onStart();
                        } else if (status == Request.STATUS_PAUSE) {
                            onPause();
                        } else if (status == Request.STATUS_SUCCESS) {
                            onSuccess();
                        } else if (status == Request.STATUS_FAIL) {
                            onCancel();
                        } else if (status == Request.STATUS_CANCEL) {
                            onCancel();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (Constants.KEY_PROCESS_CHANGE.equals(uri.getFragment())) {
                // 进度改变
                try {
                    @SuppressWarnings("ConstantConditions")
                    long current_bytes = Long.parseLong(
                            uri.getQueryParameter(Constants.KEY_PROCESS));
                    @SuppressWarnings("ConstantConditions")
                    long length = Long.parseLong(
                            uri.getQueryParameter(Constants.KEY_LENGTH));
                    onProgress(current_bytes, length);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (Constants.KEY_BLOCK_PROCESS_CHANGE.equals(uri.getFragment())) {
                // 分片进度改变
                try {
                    try {
                        @SuppressWarnings("ConstantConditions")
                        int index = Integer.parseInt(uri.getQueryParameter(Constants.KEY_PARTIAL_NUM));
                        @SuppressWarnings("ConstantConditions")
                        long current_bytes = Long.parseLong(
                                uri.getQueryParameter(Constants.KEY_PROCESS));
                        if (DEBUG) {
                            Log.d(TAG, index + "分片进度改变=" + current_bytes);
                        }
                        @SuppressWarnings("ConstantConditions")
                        long length = Long.parseLong(
                                uri.getQueryParameter(Constants.KEY_LENGTH));
                        onBlockProcessChange(index, current_bytes, length);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else if (Constants.KEY_BLOCK_STATUS_CHANGE.equals(uri.getFragment())) {
                // 分片状态改变
                try {
                    @SuppressWarnings("ConstantConditions")
                    int status = Integer.parseInt(uri.getQueryParameter(Constants.KEY_STATUS));
                    @SuppressWarnings("ConstantConditions")
                    int index = Integer.parseInt(uri.getQueryParameter(Constants.KEY_PARTIAL_NUM));
                    if (DEBUG) {
                        Log.d(TAG, "分片状态改变：当前状态=" + status);
                    }
                    onBlockStatusChange(index, status);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
//            else if (Constants.KEY_BLOCK_PREPARE.equals(uri.getFragment())) {
//                // 分片信息插入数据库
//                List<PartialInfo> partialInfos = PartialProviderHelper
//                        .queryPartialInfoList(mContext, Utils.getDownloadId(mContext, uri));
//                for (PartialInfo partialInfo : partialInfos) {
//                    InnerBlockObserver blockObserver = new InnerBlockObserver();
//                    mContext.getContentResolver().registerContentObserver(
//                            Utils.generatePartialBUri(mContext, partialInfo.id),
//                            false, blockObserver);
//                    mBlockObserverList.add(blockObserver);
//                }
//            }


        }
    }

//    class InnerBlockObserver extends ContentObserver {
//        /** 上一次下载状态 **/
//        private int mLastState = -1;
//
//        InnerBlockObserver() {
//            super(new Handler(Looper.getMainLooper()));
//        }
//
//        @Override
//        public void onChange(boolean selfChange, Uri uri) {
//            super.onChange(selfChange, uri);
////            if (Constants.KEY_BLOCK_PROCESS_CHANGE.equals(uri.getFragment())) {
////                // 分片进度改变
////                try {
////                    try {
////                        @SuppressWarnings("ConstantConditions")
////                        int index = Integer.parseInt(uri.getQueryParameter(Constants.KEY_PARTIAL_NUM));
////                        @SuppressWarnings("ConstantConditions")
////                        long current_bytes = Long.parseLong(
////                                uri.getQueryParameter(Constants.KEY_PROCESS));
////                        if (DEBUG) {
////                            Log.d(TAG, index + "分片进度改变=" + current_bytes);
////                        }
////                        @SuppressWarnings("ConstantConditions")
////                        long length = Long.parseLong(
////                                uri.getQueryParameter(Constants.KEY_LENGTH));
////                        onBlockProcessChange(index, current_bytes, length);
////                    } catch (Exception e) {
////                        e.printStackTrace();
////                    }
////                } catch (Exception e) {
////                    e.printStackTrace();
////                }
////            }else if (Constants.KEY_BLOCK_STATUS_CHANGE.equals(uri.getFragment())) {
////                // 分片状态改变
////                try {
////                    @SuppressWarnings("ConstantConditions")
////                    int status = Integer.parseInt(uri.getQueryParameter(Constants.KEY_STATUS));
////                    @SuppressWarnings("ConstantConditions")
////                    int index = Integer.parseInt(uri.getQueryParameter(Constants.KEY_PARTIAL_NUM));
////                    if (DEBUG) {
////                        Log.d(TAG, "分片状态改变：当前状态=" + status);
////                    }
////                    onBlockStatusChange(index, status);
////                } catch (Exception e) {
////                    e.printStackTrace();
////                }
////            }
//        }
//    }
    public void onBlockProcessChange(int index, long cur, long length){

    }
    public void onBlockStatusChange(int index, int status){

    }
    public void onStatusChange(int status) {

    }
    @Override
    public void onStart() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onSuccess() {

    }


    @Override
    public void onFail() {

    }

    @Override
    public void onProgress(long cur, long length) {

    }
}
