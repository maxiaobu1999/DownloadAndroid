package com.malong.download;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.io.File;

public class Builder {
    private DownloadInfo mInfo;

    public Builder() {
        mInfo = new DownloadInfo();
        // 设置默认值
        mInfo.destination_uri = "";
        mInfo.destination_path = "";

        mInfo.status = DownloadInfo.STATUS_PENDING;
        mInfo.method = DownloadInfo.METHOD_COMMON;
        mInfo.total_bytes = 0;
        mInfo.current_bytes = 0;
    }

    public DownloadInfo build() {
        // 下载地址必须有
        if (TextUtils.isEmpty(mInfo.download_url)) {
            throw new RuntimeException("下载地址必须有");
        }
        if (TextUtils.isEmpty(mInfo.destination_uri) &&
                TextUtils.isEmpty(mInfo.destination_path)) {
            throw new RuntimeException("没有设置保存路径，uri或者path必须有一个");
        }
        // 文件名
        if (TextUtils.isEmpty(mInfo.fileName)) {
            throw new RuntimeException("没有设置文件名");
        }
        if (!TextUtils.isEmpty(mInfo.destination_uri)
                && mInfo.method == DownloadInfo.METHOD_BREAKPOINT) {

        }
        return mInfo;
    }


    public Builder setDownloadUrl(@NonNull String url) {
        if (TextUtils.isEmpty(url)) {
            throw new RuntimeException("下载地址不能为空");
        }
        mInfo.download_url = url;
        return this;
    }

    public Builder setDescription_path(@NonNull String path) {
        if (TextUtils.isEmpty(path)) {
            throw new RuntimeException("保存路径不能为空");
        }
        // 保证以【/】结尾
        if (!path.endsWith(File.separator)) {
            path = path + File.separator;
        }
        mInfo.destination_path = path;
        return this;
    }

    public Builder setDescription_uri(@NonNull Uri uri) {
        if (TextUtils.isEmpty(uri.toString())) {
            throw new RuntimeException("保存Uri不能为空");
        }
        mInfo.destination_uri = uri.toString();
        return this;
    }
    public Builder setFileName(@NonNull String name) {
        if (TextUtils.isEmpty(name)) {
            throw new RuntimeException("文件名不能为空");
        }
        mInfo.fileName = name;
        return this;
    }

    // 设置下载方式
    public Builder setMethod(int method) {
        mInfo.method = method;
        return this;
    }
    // 设置分片数量
    public Builder setSeparate_num(int separate_num) {
        mInfo.separate_num = separate_num;
        return this;
    }
}
