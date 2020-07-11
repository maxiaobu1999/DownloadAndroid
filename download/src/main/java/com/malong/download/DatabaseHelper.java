package com.malong.download;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * 第一次创建
 * onCreate onOpen()
 */
public final class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "【DatabaseHelper】";
    private boolean DEBUG = Constants.DEBUG;
    /** 单例. */
    private static volatile DatabaseHelper mDbOpenHelper;

    private DatabaseHelper(final Context context) {
        super(context, Constants.DB_NAME, null, Constants.DB_VERSION);
    }

    public static DatabaseHelper getInstance(final Context context) {
        if (null == mDbOpenHelper) {
            synchronized (DatabaseHelper.class) {
                if (null == mDbOpenHelper) {
                    mDbOpenHelper = new DatabaseHelper(context);
                }
            }
        }
        return mDbOpenHelper;
    }

    /**
     * 数据库第1次创建时 则会调用
     * <p>
     * 即 第1次调用 getWritableDatabase（） / getReadableDatabase（）时调用
     */
    @Override
    public void onCreate(final SQLiteDatabase db) {
        if (DEBUG)
            Log.d(TAG, "onCreate(final SQLiteDatabase db)    执行");
        createDownloadsTable(db);// 建新表
    }

    @Override
    public void onOpen(final SQLiteDatabase db) {
        if (DEBUG)
            Log.d(TAG, "onOpen(final SQLiteDatabase db)   执行");
        // 检查downloads表，当表不正常时进行重建
        if (!checkDownloadTable(db)) {
            onUpgrade(db, 0, Constants.DB_VERSION);
        }
    }

    /**
     * 数据库升级时自动调用
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (DEBUG)
            Log.d(TAG, "onUpgrade()     执行，oldVersion=" + oldVersion + ",newVersion=" + newVersion);
        if (oldVersion == newVersion) {

        } else //noinspection StatementWithEmptyBody
            if (newVersion > oldVersion) {
                // 数据库升级，1、久表重命名为临时表2、建新表3、复制旧表数据4、删除旧表
            }

    }

    /**
     * 数据库降级
     * <p>
     * 修复降级导致的crash问题
     * SQLiteException: Can't downgrade database from version xxx to xxx
     */
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion)     执行");

    }

    /**
     * 创建下载信息表.
     *
     * @param db SQLiteDatabase
     */
    private void createDownloadsTable(SQLiteDatabase db) {
        if (DEBUG)
            Log.d(TAG, "createDownloadsTable(SQLiteDatabase db)     执行");
        try {
            db.execSQL("DROP TABLE IF EXISTS " + Constants.DB_TABLE);
            db.execSQL("CREATE TABLE " + Constants.DB_TABLE + "("
                    + Constants._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + Constants.COLUMN_DOWNLOAD_URL + " TEXT, "
                    + Constants.COLUMN_DESTINATION_URI + " TEXT, "
                    + Constants.COLUMN_DESTINATION_PATH + " TEXT, "
                    + Constants.COLUMN_FILE_NAME + " TEXT, "
                    + Constants.COLUMN_METHOD + " INTEGER, "


                    + Constants.COLUMN_MIME_TYPE + " TEXT, "
                    + Constants.COLUMN_LAST_MOD + " BIGINT, "
                    + Constants.COLUMN_STATUS + " INTEGER, "
                    + Constants.COLUMN_TOTAL_BYTES + " BIGINT, "
                    + Constants.COLUMN_CURRENT_BYTES + " BIGINT, "
                    + Constants.COLUMN_HEADER + " TEXT, "
                    + Constants.COLUMN_SPEED + " TEXT, "
                    + Constants.COLUMN_ETAG + " TEXT, "
                    + Constants.COLUMN_ALLOWED_NETWORK + " INTEGER, "
                    + Constants.COLUMN_AUTO_RESUME + " BOOLEAN, "
                    + Constants.COLUMN_PRIORITY + " INTEGER, "
                    + Constants.COLUMN_VISIBLE + " BOOLEAN, "
                    + Constants.COLUMN_INTEGRITY + " BOOLEAN, "
                    + Constants.COLUMN_ENTITY + " TEXT, "
                    + Constants.COLUMN_DATA + " TEXT, "
                    + Constants.COLUMN_SEPARATE + " BOOLEAN, "
                    + Constants.COLUMN_SEPARATE_NUM + " INTEGER, "
                    + Constants.COLUMN_SEPARATE_DONE + " TEXT, "
                    + Constants.COLUMN_TITLE + " TEXT, "
                    + Constants.COLUMN_DESCRIPTION + " , "
                    + Constants.COLUMN_MEDIA_SCANNED + " BOOLEAN, "
                    + Constants.COLUMN_NOTIFICATION_CLASS + " TEXT, "
                    + Constants.COLUMN_NOTIFICATION_EXTRAS + " TEXT);");
        } catch (SQLException ex) {
            Log.e(TAG, "下载的表download创建失败");
            throw ex;
        }
    }


