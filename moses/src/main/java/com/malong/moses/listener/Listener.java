package com.malong.moses.listener;

public interface Listener {

    void onStart();

    void onPause();

    void onCancel();

    void onSuccess();
    void onFail();

    void onProgress(long cur,long length);
}
