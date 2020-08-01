package com.malong.moses;

import androidx.annotation.Nullable;

public class CancelableThread extends Thread {
    public volatile boolean cancel;
    public CancelableThread(@Nullable Runnable target) {
        super(target);

    }
    public CancelableThread(Runnable target, String name) {
        super(target,name);
    }

    @Override
    public void interrupt() {
        cancel = true;
        //            super.interrupt();
    }

    public void test(){
        retry:
        break retry;
    }



}
