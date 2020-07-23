package com.malong.moses;

import android.Manifest;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.malong.moses.utils.FileUtils;
import com.malong.moses.utils.Utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

// 普通下载（路径）
@RunWith(AndroidJUnit4.class)
public class CommonPathTest {
    @SuppressWarnings("unused")
    private static final String TAG = "【CommonPathTest】";
    @SuppressWarnings("FieldCanBeLocal")
    private Context mContext;
    // 赋予权限
    @Rule
    public GrantPermissionRule mRuntimePermissionRule =
            GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.INTERNET);
    private Thread mThread;

    @Before
    public void prepare() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mThread = Thread.currentThread();

        DownloadListener downloadListener = new DownloadListener() {
            @Override
            public void onStatusChange(Uri uri, int status) {
                super.onStatusChange(uri, status);
            }
        };
    }


    /**
     * case
     * 下载文件
     * 成功后删除
     */
    @Test
    public void testDownload() {
        String downloadUrl = Constants.BASE_URL + Constants.IMAGE_NAME;
        // /data/user/0/com.malong.downloadsample/
        String filePath = mContext.getFilesDir().getAbsolutePath();
        String fileName = FileUtils.getFileNameFromUrl(downloadUrl);

        DownloadTask.Builder builder = new DownloadTask.Builder();
        builder.setDownloadUrl(downloadUrl);
        builder.setDescription_path(filePath);
        assert fileName != null;
        builder.setFileName(fileName);
        final DownloadTask info = builder.build();


        // 1、下载
        DownloadTask task = Download.doDownload(mContext, info);

        ContentObserver mObserver = new DownloadContentObserver(mContext) {
            @Override
            public void onStatusChange(Uri uri, int status) {
                Log.d(CommonPathTest.TAG, "状态发生改变：当前状态=" + status);
                if (status == DownloadTask.STATUS_SUCCESS)
                    LockSupport.unpark(mThread);
            }
        };
        assert task != null;
        mContext.getContentResolver().registerContentObserver(
                Utils.generateDownloadUri(mContext,task.id),
                false, mObserver);
        LockSupport.park();


        // 下载完成了
        DownloadTask doneInfo = ProviderHelper.queryDownloadInfo(mContext, info.id);
        Assert.assertNotNull(doneInfo);
        // ETag : "4df4d61142e773a16769473cf2654b71"
        String md5 = FileUtils.toMd5(new File(info.destination_path, info.fileName), false);
        Assert.assertTrue(TextUtils.equals(doneInfo.etag, md5));// 校验md5
        Assert.assertEquals(doneInfo.total_bytes, doneInfo.current_bytes);

        File file = new File(doneInfo.destination_path, doneInfo.fileName);
        Assert.assertEquals(file.length(), doneInfo.total_bytes);

        // 2、删除
        int delete = Download.getInstance().deleteDownload(mContext, doneInfo);
        Assert.assertEquals(1, delete);
        // 文件不存在
        boolean existFile = FileUtils.checkFileExist(doneInfo.destination_path, doneInfo.fileName);
        Assert.assertFalse(existFile);
    }

    /**
     * case
     * 下载文件
     * 成功后删除
     */
    @Test
    public void testStop() {
        String downloadUrl = Constants.BASE_URL + Constants.IMAGE_NAME;
        // /data/user/0/com.malong.downloadsample/
        String filePath = mContext.getFilesDir().getAbsolutePath();
        String fileName = FileUtils.getFileNameFromUrl(downloadUrl);

        DownloadTask.Builder builder = new DownloadTask.Builder();
        builder.setDownloadUrl(downloadUrl);
        builder.setDescription_path(filePath);
        Assert.assertNotNull(fileName);
        builder.setFileName(fileName);
         DownloadTask info = builder.build();


        // 1、下载
        DownloadTask task = Download.doDownload(mContext, info);

        ContentObserver mObserver = new DownloadContentObserver(mContext) {
            boolean hasStop = false;

            @Override
            public void onProcessChange(Uri uri, long cur,long length) {
                super.onProcessChange(uri, cur,length);
//                Log.d(CommonPathTest.TAG, "进度发生改变：当前进度=" + cur);
                // 停止任务
                if (!hasStop && cur > 200) {
                    hasStop = true;
                    Download.pauseDownload(mContext, task);
                    LockSupport.unpark(mThread);
                }
            }

            @Override
            public void onStatusChange(Uri uri, int status) {
                Log.d(CommonPathTest.TAG, "状态发生改变：当前状态=" + status);
                if (status == DownloadTask.STATUS_SUCCESS) {
                    LockSupport.unpark(mThread);
                }
            }
        };
        assert task != null;
        mContext.getContentResolver().registerContentObserver(
                Utils.generateDownloadUri(mContext,task.id), false, mObserver);
        LockSupport.park();
        DownloadTask doneInfo;
        // 下载停止了
        doneInfo = ProviderHelper.queryDownloadInfo(mContext, info.id);
        Assert.assertNotNull(doneInfo);
        Assert.assertEquals(doneInfo.status, DownloadTask.STATUS_PAUSE);
        Assert.assertTrue(doneInfo.current_bytes < doneInfo.total_bytes);
        Assert.assertTrue(FileUtils.checkFileExist(doneInfo.destination_path, doneInfo.fileName));

        Download.resumeDownload(mContext, doneInfo);
        LockSupport.park();

        // 下载完成了
        doneInfo = ProviderHelper.queryDownloadInfo(mContext, info.id);
        Assert.assertNotNull(doneInfo);
        // ETag : "4df4d61142e773a16769473cf2654b71"
        String md5 = FileUtils.toMd5(new File(info.destination_path, info.fileName), false);
        Assert.assertTrue(TextUtils.equals(doneInfo.etag, md5));// 校验md5
        Assert.assertEquals(doneInfo.total_bytes, doneInfo.current_bytes);

        File file = new File(doneInfo.destination_path, doneInfo.fileName);
        Assert.assertEquals(file.length(), doneInfo.total_bytes);

        // 2、删除
        int delete = Download.getInstance().deleteDownload(mContext, doneInfo);
        Assert.assertEquals(1, delete);
        // 文件不存在
        boolean existFile = FileUtils.checkFileExist(doneInfo.destination_path, doneInfo.fileName);
        Assert.assertFalse(existFile);
    }

    /**
     * case
     * 下载文件
     * 成功后删除
     */
    @Test
    public void testMultipleDownload() {
        String downloadUrl = Constants.BASE_URL + Constants.IMAGE_NAME;
        // /data/user/0/com.malong.downloadsample/
        String filePath = mContext.getFilesDir().getAbsolutePath();
        String fileName = FileUtils.getFileNameFromUrl(downloadUrl);

        DownloadTask.Builder builder = new DownloadTask.Builder();
        builder.setDownloadUrl(downloadUrl);
        builder.setDescription_path(filePath);
        Assert.assertNotNull(fileName);
        builder.setFileName(fileName);
        final  DownloadTask info = builder.build();


        // 1、下载 两次
        DownloadTask task = Download.doDownload(mContext, info);
        List<DownloadTask> downloadInfos = ProviderHelper.queryByUrl(mContext, downloadUrl);
        Assert.assertEquals(1, downloadInfos.size());
        DownloadTask task2 = Download.doDownload(mContext, info);
        downloadInfos = ProviderHelper.queryByUrl(mContext, downloadUrl);
        Assert.assertEquals(1, downloadInfos.size());
        assert task != null;
        assert task2 != null;
        Assert.assertEquals(task.id, task.id);

        ContentObserver mObserver = new DownloadContentObserver(mContext) {
            boolean hasStop = false;

            @Override
            public void onProcessChange(Uri uri, long cur,long length) {
                super.onProcessChange(uri, cur,length);
//                Log.d(CommonPathTest.TAG, "进度发生改变：当前进度=" + cur);
                // 停止任务
                if (!hasStop && cur > 200) {
                    hasStop = true;
                    Download.pauseDownload(mContext, info);
                    LockSupport.unpark(mThread);
                }
            }

            @Override
            public void onStatusChange(Uri uri, int status) {
                Log.d(CommonPathTest.TAG, "状态发生改变：当前状态=" + status);
                if (status == DownloadTask.STATUS_SUCCESS) {
                    LockSupport.unpark(mThread);
                }
            }
        };
        mContext.getContentResolver().registerContentObserver(
                Utils.generateDownloadUri(mContext,task.id),
                false, mObserver);
        LockSupport.park();
        DownloadTask doneInfo;
        // 下载停止了
        doneInfo = ProviderHelper.queryDownloadInfo(mContext, info.id);
        Assert.assertNotNull(doneInfo);
        Assert.assertEquals(doneInfo.status, DownloadTask.STATUS_PAUSE);
        Assert.assertTrue(doneInfo.current_bytes < doneInfo.total_bytes);
        Assert.assertTrue(FileUtils.checkFileExist(doneInfo.destination_path, doneInfo.fileName));

        Download.doDownload(mContext, doneInfo);

        LockSupport.park();

        // 下载完成了
        doneInfo = ProviderHelper.queryDownloadInfo(mContext, info.id);
        Assert.assertNotNull(doneInfo);
        // ETag : "4df4d61142e773a16769473cf2654b71"
        String md5 = FileUtils.toMd5(new File(info.destination_path, info.fileName), false);
        Assert.assertTrue(TextUtils.equals(doneInfo.etag, md5));// 校验md5
        Assert.assertEquals(doneInfo.total_bytes, doneInfo.current_bytes);

        File file = new File(doneInfo.destination_path, doneInfo.fileName);
        Assert.assertEquals(file.length(), doneInfo.total_bytes);

        // 2、删除
        int delete = Download.getInstance().deleteDownload(mContext, doneInfo);
        Assert.assertEquals(1, delete);
        // 文件不存在
        boolean existFile = FileUtils.checkFileExist(doneInfo.destination_path, doneInfo.fileName);
        Assert.assertFalse(existFile);
    }
}