//    /**
//     * insert() now ensures these four columns are never null for new downloads, so this method
//     * makes that true for existing columns, so that code can rely on this assumption.
//     *
//     * @param db SQLiteDatabase
//     */
//    private void fillNullValues(SQLiteDatabase db) {
//        ContentValues values = new ContentValues();
//        values.put(Downloads.Impl.COLUMN_CURRENT_BYTES, 0);
//        fillNullValuesForColumn(db, values);
//        values.put(Downloads.Impl.COLUMN_TOTAL_BYTES, -1);
//        fillNullValuesForColumn(db, values);
//        values.put(Downloads.Impl.COLUMN_TITLE, "");
//        fillNullValuesForColumn(db, values);
//        values.put(Downloads.Impl.COLUMN_DESCRIPTION, "");
//        fillNullValuesForColumn(db, values);
//    }
//
//    /**
//     * fillNullValuesForColumn
//     *
//     * @param db     SQLiteDatabase
//     * @param values ContentValues
//     */
//    private void fillNullValuesForColumn(SQLiteDatabase db, ContentValues values) {
//        String column = values.valueSet().iterator().next().getKey();
//        db.update(Constants.DB_TABLE, values, column + " is null", null);
//        values.clear();
//    }

////    /**
////     * Set all existing downloads to the cache partition to be invisible in the downloads UI.
////     *
////     * @param db db
////     */
////    private void makeCacheDownloadsInvisible(SQLiteDatabase db) {
////        ContentValues values = new ContentValues();
////        values.put(Downloads.Impl.COLUMN_IS_VISIBLE_IN_DOWNLOADS_UI, false);
////        String cacheSelection = Downloads.Impl.COLUMN_DESTINATION
////                + " != " + Downloads.Impl.DESTINATION_EXTERNAL;
////        db.update(Constants.DB_TABLE, values, cacheSelection, null);
////    }
//
//    /**
//     * Add a column to a table using ALTER TABLE.
//     *
//     * @param db               db.
//     * @param dbTable          name of the table
//     * @param columnName       name of the column to add
//     * @param columnDefinition SQL for the column definition
//     */
//    private void addColumn(SQLiteDatabase db, String dbTable, String columnName,
//                           String columnDefinition) {
//        db.execSQL("ALTER TABLE " + dbTable + " ADD COLUMN " + columnName + " "
//                + columnDefinition);
//    }


//    /**
//     * createHeadersTable
//     *
//     * @param db SQLiteDatabase
//     */
//    private void createHeadersTable(SQLiteDatabase db) {
//        db.execSQL("DROP TABLE IF EXISTS " + Downloads.Impl.RequestHeaders.HEADERS_DB_TABLE);
//        db.execSQL("CREATE TABLE " + Downloads.Impl.RequestHeaders.HEADERS_DB_TABLE + "("
//                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
//                + Downloads.Impl.RequestHeaders.COLUMN_DOWNLOAD_ID + " INTEGER NOT NULL,"
//                + Downloads.Impl.RequestHeaders.COLUMN_HEADER + " TEXT NOT NULL,"
//                + Downloads.Impl.RequestHeaders.COLUMN_VALUE + " TEXT NOT NULL"
//                + ");");
//    }

    /**
     * 检查downloads表是否正常，判断方法是判断表中的字段是否存在
     *
     * @param db {@link SQLiteDatabase}
     * @return true，表正常；false，表不正常
     */
    private boolean checkDownloadTable(final SQLiteDatabase db) {
//        Cursor cursor = null;
//        try {
//            cursor = db.query(Constants.DB_TABLE, null, null, null, null, null, null, "1");
//            if (cursor != null) {
//                if (cursor.getColumnIndex(Downloads.Impl._ID) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_DESTINATION_URI) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Constants.RETRY_AFTER_X_REDIRECT_COUNT) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_APP_DATA) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_NO_INTEGRITY) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_FILE_NAME_HINT) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Constants.OTA_UPDATE) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.DATA) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_MIME_TYPE) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_DESTINATION) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Constants.NO_SYSTEM_FILES) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_VISIBILITY) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_CONTROL) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_STATUS) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Constants.FAILED_CONNECTIONS) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_LAST_MODIFICATION) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_NOTIFICATION_PACKAGE) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_NOTIFICATION_CLASS) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_NOTIFICATION_EXTRAS) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_COOKIE_DATA) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_USER_AGENT) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_REFERER) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_TOTAL_BYTES) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_CURRENT_BYTES) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Constants.ETAG) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Constants.UID) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_OTHER_UID) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_TITLE) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_DESCRIPTION) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Constants.MEDIA_SCANNED) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_IS_PUBLIC_API) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_ALLOW_ROAMING) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_ALLOWED_NETWORK_TYPES) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_IS_VISIBLE_IN_DOWNLOADS_UI) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_BYPASS_RECOMMENDED_SIZE_LIMIT) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_MEDIAPROVIDER_URI) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_DELETED) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_RANGE_START_BYTE) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_RANGE_END_BYTE) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_RANGE) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_BOUNDARY) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_DOWNLOAD_MOD) < 0) {
//                    return false;
//                }
//                if (cursor.getColumnIndex(Downloads.Impl.COLUMN_EXTRA_INFO) < 0) {
//                    return false;
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            Closeables.closeSafely(cursor);
//        }
        return true;
    }

}

