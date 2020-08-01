package com.malong.moses;


/** 线程池拒绝策略 */
public interface RejectedExecutionHandler {


    void rejectedExecution(Runnable r, ThreadPoolExecutor executor);
}

