package com.malong.download;

import android.Manifest;
import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.malong.download.utils.FileUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
// 存储测试
//adb shell cmd appops set your-package-name android:legacy_storage allow
// 要禁用兼容模式，请在 Android Q 上卸载并重新安装您的应用，或在终端窗口中运行以下命令：
//adb shell cmd appops set your-package-name android:legacy_storage default

// adb uninstall com.malong.download.test
@RunWith(AndroidJUnit4.class)
public class StorageTest {
    @SuppressWarnings("unused")
    private static final String TAG = "【StorageTest】";
    @SuppressWarnings("FieldCanBeLocal")
    private Context mContext;
    private CountDownLatch countDownLatch;
    @Rule
    public GrantPermissionRule mRuntimePermissionRule =
            GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.INTERNET);

    @Before
    public void prepare() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
//        Intent intent = new Intent();
//        intent.setClass(mContext, DownloadService.class);
//        mContext.startService(intent);
        //        assertEquals("com.malong.download.test", appContext.packageName)
    }

    // 下载到路径
    @Test
    public void testDownloadPath() {
        String downloadUrl = Constants.BASE_URL + Constants.IMAGE_NAME;
        String fileName = FileUtils.getFileNameFromUrl(downloadUrl);
        if (TextUtils.isEmpty(fileName)) {
            fileName = String.valueOf(UUID.fromString(downloadUrl));
        }
        String filePath = mContext.getFilesDir() + File.separator + fileName;// /data/user/0/com.malong.downloadsample/files

        Builder builder = new Builder();
        builder.setDownloadUrl(downloadUrl);
        builder.setDescription_path(filePath);
        builder.setFileName(fileName);
        DownloadInfo info = builder.build();

        DownloadHelper manager = DownloadHelper.getInstance();
        Uri uri = manager.download(mContext, info);
        ContentObserver mObserver = new DownloadContentObserver(mContext, uri) {
            @Override
            public void onStatusChange(int status) {
                Log.d(TAG, "状态发生改变：当前状态=" + status);
            }
        };
        mContext.getContentResolver().registerContentObserver(uri, false, mObserver);
        countDownLatch = new CountDownLatch(1);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // 扫描指定目录
    @Test
    public void scan() {
        ContentResolver mContentResolver = mContext.getContentResolver();

        String[] mediaColumns = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.TITLE, MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_ADDED, MediaStore.Images.Media.DURATION,
                MediaStore.Images.Media.WIDTH, MediaStore.Images.Media.HEIGHT};

        Cursor mCursor = mContentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaColumns,
                null, null, MediaStore.Images.Media.DATE_ADDED);
        assert mCursor != null;

        for (mCursor.moveToFirst(); !mCursor.isAfterLast(); mCursor.moveToNext()) {
            int id = mCursor.getInt(mCursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            Log.d(TAG, "id=" + id);
            String DATA = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
            Log.d(TAG, "DATA=" + DATA);
            String TITLE = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE));
            Log.d(TAG, "TITLE=" + TITLE);
            String MIME_TYPE = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE));
            Log.d(TAG, "MIME_TYPE=" + MIME_TYPE);
            String DISPLAY_NAME = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
            Log.d(TAG, "DISPLAY_NAME=" + DISPLAY_NAME);
            int SIZE = mCursor.getInt(mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE));
            Log.d(TAG, "SIZE=" + SIZE);
            int DATE_ADDED = mCursor.getInt(mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED));
            Log.d(TAG, "DATE_ADDED=" + DATE_ADDED);
            int DURATION = mCursor.getInt(mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DURATION));
            Log.d(TAG, "DURATION=" + DURATION);
            int WIDTH = mCursor.getInt(mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH));
            Log.d(TAG, "WIDTH=" + WIDTH);
            int HEIGHT = mCursor.getInt(mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT));
            Log.d(TAG, "HEIGHT=" + HEIGHT);
            // content://media/external/images/media

