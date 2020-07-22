package com.malong.moses;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static junit.framework.TestCase.assertEquals;

// adb uninstall com.malong.doDownload.test
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.JVM)// 按照JVM得到的方法顺序，也就是代码中定义的方法顺序.保证用例按循序执行
public class DatabaseTest {
    @SuppressWarnings("unused")
    private static final String TAG = "【DatabaseTest】";
    @SuppressWarnings("FieldCanBeLocal")
    private Context mContext;
    private DatabaseHelper mHelper;

    @Before
    public void prepare() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mHelper = DatabaseHelper.getInstance(mContext);
        //        assertEquals("com.malong.doDownload.test", appContext.packageName)
    }

    // 增
    @Test
    public void testCURD() {
        String uri = "content://sdfd.sdfdsf.sdf/adfdf.txt";
        String downloadUrl = "https://blog.csdn.net/carson_ho/article/details/53241633";
        String fileName = "wenjian";
        String data = "12345678";

        SQLiteDatabase db = mHelper.getWritableDatabase();
        // 增
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_DESTINATION_URI, uri);
        values.put(Constants.COLUMN_DOWNLOAD_URL, downloadUrl);
        values.put(Constants.COLUMN_FILE_NAME, fileName);
        // 参数1：要操作的表名称
        // 参数2：SQl不允许一个空列，若ContentValues是空，那么这一列被明确的指明为NULL值
        // 参数3：ContentValues对象
        // 新增的主键
        long insertId = db.insert(Constants.DB_TABLE, null, values);// 插入数据

        // 改
        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.COLUMN_DATA, data);// data键 改 12345678
        int updateNum = db.update(Constants.DB_TABLE,// 表名
                contentValues,// 更新的内容
                Constants.COLUMN_FILE_NAME + "=? and " + Constants._ID + "=?",// 约束条件
                new String[]{fileName, String.valueOf(insertId)}// 约束的值【?】
        );
        assertEquals(1, updateNum);// 只会影响一条

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
        Cursor cursor = db.query(Constants.DB_TABLE,// 查什么表
                new String[]{Constants.COLUMN_DESTINATION_URI,
                        Constants.COLUMN_DOWNLOAD_URL,
                        Constants.COLUMN_FILE_NAME,
                        Constants.COLUMN_DATA}, // 返回哪些列的数据
                Constants.COLUMN_FILE_NAME + "=? and _id=?",// 查询条件
                new String[]{fileName, String.valueOf(insertId)},// 参数，？的值
                null, null, null
        );
        cursor.moveToFirst();
        do {
            assertEquals(uri, cursor.getString(0));
            assertEquals(downloadUrl, cursor.getString(1));
            assertEquals(fileName, cursor.getString(2));
            assertEquals(data, cursor.getString(3));
        } while (cursor.moveToNext());

        // 删
        int deleteNum = db.delete(Constants.DB_TABLE,
                "_id=? and " + Constants.COLUMN_FILE_NAME + "=?",// 约束条件
                new String[]{String.valueOf(insertId), "wenjian"}// ?的值
        );
        assertEquals(1, deleteNum);// 删除一条
    }

}
