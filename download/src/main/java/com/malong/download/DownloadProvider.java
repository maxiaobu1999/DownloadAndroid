package com.malong.download;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.malong.download.utils.Closeables;
import com.malong.download.utils.Utils;

import java.util.ArrayList;

@SuppressWarnings("WeakerAccess")
public final class DownloadProvider {
    public static final String TAG = "【DownloadProvider】";
    private static boolean DEBUG = Constants.DEBUG;
    private Context mContext;
    // 该内容提供者使用的数据库 ，单例不关闭
    private final SQLiteDatabase mDb;

    //    /** ISystemFacade. */
//    ISystemFacade mISystemFacade;

    /** 由DownloadProviderProxy保证单例 */
    public DownloadProvider(Context context) {
        mContext = context;
//        if (mISystemFacade == null) {
//            mISystemFacade = new RealSystemFacade(context);
//        }

        // 该内容提供者使用的数据库
        SQLiteOpenHelper mOpenHelper = DatabaseHelper.getInstance(context);
        mDb = mOpenHelper.getReadableDatabase();
        // Initialize the system uid
//        mSystemUid = Process.SYSTEM_UID;
    }

    public boolean onCreate() {
        return false;
    }

    /**
     * 从ContentProvider中查询数据。使用uri参数来确定查询哪张表,
     *
     * @param uri           CP的授权信息
     * @param projection    参数用于确定查询哪些列，返回哪些列的内容
     * @param selection     约束条件 列名1>? and 列名2!=?
     * @param selectionArgs 约束内容  new String[] { "28", "含含" } ？的值
     * @param sortOrder     对结果进行排序
     * @return 查询的结果存放在Cursor对象中返回。
     */
    @Nullable
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        // 查
        // 参数说明
        // table：要操作的表
        // columns：查询的列所有名称集
        // selection：WHERE之后的条件语句，可以使用占位符
        // groupBy：指定分组的列名
        // having指定分组条件，配合groupBy使用
        // orderBy指定排序的列名
        // limit指定分页参数
        // distinct可以指定“true”或“false”表示要不要过滤重复值
        // 所有方法将返回一个Cursor对象，代表数据集的游标
        return mDb.query(Constants.DB_TABLE,// 查什么表
                projection, // 返回哪些列的数据
                selection,// 查询条件
                selectionArgs,// 参数，？的值
                null, null, sortOrder
        );
    }


    /**
     * 向ContentProvider中添加一条数据。
     *
     * @param uri    CP的授权信息
     * @param values 保存待添加的数据
     * @return 返回一个用于表示这条新记录的URI, 基于_id
     */
    @Nullable
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        // 增
        // 参数1：要操作的表名称
        // 参数2：SQl不允许一个空列，若ContentValues是空，那么这一列被明确的指明为NULL值
        // 参数3：ContentValues对象
        // 新增的主键
        long insertId = mDb.insert(Constants.DB_TABLE, null, values);// 插入数据
        if (insertId == -1) {
            // 插入失败
            return null;
        }
        Uri affectedUri = Uri.parse(Utils.getDownloadBaseUriString(mContext) + "/" + insertId);
        Utils.startdownloadService(mContext);
        mContext.getContentResolver().notifyChange(affectedUri, null);
        return affectedUri;
    }

    /**
     * 从ContentProvider中删除数据。
     * uri为准，约束条件为辅
     *
     * @param uri           CP的授权信息
     * @param selection     约束条件 >? and !=?
     * @param selectionArgs 约束内容  new String[] { "28", "含含" }
     * @return 删除的条数
     */
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // 先查出受影响的条目
        Cursor cursor = mDb.query(Constants.DB_TABLE, new String[]{Constants._ID}
                , selection, selectionArgs, null, null, null);
        if (cursor == null || !cursor.moveToFirst()) {
            // 没有可删除的条目
            return 0;// 就别删了
        }
        ArrayList<Integer> list = new ArrayList<>();
        do {
            list.add(cursor.getInt(0));
        } while (cursor.moveToNext());
        Closeables.closeSafely(cursor);
        int delete = mDb.delete(Constants.DB_TABLE, selection, selectionArgs);
        if (delete == 0) {
            // 一条没删成功，失败了
            return 0;// 别通知CO了
        }
        // 忽略删除量< 应删量
        for (Integer _id : list) {
            Uri affectedUri = Utils.getDownloadUri(mContext, _id);
            // 通知contentObserver
            mContext.getContentResolver().notifyChange(affectedUri, null);
        }
        return delete;
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
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
//        // 先查出受影响的条目
//        Cursor cursor = mDb.query(Constants.DB_TABLE, new String[]{Constants._ID}
//                , selection, selectionArgs, null, null, null);
//        if (cursor == null || !cursor.moveToFirst()) {
//            // 没有可修改的条目
//            return 0;// 就别修改了
//        }
//        ArrayList<Integer> list = new ArrayList<>();// 记录受影响的_id
//        do {
//            list.add(cursor.getInt(0));
//        } while (cursor.moveToNext());
//        Closeables.closeSafely(cursor);
        // 修改
        int update = mDb.update(Constants.DB_TABLE, values, selection, selectionArgs);
//        if (update == 0) {
//            // 一条没改成功，失败了
//            return 0;// 别通知CO了
//        }
//        // 忽略删除量< 应删量
//        for (Integer _id : list) {
//            Uri affectedUri = Utils.getDownloadUri(mContext, _id);
//            // 通知contentObserver
//            mContext.getContentResolver().notifyChange(affectedUri, null);
//        }
        return update;
    }


    /**
     * 根据传入的内容URI来返回相应的MIME类型。
     */
    @Nullable
    public String getType(@NonNull Uri uri) {
        String mimeType = null;
        Cursor cursor = null;
        try {
            cursor = mDb.query(Constants.DB_TABLE,// 查什么表
                    new String[]{Constants.COLUMN_MIME_TYPE}, // 返回哪些列的数据
                    Constants.COLUMN_DESTINATION_URI + "=?",// 查询条件
                    new String[]{uri.toString()},// 参数，？的值
                    null, null, null
            );
            if (cursor == null) return null;// 没有
            mimeType = cursor.getString(0);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Closeables.closeSafely(cursor);
        }
        return mimeType;
    }

}
