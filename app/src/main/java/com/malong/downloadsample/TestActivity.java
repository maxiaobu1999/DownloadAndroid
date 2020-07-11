package com.malong.downloadsample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.malong.download.Constants;
import com.malong.download.DownloadHelper;
import com.malong.download.DownloadInfo;
import com.malong.download.DownloadService;
import com.malong.download.ProviderHelper;
import com.malong.download.utils.Utils;

import java.io.File;

public class TestActivity extends AppCompatActivity {
    public static final String TAG = "【TestActivity】";
    private Context mContext;
    private ContentObserver mObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        mContext = this;

        Intent intent = new Intent();
        intent.setClass(mContext, DownloadService.class);
        mContext.startService(intent);

        mObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                Log.d(TAG, "onChange()" + uri.toString());
                Log.d(TAG, "onChange()" + ProviderHelper.queryProcess(mContext,uri));
                Log.d(TAG, "onChange():queryStatus=" + DownloadHelper.queryStatus(mContext,uri));
            }
        };

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pagkage = mContext.getPackageName();
                String CONTENT_AUTHORITY = pagkage + ".downloads";
                Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);// content://com.malong.downloadsample.downloads

                String downloadUrl = Constants.BASE_URL+"tik_video/" + "mda-jk8du50gv2jwae5r.mp4";
                String fileName = "mda-jk8du50gv2jwae5r.mp4";
                String filePath = mContext.getFilesDir() + File.separator + fileName;// /data/user/0/com.malong.downloadsample/files
                ContentResolver resolver = mContext.getContentResolver();
                // 增
                ContentValues values = new ContentValues();
                values.put(Constants.COLUMN_DOWNLOAD_URL, downloadUrl);
                values.put(Constants.COLUMN_DESTINATION_PATH, filePath);
                values.put(Constants.COLUMN_FILE_NAME, fileName);
                values.put(Constants.COLUMN_STATUS, DownloadInfo.STATUS_PENDING);
                values.put(Constants.COLUMN_METHOD, DownloadInfo.METHOD_COMMON);
//                values.put(Constants.COLUMN_TOTAL_BYTES, 10711777);
                // 参数1：要操作的表名称
                // 参数2：SQl不允许一个空列，若ContentValues是空，那么这一列被明确的指明为NULL值
                // 参数3：ContentValues对象
                Uri insertUri = resolver.insert(BASE_CONTENT_URI, values);

                getContentResolver().registerContentObserver(insertUri,
                        false, mObserver);


            }
        });

    }


}
