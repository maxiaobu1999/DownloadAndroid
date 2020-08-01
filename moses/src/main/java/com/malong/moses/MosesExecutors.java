package com.malong.moses;

import java.util.ArrayDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MosesExecutors {
    private static final int CORE_POOL_SIZE = 1; // 核心线程数量
    private static final int MAXIMUM_POOL_SIZE = 20;// 最大线程数量
    private static final int BACKUP_POOL_SIZE = 5;// 拒绝策略核心线程数
    private static final int KEEP_ALIVE_SECONDS = 3;// 空闲线超时时间
    // 线程创建工厂
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);// 线程数量

        // 创建线程
        public Thread newThread(Runnable r) {
            // 2线程名称，线程数量自增
            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
        }
    };
    // 备用线程池，当主线程池满了时才会创建。使用这个线程池执行拒绝策略。
    private static ThreadPoolExecutor sBackupExecutor;
    // 备用线程池的队列，无边界容量
    private static LinkedBlockingQueue<Runnable> sBackupExecutorQueue;
    // 线程池的拒绝策略
    private static final RejectedExecutionHandler sRunOnSerialPolicy =
            new RejectedExecutionHandler() {
                // 线程池满了，执行拒绝策略时调用
                public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                    // 单例懒加载
                    synchronized (this) {
                        if (sBackupExecutor == null) {
                            sBackupExecutorQueue = new LinkedBlockingQueue<Runnable>();
                            // 核心5个，最大5，空闲时间3，单位秒，无边界队列，线程工厂与主线程池相同。
                            sBackupExecutor = new ThreadPoolExecutor(
                                    BACKUP_POOL_SIZE, BACKUP_POOL_SIZE, KEEP_ALIVE_SECONDS,
                                    TimeUnit.SECONDS, sBackupExecutorQueue, sThreadFactory);
                            sBackupExecutor.allowCoreThreadTimeOut(true);// 核心线程空闲超时会关闭
                        }
                    }
                    sBackupExecutor.execute(r);// 执行任务
                }
            };
    // 执行任务的线程池
    public static final ExecutorService THREAD_POOL_EXECUTOR;

    // 静态代码块，类加载初始化
    static {
        // 核心1、最大20、超时3、时间单位秒、生产消费队列
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), sThreadFactory);
        threadPoolExecutor.setRejectedExecutionHandler(sRunOnSerialPolicy);// 设置拒绝策略
        THREAD_POOL_EXECUTOR = threadPoolExecutor;
    }

    /**
     * 不是线程池，负责执行任务。
     * 对APP开放， 静态所以进程单例
     */
    public static final Executor SERIAL_EXECUTOR = new SerialExecutor();

    // 不是线程池，负责执行任务。
    private static class SerialExecutor implements Executor {
        // 双端队列，任务列表
        final ArrayDeque<Runnable> mTasks = new ArrayDeque<>();
        Runnable mActive;// 当前任务

        // 1、来了一个新任务。
        public synchronized void execute(final Runnable r) {
            // 2、把任务添加进队列
            mTasks.offer(new Runnable() {
                // 6、scheduleNext()时执行这个run()
                public void run() {
                    try {
                        r.run();// 7、把（final Runnable r）执行了，r一定至队头的任务
                    } finally {
                        scheduleNext();//8、执行完毕，队列里取下一个任务执行
                    }
                }
            });
            // 3、当前没有正在执行的任务
            if (mActive == null) {
                scheduleNext();// 4、从队列中拿任务来执行
            }// 若有mActive！=null不处理，执行完毕会从队列中取任务执行
        }

        // 使用线程池执行下一个，从mTask队列中取下一个任务，如果队列不为null
        protected synchronized void scheduleNext() {
            if ((mActive = mTasks.poll()) != null) {
                THREAD_POOL_EXECUTOR.execute(mActive);// 5、使用线程池执行任务，进入子线程
            }
        }
    }

    /** 任务集合 <下载id,下载任务> */
//    private static HashMap<Integer, FutureTask> mTaskMap = new HashMap<>();
    public static void equeue(FutureTask<Request> futureTask) {
        SERIAL_EXECUTOR.execute(futureTask);
        // 检查分片任务,使用excute()

    }

    public static void excute(FutureTask<Request> futureTask) {
        THREAD_POOL_EXECUTOR.execute(futureTask);
    }

    public static void cancel(FutureTask<Request> futureTask) {
//        mTaskMap.remove(futureTask);
        futureTask.cancel(true);
    }

    //    public static void taskDone(FutureTask<Request> futureTask) {
////        mTaskMap.remove(futureTask);
//        futureTask.cancel(true);
//    }
    // if true 都执行完
    public static boolean checkIdel(FutureTask<Request> futureTask) {
        return THREAD_POOL_EXECUTOR.isTerminated() && sBackupExecutor.isTerminated();
    }
}
