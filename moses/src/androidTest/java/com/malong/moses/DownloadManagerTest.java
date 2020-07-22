package com.malong.moses;

import android.Manifest;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.malong.moses.partial.PartialInfo;
import com.malong.moses.partial.PartialProviderHelper;
import com.malong.moses.utils.FileUtils;
import com.malong.moses.utils.Utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;

// adb uninstall com.malong.doDownload.test
@RunWith(AndroidJUnit4.class)
public class DownloadManagerTest {
    @SuppressWarnings("unused")
    private static final String TAG = "【DownloadManagerTest】";
    @SuppressWarnings("FieldCanBeLocal")
    private Context mContext;
    private CountDownLatch countDownLatch;
    // 赋予权限
    @Rule
    public GrantPermissionRule mRuntimePermissionRule =
            GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.INTERNET);

    @Before
    public void prepare() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        countDownLatch = new CountDownLatch(1);
//        mContext.startService(intent);
        //        assertEquals("com.malong.doDownload.test", appContext.packageName)
    }

    // 下载到路径
    @Test
    public void testDownloadPath() {
        String downloadUrl = Constants.BASE_URL + Constants.IMAGE_NAME;
        // /data/user/0/com.malong.downloadsample/
        String filePath = mContext.getFilesDir().getAbsolutePath();//+ File.separator
        Log.d(TAG, filePath);
        String fileName = FileUtils.getFileNameFromUrl(downloadUrl);

        DownloadTask.Builder builder = new DownloadTask.Builder();
        builder.setDownloadUrl(downloadUrl);
        builder.setDescription_path(filePath);
        builder.setFileName(fileName);
        DownloadTask info = builder.build();

        DownloadManager manager = DownloadManager.getInstance();
        Uri uri = manager.doDownload(mContext, info);

        info.id = Utils.getDownloadId(mContext, uri);
        ContentObserver mObserver = new DownloadContentObserver(mContext, uri) {
            @Override
            public void onProcessChange(Uri uri, long cur) {
                super.onProcessChange(uri, cur);
                Log.d(DownloadManagerTest.TAG, "进度发生改变：当前进度=" + cur);
            }

            @Override
            public void onStatusChange(Uri uri, int status) {
                Log.d(DownloadManagerTest.TAG, "状态发生改变：当前状态=" + status);
                if (status == DownloadTask.STATUS_SUCCESS)
                    countDownLatch.countDown();
            }
        };
        mContext.getContentResolver().registerContentObserver(uri, false, mObserver);
        countDownLatch = new CountDownLatch(1);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 删除

        int delete = DownloadManager.getInstance().deleteDownload(mContext, info);
        Assert.assertEquals(1, delete);


    }


    // 下载到路径
    @Test
    public void testDownloadUri() {
        String downloadUrl = Constants.BASE_URL + Constants.IMAGE_NAME;
        // content://media/external_primary/images/media
        // /storage/self/primary/Pictures
        Uri DescriptionUri = MediaStore.Images.Media
                .getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        String fileName = FileUtils.getFileNameFromUrl(downloadUrl);

        DownloadTask.Builder builder = new DownloadTask.Builder();
        builder.setDownloadUrl(downloadUrl);
        builder.setDescription_uri(DescriptionUri);
        builder.setFileName(fileName);
        DownloadTask info = builder.build();


        DownloadManager manager = DownloadManager.getInstance();
        Uri uri = manager.doDownload(mContext, info);
        info.id = Utils.getDownloadId(mContext, uri);
        ContentObserver mObserver = new DownloadContentObserver(mContext, uri) {
            @Override
            public void onProcessChange(Uri uri, long cur) {
                super.onProcessChange(uri, cur);
                Log.d(DownloadManagerTest.TAG, "进度发生改变：当前进度=" + cur);
            }

            @Override
            public void onStatusChange(Uri uri, int status) {
                Log.d(DownloadManagerTest.TAG, "状态发生改变：当前状态=" + status);
//                countDownLatch.countDown();
            }
        };
        mContext.getContentResolver().registerContentObserver(uri, false, mObserver);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    // 下载到路径
    @Test
    public void testErrorUrl() {
        String downloadUrl = Constants.BASE_URL + "123" + Constants.IMAGE_NAME;
        // /data/user/0/com.malong.downloadsample/
        String filePath = mContext.getFilesDir().getAbsolutePath();//+ File.separator
        Log.d(TAG, filePath);
        String fileName = FileUtils.getFileNameFromUrl(downloadUrl);

        DownloadTask.Builder builder = new DownloadTask.Builder();
        builder.setDownloadUrl(downloadUrl);
        builder.setDescription_path(filePath);
        builder.setFileName(fileName);
        DownloadTask info = builder.build();

        DownloadManager manager = DownloadManager.getInstance();
        Uri uri = manager.doDownload(mContext, info);

        info.id = Utils.getDownloadId(mContext, uri);
        ContentObserver mObserver = new DownloadContentObserver(mContext, uri) {
            @Override
            public void onProcessChange(Uri uri, long cur) {
                super.onProcessChange(uri, cur);
                Log.d(DownloadManagerTest.TAG, "进度发生改变：当前进度=" + cur);
            }

            @Override
            public void onStatusChange(Uri uri, int status) {
                Log.d(DownloadManagerTest.TAG, "状态发生改变：当前状态=" + status);
                if (status == DownloadTask.STATUS_SUCCESS || status == DownloadTask.STATUS_FAIL)
                    countDownLatch.countDown();
            }
        };
        mContext.getContentResolver().registerContentObserver(uri, false, mObserver);
        countDownLatch = new CountDownLatch(1);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 删除

        int delete = DownloadManager.getInstance().deleteDownload(mContext, info);
        Assert.assertEquals(1, delete);
    }

    @Test
    public void testContinueDownload() {
        countDownLatch = new CountDownLatch(1);
        String downloadUrl = Constants.BASE_URL + Constants.IMAGE_NAME;
        // /data/user/0/com.malong.downloadsample/
        String filePath = mContext.getFilesDir().getAbsolutePath();//+ File.separator
        Log.d(TAG, filePath);
        String fileName = FileUtils.getFileNameFromUrl(downloadUrl);

        DownloadTask.Builder builder = new DownloadTask.Builder();
        builder.setDownloadUrl(downloadUrl);
        builder.setDescription_path(filePath);
        builder.setFileName(fileName);
        builder.setMethod(DownloadTask.METHOD_BREAKPOINT);
        final DownloadTask info = builder.build();

        // 开始下载
        final DownloadManager manager = DownloadManager.getInstance();
        Uri uri = manager.doDownload(mContext, info);
        info.id = Utils.getDownloadId(mContext, uri);
        // 注册监听
        ContentObserver mObserver = new DownloadContentObserver(mContext, uri) {
            boolean hasStop = false;

            @Override
            public void onProcessChange(Uri uri, long cur) {
                super.onProcessChange(uri, cur);
                Log.d(DownloadManagerTest.TAG, "进度发生改变：当前进度=" + cur);
                // 停止任务
                if (!hasStop && cur > 200) {
                    hasStop = true;
                    manager.pauseDownload(mContext, info);
                    countDownLatch.countDown();
                }
            }

            @Override
            public void onStatusChange(Uri uri, int status) {
                Log.d(DownloadManagerTest.TAG, "状态发生改变：当前状态=" + status);
                if (status == DownloadTask.STATUS_SUCCESS)
                    countDownLatch.countDown();
            }
        };
        mContext.getContentResolver()
                .registerContentObserver(uri, false, mObserver);

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 被停止了
        int status = ProviderHelper.queryStutas(mContext, info.id);
        Assert.assertEquals(DownloadTask.STATUS_PAUSE, status);


        // 继续下载
        manager.resumeDownload(mContext, info);

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 删除
        int delete = DownloadManager.getInstance().deleteDownload(mContext, info);
        Assert.assertEquals(1, delete);

        int i = ProviderHelper.queryStutas(mContext, info.id);
        Assert.assertEquals(-1, i);
    }

    @Test
    public void testPartialDownload() {
        countDownLatch = new CountDownLatch(1);
        String downloadUrl = Constants.BASE_URL + Constants.IMAGE_NAME;
        // /data/user/0/com.malong.downloadsample/
        String filePath = mContext.getFilesDir().getAbsolutePath();//+ File.separator
        Log.d(TAG, filePath);
        String fileName = FileUtils.getFileNameFromUrl(downloadUrl);

        DownloadTask.Builder builder = new DownloadTask.Builder();
        builder.setDownloadUrl(downloadUrl);
        builder.setDescription_path(filePath);
        builder.setFileName(fileName);
        builder.setMethod(DownloadTask.METHOD_PARTIAL);
        builder.setSeparate_num(2);
        final DownloadTask info = builder.build();

        // 开始下载
        final DownloadManager manager = DownloadManager.getInstance();
        Uri uri = manager.doDownload(mContext, info);
        info.id = Utils.getDownloadId(mContext, uri);
        // 注册监听
        ContentObserver mObserver = new DownloadContentObserver(mContext, uri) {
            boolean hasStop = false;

            @Override
            public void onProcessChange(Uri uri, long cur) {
                super.onProcessChange(uri, cur);
                Log.d(DownloadManagerTest.TAG, "进度发生改变：当前进度=" + cur);
                // 停止任务
                if (!hasStop && cur > 200) {
                    hasStop = true;
                    manager.pauseDownload(mContext, info);
                }
            }

            @Override
            public void onStatusChange(Uri uri, int status) {
                Log.d(DownloadManagerTest.TAG, "状态发生改变：当前状态=" + status);
                if (status == DownloadTask.STATUS_SUCCESS)
                    countDownLatch.countDown();
                if (status == DownloadTask.STATUS_PAUSE)
                    countDownLatch.countDown();
            }
        };
        mContext.getContentResolver().registerContentObserver(uri, false, mObserver);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 被停止了
        int status = ProviderHelper.queryStutas(mContext, info.id);
        Assert.assertEquals(DownloadTask.STATUS_PAUSE, status);
        List<PartialInfo> partialInfos = PartialProviderHelper.queryPartialInfoList(mContext, info.id);
        for (PartialInfo partialInfo : partialInfos) {
            Assert.assertTrue((partialInfo.status == PartialInfo.STATUS_STOP)
                    || (partialInfo.status == PartialInfo.STATUS_SUCCESS));
        }


        // 继续下载
        manager.resumeDownload(mContext, info);

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 下载完成了
        DownloadTask doneInfo = ProviderHelper.queryDownloadInfo(mContext, info.id);
        Assert.assertNotNull(doneInfo);
        // ETag : "4df4d61142e773a16769473cf2654b71"
        String md5 = FileUtils.toMd5(new File(info.destination_path, info.fileName), false);
        Assert.assertTrue(TextUtils.equals(doneInfo.etag, md5));// 校验md5
        Assert.assertEquals(doneInfo.total_bytes, doneInfo.current_bytes);
        File file = new File(doneInfo.destination_path, doneInfo.fileName);
        Assert.assertEquals(file.length(), doneInfo.total_bytes);
        // 删除
        int delete = DownloadManager.getInstance().deleteDownload(mContext, info);
        Assert.assertEquals(1, delete);
        // 文件不存在
        boolean existFile = FileUtils.checkFileExist(doneInfo.destination_path, doneInfo.fileName);
        Assert.assertFalse(existFile);
        int i = ProviderHelper.queryStutas(mContext, info.id);
        Assert.assertEquals(-1, i);
    }

}
