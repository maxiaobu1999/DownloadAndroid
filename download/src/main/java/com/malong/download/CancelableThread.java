package com.malong.download;

import androidx.annotation.Nullable;

public class CancelableThread extends Thread {
    public volatile boolean cancel;
    public CancelableThread(@Nullable Runnable target) {
        super(target);

    }

    @Override
    public void interrupt() {
        cancel = true;
        //            super.interrupt();
    }
}
