package com.malong.download;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileNotFoundException;

public class DownloadProviderProxy extends ContentProvider {
    public static final String TAG = "【DownloadProviderProxy】";
    @SuppressWarnings("PointlessBooleanExpression")
    private boolean DEBUG = Constants.DEBUG & false;

    /** 【App】:  attachBaseContext() --调用时机-- onCreate() */
    @Override
    public boolean onCreate() {
        if (DEBUG) {
            Log.d(TAG, "onCreate() ");
        }
        return false;
    }


    /**
     * 根据传入的内容URI来返回相应的MIME类型。
     */
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        if (DEBUG) {
            Log.d(TAG, "getType() ");
        }
        return getProvider().getType(uri);
    }

    /**
     * 向ContentProvider中添加一条数据。
     *
     * @param uri    确定要添加到的表
     * @param values 保存待添加的数据
     * @return 返回一个用于表示这条新记录的URI
     */
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (DEBUG) {
            Log.d(TAG, "insert() ");
        }
        return getProvider().insert(uri, values);
    }

    /**
     * 从ContentProvider中删除数据。
     *
     * @param uri           使用uri参数来确定删除哪一张表中的数据
     * @param selection     约束条件 >? and !=?
     * @param selectionArgs 约束内容  new String[] { "28", "含含" }
     * @return 删除的条数
     */
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        if (DEBUG) {
            Log.d(TAG, "delete() ");
        }
        return getProvider().delete(uri, selection, selectionArgs);
    }

    /**
     * 更ContentProvider中已有的数据。
     *
     * @param uri           使用uri参数来确定更新哪一张表中的数据
     * @param values        新数据保存在values参数中
     * @param selection     约束条件 >? and !=?
     * @param selectionArgs 约束内容  new String[] { "28", "含含" }
     * @return 受影响的行数将作为返回值返回。
     */
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        if (DEBUG) {
            Log.d(TAG, "update() ");
        }
        return getProvider().update(uri, values, selection, selectionArgs);
    }


    /**
     * 从ContentProvider中查询数据。使用uri参数来确定查询哪张表
     *
     * @param uri           uri
     * @param projection    参数用于确定查询哪些列，返回哪些列的内容
     * @param selection     约束条件 列名1>? and 列名2!=?
     * @param selectionArgs 约束内容  new String[] { "28", "含含" } ？的值
     * @param sortOrder     对结果进行排序
     * @return 查询的结果存放在Cursor对象中返回。
     */
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        if (DEBUG) {
            Log.d(TAG, "query() ");
        }
        return getProvider().query(uri, projection, selection, selectionArgs, sortOrder);
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder, @Nullable CancellationSignal cancellationSignal) {
        return query(uri, projection, selection, selectionArgs, sortOrder);
    }

    @Nullable
    @Override
    @Deprecated
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable Bundle queryArgs, @Nullable CancellationSignal cancellationSignal) {
        return super.query(uri, projection, queryArgs, cancellationSignal);
    }


    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        return super.openFile(uri, mode);
    }

    /** ContentProvider代理实体 */
    private volatile DownloadProvider mProvider;

    /**
     * 获取真正的Provider实例对象
     */
    public DownloadProvider getProvider() {
        if (null == mProvider) {
            synchronized (DownloadProviderProxy.class) {
                if (null == mProvider) {
                    mProvider = new DownloadProvider(getContext());
                }
            }
        }
        return mProvider;
    }

}
