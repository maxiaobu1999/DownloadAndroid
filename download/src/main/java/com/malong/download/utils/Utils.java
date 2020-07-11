package com.malong.download.utils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.malong.download.Constants;
import com.malong.download.DownloadInfo;
import com.malong.download.DownloadService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static String getDownloadBaseUriString(Context context) {
        return "content://" + context.getPackageName() + ".downloads";

    }

    public static Uri getDownloadBaseUri(Context context) {
        return Uri.parse("content://" + context.getPackageName() + ".downloads");

    }

    public static String getDownloadAuthority(Context context) {
        return context.getPackageName() + ".downloads";

    }

    public static Uri getDownloadUri(Context context, int _id) {
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




    public static void deleteItem(Context context,DownloadInfo info) {
        Uri uri = getDownloadUri(context, info.id);
        context.getContentResolver().delete(uri, Constants._ID + "=?",
                new String[]{String.valueOf(info.id)});
    }


}
