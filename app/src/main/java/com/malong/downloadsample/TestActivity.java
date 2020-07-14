package com.malong.downloadsample;

import androidx.appcompat.app.AppCompatActivity;

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
import com.malong.download.utils.FileUtils;

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
                Log.d(TAG, "onChange()queryProcess=" + ProviderHelper.queryProcess(mContext,uri));
                Log.d(TAG, "onChange():queryStatus=" + DownloadHelper.queryStatus(mContext,uri));
            }
        };

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pagkage = mContext.getPackageName();
                String CONTENT_AUTHORITY = pagkage + ".downloads";
                Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);// content://com.malong.downloadsample.downloads

//                String downloadUrl = Constants.BASE_URL+Constants.ZHU_XIAN_NAME;
                String downloadUrl = Constants.BASE_URL+Constants.TIK_NAME;
                String fileName = FileUtils.getFileNameFromUrl(downloadUrl);
                String filePath = mContext.getFilesDir() + File.separator + fileName;// /data/user/0/com.malong.downloadsample/files
                // 增
                DownloadInfo info = new DownloadInfo();
                info.status = DownloadInfo.STATUS_PENDING;
                info. download_url= downloadUrl;
                info.destination_path = filePath;
                info.fileName = fileName;
                info.method = DownloadInfo.METHOD_BREAKPOINT;

                Uri uri = DownloadHelper.getInstance().download(mContext, info);
                getContentResolver().registerContentObserver(uri, false, mObserver);


            }
        });

        findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(mContext, DownloadService.class);
                mContext.stopService(intent);
            }
        });




    }


}
