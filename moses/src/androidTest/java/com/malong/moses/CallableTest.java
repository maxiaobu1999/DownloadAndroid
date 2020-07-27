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

import com.malong.moses.connect.Connection;
import com.malong.moses.connect.HttpInfo;
import com.malong.moses.connect.ResponseInfo;
import com.malong.moses.utils.FileUtils;
import com.malong.moses.utils.Utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

// 断点下载（路径）
@RunWith(AndroidJUnit4.class)
public class CallableTest {
    @SuppressWarnings("unused")
    private static final String TAG = "【CallableTest】";
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
    }


    /**
     * case
     * 下载时长3978 43m
     * 下载慢不是httpUrlClient引起的
     */
    @Test
    public void testDownload() {
        String downloadUrl =
                "https://cdn.llscdn.com/yy/files/xs8qmxn8-lls-LLS-5.8-800-20171207-111607.apk";

//        String downloadUrl = Constants.BASE_URL + Constants.IMAGE_NAME;
        // /data/user/0/com.malong.downloadsample/
        String filePath = mContext.getFilesDir().getAbsolutePath();
        String fileName = FileUtils.getFileNameFromUrl(downloadUrl);

        HttpInfo httpInfo = new HttpInfo();
        httpInfo.download_url = downloadUrl;
        httpInfo.destination_path = filePath;
        httpInfo.fileName = fileName;
        httpInfo.method = Request.METHOD_BREAKPOINT;

        Connection connection = new Connection(mContext, httpInfo);
        // 请求服务器，获取输入流
        InputStream is = connection.getInputStream();
        // 获取输出流
        //noinspection ResultOfMethodCallIgnored
        new File(filePath).mkdirs();
        File destFile = new File(filePath + fileName);// 输出文件
        FileOutputStream os;
        long start = System.currentTimeMillis();
        try {
            os = new FileOutputStream(destFile, true);
            final int defaultBufferSize = 1024 * 3;
            byte[] buf = new byte[defaultBufferSize];
            long size = 0;
            int len;
            while ((len = is.read(buf)) > 0) {
                os.write(buf, 0, len);
                size += len;
                Log.d(TAG, "len:" + len);
            }
            os.flush();
            long end = System.currentTimeMillis();
            Log.d(TAG, "end-start:" + (end - start));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


//        // 1、下载
//        final Download manager = Download.getInstance();
//        Request task = manager.doDownload(mContext, info);
//
//        ContentObserver mObserver = new DownloadContentObserver(mContext) {
//            @Override
//            public void onStatusChange(Uri uri, int status) {
//                Log.d(CallableTest.TAG, "状态发生改变：当前状态=" + status);
//                if (status == Request.STATUS_SUCCESS)
//                    LockSupport.unpark(mThread);
//            }
//        };
//        assert task != null;
//        mContext.getContentResolver().registerContentObserver(
//                Utils.generateDownloadUri(mContext, task.id),
//                false, mObserver);
//        LockSupport.park();
//
//
//        // 下载完成了
//        Request doneInfo = ProviderHelper.queryDownloadInfo(mContext, info.id);
//        Assert.assertNotNull(doneInfo);
//        // ETag : "4df4d61142e773a16769473cf2654b71"
//        String md5 = FileUtils.toMd5(new File(info.destination_path, info.fileName), false);
//        Assert.assertTrue(TextUtils.equals(doneInfo.etag, md5));// 校验md5
//        Assert.assertEquals(doneInfo.total_bytes, doneInfo.current_bytes);
//
//        File file = new File(doneInfo.destination_path, doneInfo.fileName);
//        Assert.assertEquals(file.length(), doneInfo.total_bytes);
//
//        // 2、删除
//        int delete = Download.getInstance().deleteDownload(mContext, doneInfo);
//        Assert.assertEquals(1, delete);
//        // 文件不存在
//        boolean existFile = FileUtils.checkFileExist(doneInfo.destination_path, doneInfo.fileName);
//        Assert.assertFalse(existFile);
//    }catch (IOException e) {
//            e.printStackTrace();
    }

}
