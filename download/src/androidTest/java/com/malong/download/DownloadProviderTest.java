package com.malong.download;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.malong.download.utils.Utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNotSame;

@RunWith(AndroidJUnit4.class)
public class DownloadProviderTest {
    private static final String TAG = "【DownloadProviderTest】";
    private Context mContext;

    @Before
    public void Prepare() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();

    }

    @Test
    public void testUri() {
        Uri downloadUri = Utils.getDownloadUri(mContext, 11);
        String path = downloadUri.getPath();// /downloads/11
        Log.d(TAG, "path="+path);
        String authority = downloadUri.getAuthority();// com.malong.download.test
        Log.d(TAG, "authority="+authority);
        String host = downloadUri.getHost();// com.malong.download.test
        Log.d(TAG, "host="+host);
        String lastPathSegment = downloadUri.getLastPathSegment();// 11
        Log.d(TAG, "lastPathSegment="+lastPathSegment);
        String fragment = downloadUri.getFragment();// null 最后用#分隔的部分
        Log.d(TAG, "fragment="+fragment);
        String scheme = downloadUri.getScheme();// content
        Log.d(TAG, "scheme="+scheme);


    }
    @Test
    public void testCURD() {
        String pagkage = mContext.getPackageName();
        String CONTENT_AUTHORITY = pagkage + ".downloads";
        Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);// content://com.malong.downloadsample.downloads

        String uri = "content://sdfd.sdfdsf.sdf/adfdf.txt";
        String downloadUrl = "https://blog.csdn.net/carson_ho/article/details/53241633";
        String fileName = "wenjian";
        String data = "12345678";

        ContentResolver resolver = mContext.getContentResolver();
        // 增
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_DESTINATION_URI, uri);
        values.put(Constants.COLUMN_DOWNLOAD_URL, downloadUrl);
        values.put(Constants.COLUMN_FILE_NAME, fileName);
        // 参数1：要操作的表名称
        // 参数2：SQl不允许一个空列，若ContentValues是空，那么这一列被明确的指明为NULL值
        // 参数3：ContentValues对象
        Uri insertUri = resolver.insert(BASE_CONTENT_URI, values);
        assertNotNull(insertUri);
        int downloadId = Utils.getDownloadId(mContext, insertUri);
        assertNotSame(downloadId,-1);

        // 改
        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.COLUMN_DATA, data);// data键 改 12345678
        int updateNum = resolver.update(BASE_CONTENT_URI,
                contentValues,// 更新的内容
                Constants.COLUMN_FILE_NAME + "=? and " + Constants._ID + "=?",// 约束条件
                new String[]{fileName, String.valueOf(downloadId)}// 约束的值【?】
        );
        assertEquals(1, updateNum);// 只会影响一条

        Cursor cursor = resolver.query(BASE_CONTENT_URI,
                new String[]{Constants.COLUMN_DESTINATION_URI,
                        Constants.COLUMN_DOWNLOAD_URL,
                        Constants.COLUMN_FILE_NAME,
                        Constants.COLUMN_DATA}, // 返回哪些列的数据
                Constants.COLUMN_FILE_NAME + "=? and " + Constants._ID + "=?",// 查询条件
                new String[]{fileName, String.valueOf(downloadId)},// 参数，？的值
                null);
        assertNotNull(cursor);
        cursor.moveToFirst();
        do {
            assertEquals(uri, cursor.getString(0));
            assertEquals(downloadUrl, cursor.getString(1));
            assertEquals(fileName, cursor.getString(2));
            assertEquals(data, cursor.getString(3));
        } while (cursor.moveToNext());

        // 删
        int deleteNum = resolver.delete(BASE_CONTENT_URI,
                Constants._ID + "=?",// 约束条件
                new String[]{String.valueOf(downloadId)}// ?的值
        );
        assertEquals(1, deleteNum);// 删除一条
    }
}
