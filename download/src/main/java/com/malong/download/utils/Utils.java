package com.malong.download.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.malong.download.Constants;
import com.malong.download.DownloadInfo;
import com.malong.download.DownloadService;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.io.RandomAccessFile;
import java.net.URI;

public class Utils {
    public static String getDownloadBaseUriString(Context context) {
        return "content://" + context.getPackageName() + ".downloads";

    }

    /** content://com.malong.downloadsample.downloads */
    public static Uri getDownloadBaseUri(Context context) {
        return Uri.parse("content://" + context.getPackageName() + ".downloads");

    }

    public static String getDownloadAuthority(Context context) {
        return context.getPackageName() + ".downloads";

    }

    public static Uri generateDownloadUri(Context context, int _id) {
        return Uri.parse("content://" + context.getPackageName() + ".downloads/" + _id);
    }

    /** 解析下载ID，return -1 if 解析失败 */
    public static int getDownloadId(Context context, Uri uri) {
        int id = -1;
        if (uri == null
                || !"content".equals(uri.getScheme())
                || !getDownloadAuthority(context).equals(uri.getAuthority())
                || TextUtils.isEmpty(uri.getLastPathSegment())) {
            return id;// 检查合法性
        }
        try {
            id = Integer.parseInt(uri.getLastPathSegment());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }

    public static void startdownloadService(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, DownloadService.class);
        context.startService(intent);
    }


    public static void deleteItem(Context context, DownloadInfo info) {
        Uri uri = generateDownloadUri(context, info.id);
        context.getContentResolver().delete(uri, Constants._ID + "=?",
                new String[]{String.valueOf(info.id)});
    }


    /** 检查uri对应的文件是否存在 */
    public static boolean checkUriExit(Context context, Uri uri) {
        ParcelFileDescriptor descriptor = null;
        try {
            descriptor = context.getContentResolver().openFileDescriptor(uri, "w");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
        }
        return descriptor == null;
    }

    /** 检查uri对应的文件是否存在 */
    public static FileOutputStream getOutputStream(Context context, Uri uri) {
        FileOutputStream outputStream = null;
        try {
            ParcelFileDescriptor descriptor = context.getContentResolver().openFileDescriptor(uri, "w");
            if (descriptor != null) {
                FileDescriptor fileDescriptor = descriptor.getFileDescriptor();
                outputStream = new FileOutputStream(fileDescriptor);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
        }
        return outputStream;
    }


    @Nullable
    public static FileOutputStream getOutputStream(Context context, DownloadInfo info) {
        FileOutputStream outputStream = null;
        if (!TextUtils.isEmpty(info.destination_uri)) {
            Uri description_uri = Uri.parse(info.destination_uri);
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, info.fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, info.mime_type);
//            values.put(MediaStore.Images.Media.IS_PENDING, 1);// 让其他应用不可见
            Uri item = context.getContentResolver().insert(description_uri, values);
            return getOutputStream(context, item);
        }


        //noinspection ResultOfMethodCallIgnored
        new File(info.destination_path).mkdirs();
        File destFile = new File(info.destination_path + info.fileName);
        try {
            outputStream = new FileOutputStream(destFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return outputStream;
    }


}
