package com.malong.download;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.malong.download.utils.Utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.TestCase.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class DownloadServiceTest {
    private static final String TAG = "【DownloadProviderTest】";
    private Context mContext;

    @Before
    public void Prepare() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @Test
    public void testService() {
        Intent intent = new Intent();
        intent.setClass(mContext, DownloadService.class);
        mContext.startService(intent);
        mContext.stopService(intent);
    }

    @Test
    public void testContentObserver() {



    }



}
