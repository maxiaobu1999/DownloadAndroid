package com.malong.download;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.malong.download.partial.PartialInfo;
import com.malong.download.utils.Closeables;
import com.malong.download.utils.Utils;

import java.util.List;

public final class DownloadProvider {
    public static final String TAG = "【DownloadProvider】";
    @SuppressWarnings("PointlessBooleanExpression")
    private static boolean DEBUG = Constants.DEBUG & true;
    /** 匹配到 download表 */
    public static final int URI_MATCHER_DOWNLOAD = 1;
    /** 匹配到 分片partial表 */
    public static final int URI_MATCHER_PARTIAL = 2;

    private Context mContext;
    private UriMatcher URI_MATCHER;
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

        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
//        URI_MATCHER.addURI(AUTHORITY, TABLE_B, TABLE_B_MSG);
        URI_MATCHER.addURI(Utils.getDownloadAuthority(mContext), "/", URI_MATCHER_DOWNLOAD);
        URI_MATCHER.addURI(Utils.getDownloadAuthority(mContext), "/*", URI_MATCHER_DOWNLOAD);
        URI_MATCHER.addURI(Utils.getPartialAuthority(mContext), "/", URI_MATCHER_PARTIAL);
        URI_MATCHER.addURI(Utils.getPartialAuthority(mContext), "/*", URI_MATCHER_PARTIAL);