//            【StorageTest】: id=543
//            【StorageTest】: DATA=/storage/emulated/0/UCDownloads/pictures/b3a8990620231c872bd8623d98e7b8bb.jpg
//            【StorageTest】: TITLE=b3a8990620231c872bd8623d98e7b8bb
//            【StorageTest】: MIME_TYPE=image/*
//            【StorageTest】: DISPLAY_NAME=b3a8990620231c872bd8623d98e7b8bb.jpg
//            【StorageTest】: SIZE=21790
//            【StorageTest】: DATE_ADDED=1575452508
//            【StorageTest】: DURATION=0
//            【StorageTest】: WIDTH=0
//            【StorageTest】: HEIGHT=0
        }
    }

    @Test
    public void checkUri() {
        Uri uri = Uri.parse("content://media/external/images/media/" + "543");
        ContentResolver resolver = mContext.getContentResolver();
        ParcelFileDescriptor w =null;
        try {
            w=resolver.openFileDescriptor(uri, "w");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        assert w != null;


         uri = Uri.parse("content://media/external/images/media" + "543123123");
        try {
            w = resolver.openFileDescriptor(uri, "w");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
        }
        assert w == null;

    }

    // 将图片存储到 MediaStore.Images 集合所对应的目录
    @Test
    public void writeUri() {
        // content://media/external/images/media
        Log.d(TAG, "MediaStore.Images.Media.EXTERNAL_CONTENT_URI:"
                + MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString());
//        MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon()
//                .appendPath(String.valueOf(id)).build().toString();

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "IMG1024.JPG");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);// 让其他应用不可见

        ContentResolver resolver = mContext.getContentResolver();
        // /sdcard/Pictures/IMG1024.JPG
        // content://media/external_primary/images/media
        Uri collection = MediaStore.Images.Media
                .getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri item = resolver.insert(collection, values);




        // try括号内的资源会在try语句结束后自动释放，前提是这些可关闭的资源必须实现 java.lang.AutoCloseable 接口。
        try (ParcelFileDescriptor pfd = resolver
                .openFileDescriptor(item, "w", null)) {
            DownloadInfo info = new Builder().setDownloadUrl(Constants.BASE_URL + Constants.IMAGE_NAME)
                    .setDescription_uri(item).setFileName("IMG1024.JPG").build();
            Http http = new Http(mContext, info);
            InputStream inputStream = http.getDownloadStream();
            FileOutputStream outputStream = new FileOutputStream(pfd.getFileDescriptor());
            long l = FileUtils.copyStream(inputStream, outputStream);
            Log.d(TAG, "l:" + l);

            // Write data into the pending image.
        } catch (IOException e) {
            e.printStackTrace();
        }


        // 让其他应用不可见
        values.clear();
        values.put(MediaStore.Images.Media.IS_PENDING, 0);
        resolver.update(item, values, null, null);


        // 读
        try {
            AssetFileDescriptor afd = resolver.openAssetFileDescriptor(item, "r");
            Log.d(TAG, "afd.getLength():" + afd.getLength());// 文件大小
            //
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // 删除
        mContext.getContentResolver().delete(item, null, null);

        // 读
        try {
            AssetFileDescriptor afd = resolver.openAssetFileDescriptor(item, "r");
            Log.d(TAG, "afd.getLength():" + afd.getLength());// 文件大小
            //
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void readUri() {
        // content://media/external/images/media
        Log.d(TAG, "MediaStore.Images.Media.EXTERNAL_CONTENT_URI:"
                + MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString());
//        MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon()
//                .appendPath(String.valueOf(id)).build().toString();
    }

    /**
     * AndroidQ以上保存图片到公共目录
     *
     * @param imageName    图片名称
     * @param imageType    图片类型
     * @param relativePath 缓存路径
     */
    private static Uri insertImageFileIntoMediaStore(Context context, String imageName, String imageType,
                                                     String relativePath) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null;
        }
        if (TextUtils.isEmpty(relativePath)) {
            return null;
        }
        Uri insertUri = null;
        ContentResolver resolver = context.getContentResolver();
        //设置文件参数到ContentValues中
        ContentValues values = new ContentValues();
        //设置文件名
        values.put(MediaStore.Images.Media.DISPLAY_NAME, imageName);
        //设置文件描述，这里以文件名代替
        values.put(MediaStore.Images.Media.DESCRIPTION, imageName);
        //设置文件类型为image/*
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/" + imageType);
        //注意：MediaStore.Images.Media.RELATIVE_PATH需要targetSdkVersion=29,
        //故该方法只可在Android10的手机上执行
        values.put(MediaStore.Images.Media.RELATIVE_PATH, relativePath);
        //EXTERNAL_CONTENT_URI代表外部存储器
        Uri external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        //insertUri表示文件保存的uri路径
        insertUri = resolver.insert(external, values);
        return insertUri;
    }
}