        // 该内容提供者使用的数据库
        SQLiteOpenHelper mOpenHelper = DatabaseHelper.getInstance(context);
        mDb = mOpenHelper.getWritableDatabase();
        // Initialize the system uid
//        mSystemUid = Process.SYSTEM_UID;
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
        if (URI_MATCHER.match(uri) == URI_MATCHER_DOWNLOAD) {
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
        } else if (URI_MATCHER.match(uri) == URI_MATCHER_PARTIAL) {
            // 分片
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
            return mDb.query(Constants.DB_PARTIAL_TABLE,// 查什么表
                    projection, // 返回哪些列的数据
                    selection,// 查询条件
                    selectionArgs,// 参数，？的值
                    null, null, sortOrder
            );
        }
        return null;

    }


    /**
     * 向ContentProvider中添加一条数据。
     * <p>
     *     主键在这里传不传一样，都会新增主键自增
     *
     * @param uri    CP的授权信息
     * @param values 保存待添加的数据
     * @return 返回一个用于表示这条新记录的URI, 基于_id
     */
    @Nullable
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (URI_MATCHER.match(uri) == URI_MATCHER_DOWNLOAD) {
            // 增,
            // 参数1：要操作的表名称
            // 参数2：SQl不允许一个空列，若ContentValues是空，那么这一列被明确的指明为NULL值
            // 参数3：ContentValues对象
            // 新增的主键
            int insertId = (int) mDb.insert(Constants.DB_TABLE, null, values);// 插入数据
            if (insertId == -1) {
                // 插入失败
                return null;
            }
            return Utils.generateDownloadUri(mContext, insertId);
        } else if (URI_MATCHER.match(uri) == URI_MATCHER_PARTIAL) {
            // 分片
            int insertId = (int) mDb.insert(Constants.DB_PARTIAL_TABLE, null, values);// 插入数据
            if (insertId == -1) {
                // 插入失败
                return null;
            }
            return Utils.generatePartialBUri(mContext, insertId);
        }
        return null;

    }


    /**
     * 更ContentProvider中已有的数据。
     * 1、状态发生改变 2、下载进度发生改变
     *
     * @param uri           用于确定修改的那张表，使用什么CP
     * @param values        新数据保存在values参数中
     * @param selection     约束条件 >? and !=?
     * @param selectionArgs 约束内容  new String[] { "28", "含含" }
     * @return 受影响的行数将作为返回值返回。
     * <p>
     * <p>
     * 1、 首次启动 running2Pending
     * 2、 新增任务启动 Oending2running
     * 3、停止
     * 4、继续下载
     * <p>
     * 进度发生改变
     */
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        if (DEBUG) {
            String sValues = "null";
            StringBuilder builder = new StringBuilder();
            if (values != null) {
                sValues = values.toString();
            }
            if (selectionArgs != null) {
                for (String selectionArg : selectionArgs) {
                    builder.append(selectionArg).append("+");
                }
            }
            Log.d(TAG, "update调用values=" + sValues + ";selection="
                    + selection + ";selectionArgs=" + builder.toString()
            + ";uri="+uri.toString());
        }
        if (values == null) {
            return 0;
        }
        if (URI_MATCHER.match(uri) == URI_MATCHER_DOWNLOAD) {
            // 先查出受影响的条目
            Cursor cursor = mDb.query(Constants.DB_TABLE, new String[]{"*"},
                    selection, selectionArgs, null, null, null);
            List<DownloadInfo> infoList = DownloadInfo.readDownloadInfos(mContext, cursor);
            Closeables.closeSafely(cursor);

            // 修改
            int update = mDb.update(Constants.DB_TABLE, values, selection, selectionArgs);
            if (update == 0) {
                // 一条没改成功，失败了.eg:扫描running2pending
                return 0;// 别通知CO了
            }
            // 这些条目发生了改变
            Uri destUri = null;
            for (DownloadInfo info : infoList) {
                // 1、状态发生改变
                if (values.containsKey(Constants.COLUMN_STATUS)) {
                    // 成功的不能变
                    if (info.status == PartialInfo.STATUS_SUCCESS) {
                        // TODO: 2020-07-17  
                        continue;
                    }
                    int curStatus = (int) values.get(Constants.COLUMN_STATUS);
                    if (info.status != curStatus) {// 保证状态发生改变
                        // 状态发生改变通知 service
                        Bundle bundle = new Bundle();
                        bundle.putInt(Constants.KEY_ID, info.id);
                        bundle.putInt(Constants.KEY_STATUS, curStatus);
                        bundle.putString(Constants.KEY_URI,
                                Utils.generateDownloadUri(mContext, info.id).toString());
                        Utils.startDownloadService(mContext, bundle);
                        destUri = Utils.getDownloadBaseUri(mContext).buildUpon().appendPath(String.valueOf(info.id))
                                .appendQueryParameter(Constants.KEY_STATUS, String.valueOf(curStatus))
                                .appendQueryParameter(Constants.KEY_ID, String.valueOf(info.id))
                                .fragment(Constants.KEY_STATUS_CHANGE).build();
                    }
                } else if (values.containsKey(Constants.COLUMN_CURRENT_BYTES)) {
                    //  2、下载进度发生改变
                    long process = (long) values.get(Constants.COLUMN_CURRENT_BYTES);
                    destUri = Utils.getDownloadBaseUri(mContext).buildUpon().appendPath(String.valueOf(info.id))
                            .appendQueryParameter(Constants.KEY_PROCESS, String.valueOf(process))
                            .fragment(Constants.KEY_PROCESS_CHANGE).build();

                }
                if (destUri != null)
                    mContext.getContentResolver().notifyChange(destUri, null);
            }

            return update;
        } else if (URI_MATCHER.match(uri) == URI_MATCHER_PARTIAL) {
            // 分片
            // 先查出受影响的条目
            Cursor cursor = mDb.query(Constants.DB_PARTIAL_TABLE,
                    new String[]{"*"},
                    selection, selectionArgs, null, null, null);
            List<PartialInfo> infoList = PartialInfo.readPartialInfos(mContext, cursor);
            Closeables.closeSafely(cursor);

            // 修改
            int update = mDb.update(Constants.DB_PARTIAL_TABLE, values, selection, selectionArgs);
            if (update == 0) {
                // 一条没改成功，失败了
                return 0;// 别通知CO了
            }

            // 这些条目发生了改变
            Uri destUri = null;
            for (PartialInfo info : infoList) {
                // 1、状态发生改变
                if (values.containsKey(Constants.PARTIAL_STATUS)) {
                    int curStatus = (int) values.get(Constants.PARTIAL_STATUS);
                    if (info.status != curStatus) {// 保证状态发生改变
                        // 状态发生改变通知 service
                        Bundle bundle = new Bundle();
                        bundle.putInt(Constants.KEY_ID, info.id);
                        bundle.putInt(Constants.KEY_STATUS, curStatus);
                        bundle.putString(Constants.KEY_URI,
                                Utils.generatePartialBUri(mContext, info.id).toString());
                        Utils.startDownloadService(mContext, bundle);
                        destUri = Utils.getPartialBaseUri(mContext).buildUpon().appendPath(String.valueOf(info.id))
                                .appendQueryParameter(Constants.KEY_ID, String.valueOf(info.id))
                                .appendQueryParameter(Constants.KEY_STATUS, String.valueOf(curStatus))
                                .appendQueryParameter(Constants.KEY_PARTIAL_NUM, String.valueOf(info.num))
                                .fragment(Constants.KEY_STATUS_CHANGE).build();
                    }
                } else if (values.containsKey(Constants.PARTIAL_CURRENT_BYTES)) {
                    //  2、下载进度发生改变
                    long process = (long) values.get(Constants.PARTIAL_CURRENT_BYTES);
                    destUri = Utils.getPartialBaseUri(mContext).buildUpon()
                            .appendPath(String.valueOf(info.id))
                            .appendQueryParameter(Constants.KEY_PARTIAL_NUM, String.valueOf(info.num))
                            .appendQueryParameter(Constants.KEY_PROCESS, String.valueOf(process))
                            .fragment(Constants.KEY_PROCESS_CHANGE).build();

                }
                if (destUri != null)
                    mContext.getContentResolver().notifyChange(destUri, null);
            }
            return update;
        }


        return 0;
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
        int delete = 0;
        if (URI_MATCHER.match(uri) == URI_MATCHER_DOWNLOAD) {
            // 先查出受影响的条目
            Cursor cursor = mDb.query(Constants.DB_TABLE, new String[]{"*"},
                    selection, selectionArgs, null, null, null);
            List<DownloadInfo> infoList = DownloadInfo.readDownloadInfos(mContext, cursor);
            Closeables.closeSafely(cursor);
            delete = mDb.delete(Constants.DB_TABLE, selection, selectionArgs);
            if (delete == 0) {
                // 一条没删成功，失败了
                return 0;// 别通知CO了
            }
//        // 忽略删除量< 应删量
//        for (Integer _id : list) {
//            Uri affectedUri = Utils.generateDownloadUri(mContext, _id);
//            // 通知contentObserver
//            mContext.getContentResolver().notifyChange(affectedUri, null);
//        }

            int downloadId = Integer.parseInt(selectionArgs[0]);
            Bundle bundle = new Bundle();
            bundle.putInt(Constants.KEY_ID, downloadId);
            bundle.putInt(Constants.KEY_STATUS, DownloadInfo.STATUS_CANCEL);// 新增一定是 STATUS_CANCEL
            bundle.putString(Constants.KEY_URI, Utils.generateDownloadUri(mContext, downloadId).toString());
            Utils.startDownloadService(mContext, bundle);
            DownloadInfo info = infoList.get(0);
            Uri destUri = Utils.getDownloadBaseUri(mContext).buildUpon().appendPath(String.valueOf(info.id))
                    .appendQueryParameter(Constants.KEY_STATUS, String.valueOf(DownloadInfo.STATUS_CANCEL))
                    .appendQueryParameter(Constants.KEY_ID, String.valueOf(info.id))
                    .fragment(Constants.KEY_STATUS_CHANGE).build();
            mContext.getContentResolver().notifyChange(destUri, null);


        } else if (URI_MATCHER.match(uri) == URI_MATCHER_PARTIAL) {
            // 先查出受影响的条目
            Cursor cursor = mDb.query(Constants.DB_PARTIAL_TABLE, new String[]{"*"},
                    selection, selectionArgs, null, null, null);
            List<PartialInfo> infoList = PartialInfo.readPartialInfos(mContext, cursor);
            Closeables.closeSafely(cursor);
            // 分片
            delete = mDb.delete(Constants.DB_PARTIAL_TABLE, selection, selectionArgs);
            if (delete == 0) {
                // 一条没删成功，失败了
                return 0;// 别通知CO了
            }
            PartialInfo info = infoList.get(0);
            Uri destUri = Utils.getPartialBaseUri(mContext).buildUpon().appendPath(String.valueOf(info.id))
                    .appendQueryParameter(Constants.KEY_ID, String.valueOf(info.id))
                    .appendQueryParameter(Constants.KEY_STATUS, String.valueOf(PartialInfo.STATUS_CANCEL))
                    .appendQueryParameter(Constants.KEY_PARTIAL_NUM, String.valueOf(info.num))
                    .fragment(Constants.KEY_STATUS_CHANGE).build();
            mContext.getContentResolver().notifyChange(destUri, null);

        }


        return delete;
    }

    /**
     * 根据传入的内容URI来返回相应的MIME类型。
     */
    @Nullable
    public String getType(@NonNull Uri uri) {
        if (URI_MATCHER.match(uri) == URI_MATCHER_DOWNLOAD) {
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
        } else if (URI_MATCHER.match(uri) == URI_MATCHER_PARTIAL) {
            // 分片
        }

        return null;

    }

}
