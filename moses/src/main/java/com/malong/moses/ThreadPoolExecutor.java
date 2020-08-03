package com.malong.moses;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 1、thread 可以放弃：加了Looper再用可能会有问题
 * 2、查询线程池当前是否执行完毕：闲置时关闭service
 */
public class ThreadPoolExecutor extends AbstractExecutorService {
    public static final String TAG = "【ThreadPoolExecutor】";
    @SuppressWarnings("PointlessBooleanExpression")
    private static boolean DEBUG = Constants.DEBUG & true;
    // add by me BEGIN

    /** 当前线程是否执行完毕 */
    public boolean isIdle() {
        if (DEBUG)
            Log.d(TAG, "isIdle()：workQueue.size()="
                    + workQueue.size() + "；workers=" + workers.size());
        if (workQueue.size() > 0) {
            return false;
        }
        for (Worker worker : workers) {
            if (DEBUG) Log.d(TAG, "worker.firstTask:" + worker.firstTask);
            if (DEBUG) Log.d(TAG, "worker.curTask:" + worker.curTask);

            if (worker.firstTask != null || worker.curTask != null) {
                return false;
            }
        }
        return true;
    }
    // add by me END


    /**
     * 利用ctl来保证当前线程池的状态和当前的线程的数量。ps：低29位为线程池容量，高3位为线程状态。
     * <p>
     * runState是整个线程池的运行生命周期，有如下取值：
     * *  1. RUNNING：可以新加线程，同时可以处理queue中的线程。
     * *  2. SHUTDOWN：不增加新线程，但是处理queue中的线程。
     * *  3.STOP 不增加新线程，同时不处理queue中的线程。
     * *  4.TIDYING 所有的线程都终止了（queue中），同时workerCount为0，那么此时进入TIDYING
     * *  5.terminated()方法结束，变为TERMINATED
     * <p>
     * 状态的转化主要是：
     * RUNNING -> SHUTDOWN
     * 手动调用shutdown方法，或者ThreadPoolExecutor要被GC回收的时候调用finalize方法，finalize方法内部也会调用shutdown方法
     * <p>
     * (RUNNING or SHUTDOWN) -> STOP：调用shutdownNow方法
     * SHUTDOWN -> TIDYING：当队列和线程池都为空的时候
     * STOP -> TIDYING：When pool is empty当线程池为空的时候
     * TIDYING -> TERMINATED：terminated方法调用完成之后
     * <p>
     * Threads waiting in awaitTermination() will return when the
     * state reaches TERMINATED.
     * <p>
     * Detecting the transition from SHUTDOWN to TIDYING is less
     * straightforward than you'd like because the queue may become
     * empty after non-empty and vice versa during SHUTDOWN state, but
     * we can only terminate if, after seeing that it is empty, we see
     * that workerCount is 0 (which sometimes entails a recheck -- see
     * below).
     */
    //
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    /** 设定偏移量 即29 */
    private static final int COUNT_BITS = Integer.SIZE - 3;
    /** 线程最大的容量 2^29-1 低29位均为1 */
    private static final int CAPACITY = (1 << COUNT_BITS) - 1;

    /** 可以接受新的任务，也可以处理阻塞队列里的任务。高三位111，其他0 */
    private static final int RUNNING = -1 << COUNT_BITS;
    /** shutdown()不接受新的任务，但是可以处理阻塞队列里的任务。高三位000 */
    private static final int SHUTDOWN = 0 << COUNT_BITS;
    /** shutdownNow(),接受新的任务，不处理阻塞队列里的任务，中断正在处理的任务。001 */
    private static final int STOP = 1 << COUNT_BITS;
    /** 所有的线程都终止了（queue中），同时workerCount为0，那么此时进入TIDYING。010 */
    private static final int TIDYING = 2 << COUNT_BITS;
    /** terminated()方法结束，变为TERMINATED。011 */
    private static final int TERMINATED = 3 << COUNT_BITS;


    /** 获取线程池状态，取前三位 */
    private static int runStateOf(int c) {
        return c & ~CAPACITY;
    }

    /** 获取当前正在工作的worker数量,基于后面29位 */
    private static int workerCountOf(int c) {
        return c & CAPACITY;
    }

    /** 获取ctl */
    private static int ctlOf(int rs, int wc) {
        return rs | wc;
    }


    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    private static boolean runStateAtLeast(int c, int s) {
        return c >= s;
    }

    /** true 没调用过 shutdown() */
    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }

    /**
     * worker数量+1.
     *
     * @param expect worker数量
     * @return true 当前值==内存值，可以+1
     */
    private boolean compareAndIncrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect + 1);
    }

    /** ctl减1 。Attempts to CAS-decrement the workerCount field of ctl. */
    private boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }

    /** 减小ctl的workerCount字段. 仅线程突然终止(see processWorkerExit). 其他减少操作在 getTask(). */
    private void decrementWorkerCount() {
        //noinspection StatementWithEmptyBody
        do {
        } while (!compareAndDecrementWorkerCount(ctl.get()));
    }

    /** 阻塞任务队列 */
    private final BlockingQueue<Runnable> workQueue;

    /**
     * 锁{@link ThreadPoolExecutor#workers}，线程池成员变量
     * Lock held on access to workers set and related bookkeeping.
     * While we could use a concurrent set of some sort, it turns out
     * to be generally preferable to use a lock. Among the reasons is
     * that this serializes interruptIdleWorkers, which avoids
     * unnecessary interrupt storms, especially during shutdown.
     * Otherwise exiting threads would concurrently interrupt those
     * that have not yet interrupted. It also simplifies some of the
     * associated statistics bookkeeping of largestPoolSize etc. We
     * also hold mainLock on shutdown and shutdownNow, for the sake of
     * ensuring workers set is stable while separately checking
     * permission to interrupt and actually interrupting.
     */
    private final ReentrantLock mainLock = new ReentrantLock();

    /** 保存 worker */
    private final HashSet<Worker> workers = new HashSet<>();

    /** 用于唤醒。实现wait和notify的功能。 awaitTermination. */
    private final Condition termination = mainLock.newCondition();

    /** 记录着线程池中出现过的最大线程数量 */
    private int largestPoolSize;

    /**
     * 完成的任务数
     * Counter for completed tasks. Updated only on termination of
     * worker threads. Accessed only under mainLock.
     */
    private long completedTaskCount;

    /** 线程工厂 */
    private volatile ThreadFactory threadFactory;

    /** 拒绝策略，队列饱和 or shutdown() 触发 */
    private volatile RejectedExecutionHandler handler;

    /** 非核心线程超时时长 */
    private volatile long keepAliveTime;

    /** If true, 核心线程使用 {@link ThreadPoolExecutor#keepAliveTime} 超时回收. */
    private volatile boolean allowCoreThreadTimeOut;

    /** 核心线程数 */
    private volatile int corePoolSize;

    /** 最大线程数. 最大不会超过2^29幂. */
    private volatile int maximumPoolSize;

    /** 默认的拒绝处理器 */
    private static final RejectedExecutionHandler defaultHandler =
            new ThreadPoolExecutor.AbortPolicy();

    /**
     * Permission required for callers of shutdown and shutdownNow.
     * We additionally require (see checkShutdownAccess) that callers
     * have permission to actually interrupt threads in the worker set
     * (as governed by Thread.interrupt, which relies on
     * ThreadGroup.checkAccess, which in turn relies on
     * SecurityManager.checkAccess). Shutdowns are attempted only if
     * these checks pass.
     * <p>
     * All actual invocations of Thread.interrupt (see
     * interruptIdleWorkers and interruptWorkers) ignore
     * SecurityExceptions, meaning that the attempted interrupts
     * silently fail. In the case of shutdown, they should not fail
     * unless the SecurityManager has inconsistent policies, sometimes
     * allowing access to a thread and sometimes not. In such cases,
     * failure to actually interrupt threads may disable or delay full
     * termination. Other uses of interruptIdleWorkers are advisory,
     * and failure to actually interrupt will merely delay response to
     * configuration changes so is not handled exceptionally.
     */
    private static final RuntimePermission shutdownPerm =
            new RuntimePermission("modifyThread");

    /**
     * Class Worker mainly maintains interrupt control state for
     * threads running tasks, along with other minor bookkeeping.
     * This class opportunistically extends AbstractQueuedSynchronizer
     * to simplify acquiring and releasing a lock surrounding each
     * task execution.  This protects against interrupts that are
     * intended to wake up a worker thread waiting for a task from
     * instead interrupting a task being run.  We implement a simple
     * non-reentrant mutual exclusion lock rather than use
     * ReentrantLock because we do not want worker tasks to be able to
     * reacquire the lock when they invoke pool control methods like
     * setCorePoolSize.  Additionally, to suppress interrupts until
     * the thread actually starts running tasks, we initialize lock
     * state to a negative value, and clear it upon start (in
     * runWorker).
     * 线程池中的每一个线程被封装成一个Worker对象，ThreadPool维护的其实就是一组Worker对象，
     * 1、继承了AQS, 实现独占锁的功能,不允许重入.用于判断线程是否空闲以及是否可以被中断。
     * <p>
     * (1)lock方法一旦获取了独占锁，表示当前线程正在执行任务中；
     * (2)如果正在执行任务，则不应该中断线程；
     * (3)如果该线程现在不是独占锁的状态，也就是空闲的状态，说明它没有在处理任务，这时可以对该线程进行中断；
     * (4)线程池在执行shutdown方法或tryTerminate方法时会调用interruptIdleWorkers方法来中断空闲的线程，interruptIdleWorkers方法会使用tryLock方法来判断线程池中的线程是否是空闲状态；
     * (5)之所以设置为不可重入，是因为我们不希望任务在调用像setCorePoolSize这样的线程池控制方法时重新获取锁。如果使用ReentrantLock，它是可重入的，这样如果在任务中调用了如setCorePoolSize这类线程池控制的方法，会中断正在运行的线程。
     * <p>
     * 2、实现了Runnable接口,线程执行的就是这个run()
     */
    // 这里发现它是实现了AQS，是一个不可重入的独占锁模式
// 并且它还集成了Runable接口，实现了run方法。
    private final class Worker
            extends AbstractQueuedSynchronizer// 同步fifo队列，双向链表保存<线程，状态>并让其阻塞
            implements Runnable {
        //  1 CANCELLED：当前线程被取消；
        //  -1 SIGNAL：当前节点的后继节点需要运行；
        //  -2 CONDITION：当前节点在等待condition；
        // -3 PROPAGATE：当前场景下后续的acquireShared可以执行；
        //  0 默认值，当前节点在sync队列中等待获取锁。
        /**
         * This class will never be serialized, but we provide a
         * serialVersionUID to suppress a javac warning.
         */
        private static final long serialVersionUID = 6138294804551838833L;

        /** 执行任务的线程，通过ThreadFactory创建 */
        final Thread thread;
        /** 线程池提交的任务，执行完之后会从队列中在取一个。创建worker时赋值，执行完之后永远是null */
        Runnable firstTask;
        /** 记录这个线程完成了多少任务 */
        volatile long completedTasks;

        /** add by me 当前正在执行的任务 */
        Runnable curTask;

        /**
         * 首先现将state值设置为-1，因为在AQS中state=0代表的是锁没有被占用，
         * 而且在线程池中shutdown方法会判断能否争抢到锁，如果可以获得锁则对线程进行中断操作，
         * 如果调用了shutdownNow它会判断state>=0会被中断。
         * firstTask第一个任务，如果为空则会从队列中获取任务，后面runWorker中。
         */
        Worker(Runnable firstTask) {
            // 禁止在执行任务前对线程进行中断。
            setState(-1); // -1 SIGNAL：当前节点的后继节点需要运行；
            this.firstTask = firstTask;// 线程池执行的任务
            this.thread = getThreadFactory().newThread(this);// 用线程工厂创建一个新线程
        }

        /** 线程执行的run() */
        public void run() {
            runWorker(this);
        }

        // 独占模式下，判断同步状态是否已经被占用。
        //
        // true 没锁。The value 0 represents the unlocked state.
        // false 锁了。The value 1 represents the locked state.
        protected boolean isHeldExclusively() {
            return getState() != 0;
        }

        /**
         * 独占式同步状态获取
         * 该方法的实现需要先查询当前的同步状态是否可以获取，如果可以获取再进行获取；
         *
         * @return
         */
        protected boolean tryAcquire(int unused) {
            if (compareAndSetState(0, 1)) {
                // 设置当前拥有独占访问权限的线程。空参数表示没有线程拥有访问权。此方法不会强制任何同步或易失性字段访问。
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        // 释放状态；
        protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        public void lock() {
            acquire(1);
        }

        public boolean tryLock() {
            return tryAcquire(1);
        }

        public void unlock() {
            release(1);
        }

        public boolean isLocked() {
            return isHeldExclusively();
        }

        //这里就是上面shutdownNow中调用的线程中断的方法，getState()>=0
        void interruptIfStarted() {
            Thread t;
            if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
                try {
                    t.interrupt();
                } catch (SecurityException ignore) {
                }
            }
        }
    }

    /*
     * Methods for setting control state
     */

    /**
     * Transitions runState to given target, or leaves it alone if
     * already at least the given target.
     *
     * @param targetState the desired state, either SHUTDOWN or STOP
     *                    (but not TIDYING or TERMINATED -- use tryTerminate for that)
     */
    private void advanceRunState(int targetState) {
        // assert targetState == SHUTDOWN || targetState == STOP;
        for (; ; ) {
            int c = ctl.get();
            if (runStateAtLeast(c, targetState) ||
                    ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c))))
                break;
        }
    }

    /**
     * 尝试终止线程池，根据线程池状态进行判断是否结束线程池
     * Transitions to TERMINATED state if either (SHUTDOWN and pool
     * and queue empty) or (STOP and pool empty).  If otherwise
     * eligible to terminate but workerCount is nonzero, interrupts an
     * idle worker to ensure that shutdown signals propagate. This
     * method must be called following any action that might make
     * termination possible -- reducing worker count or removing tasks
     * from the queue during shutdown. The method is non-private to
     * allow access from ScheduledThreadPoolExecutor.
     */
    final void tryTerminate() {
        for (; ; ) {
            int c = ctl.get();// 获取线程池的状态和线程池的数量组合状态
            /*
             * 当前线程池的状态为以下几种情况时，直接返回：
             * 1. RUNNING，因为还在运行中，不能停止；
             * 2. TIDYING或TERMINATED，因为线程池中已经没有正在运行的线程了；
             * 3. SHUTDOWN并且等待队列非空，这时要执行完workQueue中的task；
             */
            if (isRunning(c) ||
                    runStateAtLeast(c, TIDYING) ||
                    (runStateOf(c) == SHUTDOWN && !workQueue.isEmpty()))
                return;
            // 如果线程数量不为0，则中断一个空闲的工作线程，并返回
            if (workerCountOf(c) != 0) { // Eligible to terminate
                interruptIdleWorkers(ONLY_ONE);
                return;
            }

            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                // 这里尝试设置状态为TIDYING，如果设置成功，则调用terminated方法
                if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                    try {
                        terminated();//默认是空的，留给子类实现
                    } finally {
                        ctl.set(ctlOf(TERMINATED, 0));//修改状态为TERMINATED
                        termination.signalAll();//唤醒调用awaitTermination方法的线程
                    }
                    return;
                }
            } finally {
                mainLock.unlock();
            }
            // else retry on failed CAS
        }
    }

    /*
     * Methods for controlling interrupts to worker threads.
     */

    /**
     * If there is a security manager, makes sure caller has
     * permission to shut down threads in general (see shutdownPerm).
     * If this passes, additionally makes sure the caller is allowed
     * to interrupt each worker thread. This might not be true even if
     * first check passed, if the SecurityManager treats some threads
     * specially.
     */
    private void checkShutdownAccess() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(shutdownPerm);
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                for (Worker w : workers)
                    security.checkAccess(w.thread);
            } finally {
                mainLock.unlock();
            }
        }
    }

    /**
     * 中断所有的Worker
     * Interrupts all threads, even if active. Ignores SecurityExceptions
     * (in which case some threads may remain uninterrupted).
     */
    private void interruptWorkers() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers)
                w.interruptIfStarted();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 中断线程 ，即调用thread的interrupt()
     * Interrupts threads that might be waiting for tasks (as
     * indicated by not being locked) so they can check for
     * termination or configuration changes. Ignores
     * SecurityExceptions (in which case some threads may remain
     * uninterrupted).
     *
     * @param onlyOne If true, interrupt at most one worker. This is
     *                called only from tryTerminate when termination is otherwise
     *                enabled but there are still other workers.  In this case, at
     *                most one waiting worker is interrupted to propagate shutdown
     *                signals in case all threads are currently waiting.
     *                Interrupting any arbitrary thread ensures that newly arriving
     *                workers since shutdown began will also eventually exit.
     *                To guarantee eventual termination, it suffices to always
     *                interrupt only one idle worker, but shutdown() interrupts all
     *                idle workers so that redundant workers exit promptly, not
     *                waiting for a straggler task to finish.
     *                是空闲线程
     *                <p>
     *                这里主要是为了中断worker，但是中断之前需要先获取锁，这就意味着正在运行的Worker不能中断。
     *                但是上面的代码有w.tryLock()，那么获取不到锁就不会中断，
     *                shutdown的Interrupt只是对所有的空闲Worker（正在从workQueue中取Task，此时Worker没有加锁）
     *                发送中断信号。
     */
    private void interruptIdleWorkers(boolean onlyOne) {
        final ReentrantLock mainLock = this.mainLock;// 线程池的成员变量
        mainLock.lock();// 其他线程不能访问线程池
        //这里的意图很简单，遍历workers 对所有worker做中断处理。
        // w.tryLock()对Worker加锁，这保证了正在运行执行Task的Worker不会被中断，那么能中断哪些线程呢？
        try {
            for (Worker w : workers) {
                Thread t = w.thread;
                if (!t.isInterrupted() && w.tryLock()) {
                    try {
                        t.interrupt();
                    } catch (SecurityException ignore) {
                    } finally {
                        w.unlock();
                    }
                }
                if (onlyOne)
                    break;
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Common form of interruptIdleWorkers, to avoid having to
     * remember what the boolean argument means.
     */
    private void interruptIdleWorkers() {
        interruptIdleWorkers(false);
    }

    private static final boolean ONLY_ONE = true;

    /*
     * Misc utilities, most of which are also exported to
     * ScheduledThreadPoolExecutor
     */

    /**
     * Invokes the rejected execution handler for the given command.
     * Package-protected for use by ScheduledThreadPoolExecutor.
     */
    final void reject(Runnable command) {
        handler.rejectedExecution(command, this);
    }

    /**
     * Performs any further cleanup following run state transition on
     * invocation of shutdown.  A no-op here, but used by
     * ScheduledThreadPoolExecutor to cancel delayed tasks.
     */
    void onShutdown() {
    }

    /**
     * State check needed by ScheduledThreadPoolExecutor to
     * enable running tasks during shutdown.
     *
     * @param shutdownOK true if should return true if SHUTDOWN
     */
    final boolean isRunningOrShutdown(boolean shutdownOK) {
        int rs = runStateOf(ctl.get());
        return rs == RUNNING || (rs == SHUTDOWN && shutdownOK);
    }

    /**
     * Drains the task queue into a new list, normally using
     * drainTo. But if the queue is a DelayQueue or any other kind of
     * queue for which poll or drainTo may fail to remove some
     * elements, it deletes them one by one.
     */
    private List<Runnable> drainQueue() {
        BlockingQueue<Runnable> q = workQueue;
        ArrayList<Runnable> taskList = new ArrayList<>();
        q.drainTo(taskList);
        if (!q.isEmpty()) {
            for (Runnable r : q.toArray(new Runnable[0])) {
                if (q.remove(r))
                    taskList.add(r);
            }
        }
        return taskList;
    }

    /*
     * Methods for creating, running and cleaning up after workers
     */

    /**
     * 创建一个Worker
     * 检查是否可以根据当前池状态和给定的界限（核心或最大值）添加新的辅助进程。如果是，则相应地调整worker计数，如果可能，将创建并启动一个新的worker，并将firstTask作为其第一个任务运行。如果池已停止或符合关闭条件，则此方法返回false。如果线程工厂在被询问时未能创建线程，它也会返回false。如果线程创建失败，可能是由于线程工厂返回null，或者是由于异常（在中通常是OutOfMemoryError线程启动（）），我们干净利落地后退
     * Checks if a new worker can be added with respect to current
     * pool state and the given bound (either core or maximum). If so,
     * the worker count is adjusted accordingly, and, if possible, a
     * new worker is created and started, running firstTask as its
     * first task. This method returns false if the pool is stopped or
     * eligible to shut down. It also returns false if the thread
     * factory fails to create a thread when asked.  If the thread
     * creation fails, either due to the thread factory returning
     * null, or due to an exception (typically OutOfMemoryError in
     * Thread.start()), we roll back cleanly.
     *
     * @param firstTask the task the new thread should run first (or
     *                  null if none). Workers are created with an initial first task
     *                  (in method execute()) to bypass queuing when there are fewer
     *                  than corePoolSize threads (in which case we always start one),
     *                  or when the queue is full (in which case we must bypass queue).
     *                  Initially idle threads are usually created via
     *                  prestartCoreThread or to replace other dying workers.
     * @param core      if true 限制添加线程的数量是根据corePoolSize来判断
     *                  if false 根据maximumPoolSize来判断；
     * @return true if successful
     */
    private boolean addWorker(Runnable firstTask, boolean core) {
        // flag 用于跳出循环
        // break retry 跳到retry处，且不再进入循环
        // continue retry 跳到retry处，且再次进入循环
        retry:
        // 这个循环目的：worker数量+1
        for (; ; ) {
            int c = ctl.get();
            int rs = runStateOf(c);// 线程池状态 ，高三位
            // (状态不是RUNNING&&不是首次启动)拒绝添加
            if (rs >= SHUTDOWN &&
                    !(rs == SHUTDOWN && firstTask == null && !workQueue.isEmpty()))
                return false;

            for (; ; ) {
                int wc = workerCountOf(c);// 当前worker数量
                if (wc >= CAPACITY || wc >= (core ? corePoolSize : maximumPoolSize))
                    return false;
                // worker数量+1
                if (compareAndIncrementWorkerCount(c))
                    break retry;
                c = ctl.get();  // +1没成功重试
                if (runStateOf(c) != rs)
                    continue retry;
            }
        }

        boolean workerStarted = false;// 线程start成功？
        boolean workerAdded = false;// 添加成功？
        Worker w = null;
        try {
            w = new Worker(firstTask);
            final Thread t = w.thread;
            if (t != null) {
                final ReentrantLock mainLock = this.mainLock;
                mainLock.lock();
                try {
                    // 线程池的状态.
                    int rs = runStateOf(ctl.get());
                    // 同样检测当rs>SHUTDOWN时直接拒绝减小Wc，同时Terminate，
                    // 如果为SHUTDOWN同时firstTask不为null的时候也要Terminate
                    if (rs < SHUTDOWN || (rs == SHUTDOWN && firstTask == null)) {
                        if (t.isAlive()) // precheck that t is startable
                            throw new IllegalThreadStateException();
                        workers.add(w);// 保存一下worker
                        int s = workers.size();
                        if (s > largestPoolSize)// 线程池中出现过的最大线程数量
                            largestPoolSize = s;
                        workerAdded = true;
                    }
                } finally {
                    mainLock.unlock();
                }
                if (workerAdded) {
                    t.start(); // 启动线程，执行的run就是worker自身
                    workerStarted = true;
                }
            }
        } finally {
            if (!workerStarted)
                addWorkerFailed(w);
        }
        return workerStarted;
    }

    /**
     * Rolls back the worker thread creation.
     * - removes worker from workers, if present
     * - decrements worker count
     * - rechecks for termination, in case the existence of this
     * worker was holding up termination
     */
    private void addWorkerFailed(Worker w) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (w != null)
                workers.remove(w);
            decrementWorkerCount();
            tryTerminate();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Performs cleanup and bookkeeping for a dying worker. Called
     * only from worker threads. Unless completedAbruptly is set,
     * assumes that workerCount has already been adjusted to account
     * for exit.  This method removes thread from worker set, and
     * possibly terminates the pool or replaces the worker if either
     * it exited due to user task exception or if fewer than
     * corePoolSize workers are running or queue is non-empty but
     * there are no workers.
     *
     * @param w                 the worker
     * @param completedAbruptly if the worker died due to user exception
     */
    private void processWorkerExit(Worker w, boolean completedAbruptly) {
        // 如果突然完成则调整线程数量
        if (completedAbruptly) // If abrupt, then workerCount wasn't adjusted
            decrementWorkerCount();// 减少线程数量1

        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            completedTaskCount += w.completedTasks;    // 统计整个线程池完成的数量
            workers.remove(w);
        } finally {
            mainLock.unlock();
        }
        //尝试设置线程池状态为TERMINATED
        //1.如果线程池状态为SHUTDOWN并且线程池线程数量与工作队列为空时，修改状态。
        //2.如果线程池状态为STOP并且线程池线程数量为空时，修改状态。
        tryTerminate();

        int c = ctl.get();    // 获取线程池的状态和线程池的数量
        // 如果线程池的状态小于STOP，也就是SHUTDOWN或RUNNING状态
        if (runStateLessThan(c, STOP)) {
            //如果不是突然完成，也就是正常结束
            if (!completedAbruptly) {
                //如果指定allowCoreThreadTimeOut=true(默认false)则代表线程池中有空余线程时需要进行清理操作，
                // 否则线程池中的线程应该保持corePoolSize
                int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
                // 这里判断如果线程池中队列为空并且线程数量最小为0时，将最小值调整为1，
                // 因为队列中还有任务没有完成需要增加队列，所以这里增加了一个线程。
                if (min == 0 && !workQueue.isEmpty())
                    min = 1;
                if (workerCountOf(c) >= min)
                    return; // replacement not needed
            }
            // 如果当前线程数效益核心个数，就增加一个Worker
            addWorker(null, false);
        }
    }

    /**
     * todo 循环的位置
     * 工作线程调用getTask从任务队列中进行获取任务。
     * 死循环的获取任务，if任务队列空了，那么这个线程就阻塞了。阻塞超时停止阻塞，线程自然被JVM回收。核心线程就一直阻塞
     * 如果指定了allowCoreThreadTimeOut或线程池线程数量大于corePoolSize则进行清除空闲多余的线程，调用阻塞队列的poll方法，在指定时间内如果没有获取到任务直接返回false。
     * 如果线程池中线程池数量小于corePoolSize或者allowCoreThreadTimeOut为false默认值，则进行阻塞线程从队列中获取任务，直到队列有任务唤醒线程。
     * <p>
     * Performs blocking or timed wait for a task, depending on
     * current configuration settings, or returns null if this worker
     * must exit because of any of:
     * 1. There are more than maximumPoolSize workers (due to
     * a call to setMaximumPoolSize).
     * 2. The pool is stopped.
     * 3. The pool is shutdown and the queue is empty.
     * 4. This worker timed out waiting for a task, and timed-out
     * workers are subject to termination (that is,
     * {@code allowCoreThreadTimeOut || workerCount > corePoolSize})
     * both before and after the timed wait, and if the queue is
     * non-empty, this worker is not the last thread in the pool.
     *
     * @return task, or null if the worker must exit, in which case
     * workerCount is decremented
     * 队列中获取线程
     */
    private Runnable getTask() {
        // 主要是判断后面的poll是否超时
        boolean timedOut = false; // Did the last poll() time out?

        for (; ; ) {
            int c = ctl.get();//获取线程池的状态和线程数量
            int rs = runStateOf(c);//获取线程池的状态
            /*
             * 如果线程池状态rs >= SHUTDOWN，也就是非RUNNING状态，再进行以下判断：
             * 1. rs >= STOP，线程池是否正在stop；
             * 2. 阻塞队列是否为空。
             * 如果以上条件满足，则将workerCount减1并返回null。
             * 因为如果当前线程池状态的值是SHUTDOWN或以上时，不允许再向阻塞队列中添加任务。
             */
            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
                decrementWorkerCount();
                return null;
            }

            int wc = workerCountOf(c);//获取线程池数量。

            // timed变量用于判断是否需要进行超时控制。
            // allowCoreThreadTimeOut默认是false，也就是核心线程不允许进行超时；
            // wc > corePoolSize，表示当前线程池中的线程数量大于核心线程数量；
            // 对于超过核心线程数量的这些线程，需要进行超时控制
            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;

            /*
             * wc > maximumPoolSize的情况是因为可能在此方法执行阶段同时执行了setMaximumPoolSize方法；
             * timed && timedOut 如果为true，表示当前操作需要进行超时控制，并且上次从阻塞队列中获取任务发生了超时
             * 接下来判断，如果有效线程数量大于1，或者阻塞队列是空的，那么尝试将workerCount减1；
             * 如果减1失败，则返回重试。
             * 如果wc == 1时，也就说明当前线程是线程池中唯一的一个线程了。
             */
            if ((wc > maximumPoolSize || (timed && timedOut))
                    && (wc > 1 || workQueue.isEmpty())) {
                if (compareAndDecrementWorkerCount(c))
                    return null;
                continue;
            }

            try {
                // 根据timed来判断，如果为true，则通过阻塞队列的poll方法进行超时控制，
                // 如果在keepAliveTime时间内没有获取到任务，则返回null；
                // 否则通过take方法，如果这时队列为空，则take方法会阻塞直到队列不为空。
                Runnable r = timed ?
                        workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) ://等待一段时间如果没有获取到返回null。
                        workQueue.take();//阻塞当前线程
                if (r != null)
                    return r;
                timedOut = true; // 如果 r == null，说明已经超时，timedOut设置为true
            } catch (InterruptedException retry) {
                timedOut = false;// 如果获取任务时当前线程发生了中断，则设置timedOut为false并返回循环重试
            }
        }
    }

    /**
     * run方法的具体实现:
     * 1、执行任务的run(),2、循环，执行完一个再去队列拿一个，3、队列没任务就阻塞
     * Worker可能还是执行一个初始化的task——firstTask。
     * 但是有时也不需要这个初始化的task（可以为null）,只要pool在运行，就会
     * 通过getTask从队列中获取Task，如果返回null，那么worker退出。
     * 另一种就是external抛出异常导致worker退出
     * 1. We may start out with an initial task, in which case we
     * don't need to get the first one. Otherwise, as long as pool is
     * running, we get tasks from getTask. If it returns null then the
     * worker exits de to changed pool state or configuration
     * parameters.  Other exits result from exception throws in
     * external code, in which case completedAbruptly holds, which
     * usually leads processWorkerExit to replace this thread.
     * <p>
     * 2 在运行任何task之前，都需要对worker加锁来防止other pool中断worker。
     * clearInterruptsForTaskRun保证除了线程池stop，那么现场都没有中断标志
     * 2. Before running any task, the lock is acquired to prevent
     * other pool interrupts while the task is executing, and then we
     * ensure that unless pool is stopping, this thread does not have
     * its interrupt set.
     * <p>
     * 3. Each task run is preceded by a call to beforeExecute, which
     * might throw an exception, in which case we cause thread to die
     * (breaking loop with completedAbruptly true) without processing
     * the task.
     * <p>
     * 4. Assuming beforeExecute completes normally, we run the task,
     * gathering any of its thrown exceptions to send to afterExecute.
     * We separately handle RuntimeException, Error (both of which the
     * specs guarantee that we trap) and arbitrary Throwables.
     * Because we cannot rethrow Throwables within Runnable.run, we
     * wrap them within Errors on the way out (to the thread's
     * UncaughtExceptionHandler).  Any thrown exception also
     * conservatively causes thread to die.
     * <p>
     * 5. After task.run completes, we call afterExecute, which may
     * also throw an exception, which will also cause thread to
     * die. According to JLS Sec 14.20, this exception is the one that
     * will be in effect even if task.run throws.
     * <p>
     * The net effect of the exception mechanics is that afterExecute
     * and the thread's UncaughtExceptionHandler have as accurate
     * information as we can provide about any problems encountered by
     * user code.
     *
     * @param w the worker
     */
    final void runWorker(Worker w) {
        Thread wt = Thread.currentThread();// 调用者也就是Worker中的线程
        Runnable task = w.firstTask;// 获取任务
        w.firstTask = null;//将Worker中的任务清除代表执行了第一个任务了，后面如果再有任务就从队列中获取。
        w.curTask = task;// add by me
        w.unlock();  // 允许线程中断
        boolean completedAbruptly = true;// 标识线程是不是异常终止的
        try {
            // 如果task为空，则从任务队列中再获取一个任务
            while (task != null || (task = getTask()) != null) {
                w.curTask = task;// add by me
                w.lock();//先获取worker的独占锁，防止其他线程调用了shutdown方法。
                // 如果线程池正在停止，确保线程是被中断的，如果没有则确保线程不被中断操作。
                if ((runStateAtLeast(ctl.get(), STOP)
                        || (Thread.interrupted() && runStateAtLeast(ctl.get(), STOP)))
                        && !wt.isInterrupted())
                    wt.interrupt();
                try {
                    //执行任务之前做一些操作，可进行自定义
                    beforeExecute(wt, task);//线程开始执行之前执行此方法，可以实现Worker未执行退出，本类中未实现
                    Throwable thrown = null;
                    try {
                        task.run();// 运行任务在这里喽。
                    } catch (RuntimeException x) {
                        x.printStackTrace();
                        thrown = x;
                        throw x;
                    } catch (Error x) {
                        x.printStackTrace();
                        thrown = x;
                        throw x;
                    } catch (Throwable x) {
                        x.printStackTrace();
                        thrown = x;
                        throw new Error(x);
                    } finally {
                        afterExecute(task, thrown);//线程执行后执行，可以实现标识Worker异常中断的功能，本类中未实现
                    }
                } finally {
                    Log.d(TAG, "+++++++++");
                    w.curTask = null;// add by me
                    task = null;// 运行过的task标null
                    w.completedTasks++;// 统计当前Worker完成了多少任务
                    w.unlock();// 独占锁释放
                }
            }
            completedAbruptly = false;
        } finally {

            // 处理Worker的退出操作，执行清理工作。
            processWorkerExit(w, completedAbruptly);
        }
    }

    // Public constructors and methods

    /**
     * 构造.
     *
     * @param corePoolSize    核心线程数 unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize 最大线程数量
     * @param keepAliveTime   非核心线程超时时长.
     * @param unit            the {@code keepAliveTime} 时间单位
     * @param workQueue       任务队列.  只能 {@code Runnable}，submitted by the {@code execute} method.
     * @throws IllegalArgumentException if one of the following holds:<br>
     *                                  {@code corePoolSize < 0}核心线程数不能为负<br>
     *                                  {@code keepAliveTime < 0}<br>
     *                                  {@code maximumPoolSize <= 0}<br>
     *                                  {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException     if {@code workQueue} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), defaultHandler);
    }

    /**
     * 构造.
     *
     * @param corePoolSize    核心线程数 unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize 最大线程数量
     * @param keepAliveTime   非核心线程超时时长.
     * @param unit            the {@code keepAliveTime} 时间单位
     * @param workQueue       任务队列.  只能 {@code Runnable}，submitted by the {@code execute} method.
     * @param threadFactory   线程工厂
     * @throws IllegalArgumentException if one of the following holds:<br>
     *                                  {@code corePoolSize < 0}<br>
     *                                  {@code keepAliveTime < 0}<br>
     *                                  {@code maximumPoolSize <= 0}<br>
     *                                  {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException     if {@code workQueue}
     *                                  or {@code threadFactory} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                threadFactory, defaultHandler);
    }

    /**
     * 构造.
     *
     * @param corePoolSize    核心线程数 unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize 最大线程数量
     * @param keepAliveTime   非核心线程超时时长.
     * @param unit            the {@code keepAliveTime} 时间单位
     * @param workQueue       任务队列.  只能 {@code Runnable}，submitted by the {@code execute} method.
     * @param handler         拒绝策略
     * @throws IllegalArgumentException if one of the following holds:<br>
     *                                  {@code corePoolSize < 0}<br>
     *                                  {@code keepAliveTime < 0}<br>
     *                                  {@code maximumPoolSize <= 0}<br>
     *                                  {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException     if {@code workQueue}
     *                                  or {@code handler} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              RejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), handler);
    }

    /**
     * 核心构造.
     *
     * @param corePoolSize    核心线程数 unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize 最大线程数量
     * @param keepAliveTime   非核心线程超时时长.
     * @param unit            the {@code keepAliveTime} 时间单位
     * @param workQueue       任务队列.  只能 {@code Runnable}，submitted by the {@code execute} method.
     * @param threadFactory   线程工厂
     * @param handler         拒绝策略
     * @throws IllegalArgumentException if one of the following holds:<br>
     *                                  {@code corePoolSize < 0}<br>
     *                                  {@code keepAliveTime < 0}<br>
     *                                  {@code maximumPoolSize <= 0}<br>
     *                                  {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException     if {@code workQueue}
     *                                  or {@code threadFactory} or {@code handler} is null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler) {
        if (corePoolSize < 0 ||
                maximumPoolSize <= 0 ||
                maximumPoolSize < corePoolSize ||
                keepAliveTime < 0)
            throw new IllegalArgumentException();
        if (workQueue == null || threadFactory == null || handler == null)
            throw new NullPointerException();
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.threadFactory = threadFactory;
        this.handler = handler;
    }

    /**
     * 提交run，提交不了，会触发拒绝策略 {@code RejectedExecutionHandler}.
     *
     * @param command 待执行的任务{@code Runnable}
     * @throws RejectedExecutionException at discretion of
     *                                    {@code RejectedExecutionHandler}, if the task
     *                                    cannot be accepted for execution
     * @throws NullPointerException       if {@code command} is null
     */
    public void execute(@NotNull Runnable command) {
        //noinspection ConstantConditions
        if (command == null)
            throw new NullPointerException();
        int c = ctl.get();
        // 情况1、当前的Worker的数量<核心线程池大小，新建一个Worker。
        if (workerCountOf(c) < corePoolSize) {
            if (addWorker(command, true))
                return;
            c = ctl.get();
        }
        // 情况2、没调用shutdown()&& 队列没满，任务放队列里
        if (isRunning(c) && workQueue.offer(command)) {
            int recheck = ctl.get();// 再检查一次线程池的状态
            // 线程池状态改变不再是running&& 从队列中移除任务
            if (!isRunning(recheck) && remove(command))
                reject(command);// 触发拒绝策略
            else if (workerCountOf(recheck) == 0)// worker数量此时为0
                addWorker(null, false); //那么将没有Worker执行新的task，所以增加一个Worker.
        }
        // 情况3、如果任务队列满了。这时候线程数量可能还没到线程池的最大值，所以尝试增加一个Worker
        else if (!addWorker(command, false))
            reject(command);// 线程数量也满了，那么就拒绝此线程
    }

    /**
     * 终止线程池
     * 1. 检查是否能操作目标线程
     * 2. 将线程池状态转为SHUTDOWN
     * 3. 中断所有空闲线程
     * Initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down.
     *
     * <p>This method does not wait for previously submitted tasks to
     * complete execution.  Use {@link #awaitTermination awaitTermination}
     * to do that.
     */
    // android-note: Removed @throws SecurityException
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            //判断是否可以操作目标线程
            checkShutdownAccess();
            //设置线程池状态为SHUTDOWN,此处之后，线程池中不会增加新Task
            advanceRunState(SHUTDOWN);
            //中断所有的空闲线程
            interruptIdleWorkers();
            onShutdown(); // hook for ScheduledThreadPoolExecutor
        } finally {
            mainLock.unlock();
        }
        //转到Terminate
        tryTerminate();
    }

    /**
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution. These tasks are drained (removed)
     * from the task queue upon return from this method.
     *
     * <p>This method does not wait for actively executing tasks to
     * terminate.  Use {@link #awaitTermination awaitTermination} to
     * do that.
     *
     * <p>There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks.  This implementation
     * interrupts tasks via {@link Thread#interrupt}; any task that
     * fails to respond to interrupts may never terminate.
     * 首先是设置线程池状态为STOP，前面的代码我们可以看到，是对SHUTDOWN有一些额外的判断逻辑，但是对于>=STOP,基本都是reject，STOP也是比SHUTDOWN更加严格的一种状态。此时不会有新Worker加入，所有刚执行完一个线程后去GetTask的Worker都会退出。
     */
    // android-note: Removed @throws SecurityException
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(STOP);
            interruptWorkers();
            tasks = drainQueue();
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
        return tasks;
    }

    public boolean isShutdown() {
        return !isRunning(ctl.get());
    }

    /**
     * Returns true if this executor is in the process of terminating
     * after {@link #shutdown} or {@link #shutdownNow} but has not
     * completely terminated.  This method may be useful for
     * debugging. A return of {@code true} reported a sufficient
     * period after shutdown may indicate that submitted tasks have
     * ignored or suppressed interruption, causing this executor not
     * to properly terminate.
     *
     * @return {@code true} if terminating but not yet terminated
     */
    public boolean isTerminating() {
        int c = ctl.get();
        return !isRunning(c) && runStateLessThan(c, TERMINATED);
    }

    public boolean isTerminated() {
        return runStateAtLeast(ctl.get(), TERMINATED);
    }

    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            while (!runStateAtLeast(ctl.get(), TERMINATED)) {
                if (nanos <= 0L)
                    return false;
                nanos = termination.awaitNanos(nanos);
            }
            return true;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Invokes {@code shutdown} when this executor is no longer
     * referenced and it has no threads.
     */
    protected void finalize() {
        shutdown();
    }

    /**
     * Sets the thread factory used to create new threads.
     *
     * @param threadFactory the new thread factory
     * @throws NullPointerException if threadFactory is null
     * @see #getThreadFactory
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        if (threadFactory == null)
            throw new NullPointerException();
        this.threadFactory = threadFactory;
    }

    /**
     * Returns the thread factory used to create new threads.
     *
     * @return the current thread factory
     * @see #setThreadFactory(ThreadFactory)
     */
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    /**
     * Sets a new handler for unexecutable tasks.
     *
     * @param handler the new handler
     * @throws NullPointerException if handler is null
     * @see #getRejectedExecutionHandler
     */
    public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
        if (handler == null)
            throw new NullPointerException();
        this.handler = handler;
    }

    /**
     * Returns the current handler for unexecutable tasks.
     *
     * @return the current handler
     * @see #setRejectedExecutionHandler(RejectedExecutionHandler)
     */
    public RejectedExecutionHandler getRejectedExecutionHandler() {
        return handler;
    }

    // Android-changed: Tolerate maximumPoolSize >= corePoolSize during setCorePoolSize().

    /**
     * Sets the core number of threads.  This overrides any value set
     * in the constructor.  If the new value is smaller than the
     * current value, excess existing threads will be terminated when
     * they next become idle.  If larger, new threads will, if needed,
     * be started to execute any queued tasks.
     *
     * @param corePoolSize the new core size
     * @throws IllegalArgumentException if {@code corePoolSize < 0}
     * @see #getCorePoolSize
     */
    public void setCorePoolSize(int corePoolSize) {
        // BEGIN Android-changed: Tolerate maximumPoolSize >= corePoolSize during setCorePoolSize().
        // This reverts a change that threw an IAE on that condition. This is due to defective code
        // in a commonly used third party library that does something like exec.setCorePoolSize(N)
        // before doing exec.setMaxPoolSize(N).
        //
        // if (corePoolSize < 0 || maximumPoolSize < corePoolSize)
        if (corePoolSize < 0)
            // END Android-changed: Tolerate maximumPoolSize >= corePoolSize during setCorePoolSize().
            throw new IllegalArgumentException();
        int delta = corePoolSize - this.corePoolSize;
        this.corePoolSize = corePoolSize;
        if (workerCountOf(ctl.get()) > corePoolSize)
            interruptIdleWorkers();
        else if (delta > 0) {
            // We don't really know how many new threads are "needed".
            // As a heuristic, prestart enough new workers (up to new
            // core size) to handle the current number of tasks in
            // queue, but stop if queue becomes empty while doing so.
            int k = Math.min(delta, workQueue.size());
            while (k-- > 0 && addWorker(null, true)) {
                if (workQueue.isEmpty())
                    break;
            }
        }
    }

    /**
     * Returns the core number of threads.
     *
     * @return the core number of threads
     * @see #setCorePoolSize
     */
    public int getCorePoolSize() {
        return corePoolSize;
    }

    /**
     * Starts a core thread, causing it to idly wait for work. This
     * overrides the default policy of starting core threads only when
     * new tasks are executed. This method will return {@code false}
     * if all core threads have already been started.
     *
     * @return {@code true} if a thread was started
     */
    public boolean prestartCoreThread() {
        return workerCountOf(ctl.get()) < corePoolSize &&
                addWorker(null, true);
    }

    /**
     * Same as prestartCoreThread except arranges that at least one
     * thread is started even if corePoolSize is 0.
     */
    void ensurePrestart() {
        int wc = workerCountOf(ctl.get());
        if (wc < corePoolSize)
            addWorker(null, true);
        else if (wc == 0)
            addWorker(null, false);
    }

    /**
     * Starts all core threads, causing them to idly wait for work. This
     * overrides the default policy of starting core threads only when
     * new tasks are executed.
     *
     * @return the number of threads started
     */
    public int prestartAllCoreThreads() {
        int n = 0;
        while (addWorker(null, true))
            ++n;
        return n;
    }

    /**
     * Returns true if this pool allows core threads to time out and
     * terminate if no tasks arrive within the keepAlive time, being
     * replaced if needed when new tasks arrive. When true, the same
     * keep-alive policy applying to non-core threads applies also to
     * core threads. When false (the default), core threads are never
     * terminated due to lack of incoming tasks.
     *
     * @return {@code true} if core threads are allowed to time out,
     * else {@code false}
     * @since 1.6
     */
    public boolean allowsCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    /**
     * Sets the policy governing whether core threads may time out and
     * terminate if no tasks arrive within the keep-alive time, being
     * replaced if needed when new tasks arrive. When false, core
     * threads are never terminated due to lack of incoming
     * tasks. When true, the same keep-alive policy applying to
     * non-core threads applies also to core threads. To avoid
     * continual thread replacement, the keep-alive time must be
     * greater than zero when setting {@code true}. This method
     * should in general be called before the pool is actively used.
     *
     * @param value {@code true} if should time out, else {@code false}
     * @throws IllegalArgumentException if value is {@code true}
     *                                  and the current keep-alive time is not greater than zero
     * @since 1.6
     */
    public void allowCoreThreadTimeOut(boolean value) {
        if (value && keepAliveTime <= 0)
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        if (value != allowCoreThreadTimeOut) {
            allowCoreThreadTimeOut = value;
            if (value)
                interruptIdleWorkers();
        }
    }

    /**
     * Sets the maximum allowed number of threads. This overrides any
     * value set in the constructor. If the new value is smaller than
     * the current value, excess existing threads will be
     * terminated when they next become idle.
     *
     * @param maximumPoolSize the new maximum
     * @throws IllegalArgumentException if the new maximum is
     *                                  less than or equal to zero, or
     *                                  less than the {@linkplain #getCorePoolSize core pool size}
     * @see #getMaximumPoolSize
     */
    public void setMaximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize <= 0 || maximumPoolSize < corePoolSize)
            throw new IllegalArgumentException();
        this.maximumPoolSize = maximumPoolSize;
        if (workerCountOf(ctl.get()) > maximumPoolSize)
            interruptIdleWorkers();
    }

    /**
     * Returns the maximum allowed number of threads.
     *
     * @return the maximum allowed number of threads
     * @see #setMaximumPoolSize
     */
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    /**
     * Sets the thread keep-alive time, which is the amount of time
     * that threads may remain idle before being terminated.
     * Threads that wait this amount of time without processing a
     * task will be terminated if there are more than the core
     * number of threads currently in the pool, or if this pool
     * {@linkplain #allowsCoreThreadTimeOut() allows core thread timeout}.
     * This overrides any value set in the constructor.
     *
     * @param time the time to wait.  A time value of zero will cause
     *             excess threads to terminate immediately after executing tasks.
     * @param unit the time unit of the {@code time} argument
     * @throws IllegalArgumentException if {@code time} less than zero or
     *                                  if {@code time} is zero and {@code allowsCoreThreadTimeOut}
     * @see #getKeepAliveTime(TimeUnit)
     */
    public void setKeepAliveTime(long time, TimeUnit unit) {
        if (time < 0)
            throw new IllegalArgumentException();
        if (time == 0 && allowsCoreThreadTimeOut())
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        long keepAliveTime = unit.toNanos(time);
        long delta = keepAliveTime - this.keepAliveTime;
        this.keepAliveTime = keepAliveTime;
        if (delta < 0)
            interruptIdleWorkers();
    }

    /**
     * Returns the thread keep-alive time, which is the amount of time
     * that threads may remain idle before being terminated.
     * Threads that wait this amount of time without processing a
     * task will be terminated if there are more than the core
     * number of threads currently in the pool, or if this pool
     * {@linkplain #allowsCoreThreadTimeOut() allows core thread timeout}.
     *
     * @param unit the desired time unit of the result
     * @return the time limit
     * @see #setKeepAliveTime(long, TimeUnit)
     */
    public long getKeepAliveTime(TimeUnit unit) {
        return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS);
    }

    /* User-level queue utilities */

    /**
     * Returns the task queue used by this executor. Access to the
     * task queue is intended primarily for debugging and monitoring.
     * This queue may be in active use.  Retrieving the task queue
     * does not prevent queued tasks from executing.
     *
     * @return the task queue
     */
    public BlockingQueue<Runnable> getQueue() {
        return workQueue;
    }

    /**
     * Removes this task from the executor's internal queue if it is
     * present, thus causing it not to be run if it has not already
     * started.
     *
     * <p>This method may be useful as one part of a cancellation
     * scheme.  It may fail to remove tasks that have been converted
     * into other forms before being placed on the internal queue.
     * For example, a task entered using {@code submit} might be
     * converted into a form that maintains {@code Future} status.
     * However, in such cases, method {@link #purge} may be used to
     * remove those Futures that have been cancelled.
     *
     * @param task the task to remove
     * @return {@code true} if the task was removed
     */
    public boolean remove(Runnable task) {
        boolean removed = workQueue.remove(task);
        tryTerminate(); // In case SHUTDOWN and now empty
        return removed;
    }

    /**
     * Tries to remove from the work queue all {@link Future}
     * tasks that have been cancelled. This method can be useful as a
     * storage reclamation operation, that has no other impact on
     * functionality. Cancelled tasks are never executed, but may
     * accumulate in work queues until worker threads can actively
     * remove them. Invoking this method instead tries to remove them now.
     * However, this method may fail to remove tasks in
     * the presence of interference by other threads.
     */
    public void purge() {
        final BlockingQueue<Runnable> q = workQueue;
        try {
            Iterator<Runnable> it = q.iterator();
            while (it.hasNext()) {
                Runnable r = it.next();
                if (r instanceof Future<?> && ((Future<?>) r).isCancelled())
                    it.remove();
            }
        } catch (ConcurrentModificationException fallThrough) {
            // Take slow path if we encounter interference during traversal.
            // Make copy for traversal and call remove for cancelled entries.
            // The slow path is more likely to be O(N*N).
            for (Object r : q.toArray())
                if (r instanceof Future<?> && ((Future<?>) r).isCancelled())
                    q.remove(r);
        }

        tryTerminate(); // In case SHUTDOWN and now empty
    }

    /* Statistics */

    /**
     * Returns the current number of threads in the pool.
     *
     * @return the number of threads
     */
    public int getPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // Remove rare and surprising possibility of
            // isTerminated() && getPoolSize() > 0
            return runStateAtLeast(ctl.get(), TIDYING) ? 0
                    : workers.size();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the approximate number of threads that are actively
     * executing tasks.
     *
     * @return the number of threads
     */
    public int getActiveCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            int n = 0;
            for (Worker w : workers)
                if (w.isLocked())
                    ++n;
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the largest number of threads that have ever
     * simultaneously been in the pool.
     *
     * @return the number of threads
     */
    public int getLargestPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            return largestPoolSize;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the approximate total number of tasks that have ever been
     * scheduled for execution. Because the states of tasks and
     * threads may change dynamically during computation, the returned
     * value is only an approximation.
     *
     * @return the number of tasks
     */
    public long getTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers) {
                n += w.completedTasks;
                if (w.isLocked())
                    ++n;
            }
            return n + workQueue.size();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the approximate total number of tasks that have
     * completed execution. Because the states of tasks and threads
     * may change dynamically during computation, the returned value
     * is only an approximation, but one that does not ever decrease
     * across successive calls.
     *
     * @return the number of tasks
     */
    public long getCompletedTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers)
                n += w.completedTasks;
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns a string identifying this pool, as well as its state,
     * including indications of run state and estimated worker and
     * task counts.
     *
     * @return a string identifying this pool, as well as its state
     */
    public String toString() {
        long ncompleted;
        int nworkers, nactive;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            ncompleted = completedTaskCount;
            nactive = 0;
            nworkers = workers.size();
            for (Worker w : workers) {
                ncompleted += w.completedTasks;
                if (w.isLocked())
                    ++nactive;
            }
        } finally {
            mainLock.unlock();
        }
        int c = ctl.get();
        String runState =
                runStateLessThan(c, SHUTDOWN) ? "Running" :
                        runStateAtLeast(c, TERMINATED) ? "Terminated" :
                                "Shutting down";
        return super.toString() +
                "[" + runState +
                ", pool size = " + nworkers +
                ", active threads = " + nactive +
                ", queued tasks = " + workQueue.size() +
                ", completed tasks = " + ncompleted +
                "]";
    }

    /* Extension hooks */

    /**
     * Method invoked prior to executing the given Runnable in the
     * given thread.  This method is invoked by thread {@code t} that
     * will execute task {@code r}, and may be used to re-initialize
     * ThreadLocals, or to perform logging.
     *
     * <p>This implementation does nothing, but may be customized in
     * subclasses. Note: To properly nest multiple overridings, subclasses
     * should generally invoke {@code super.beforeExecute} at the end of
     * this method.
     *
     * @param t the thread that will run task {@code r}
     * @param r the task that will be executed
     */
    protected void beforeExecute(Thread t, Runnable r) {
    }

    /**
     * Method invoked upon completion of execution of the given Runnable.
     * This method is invoked by the thread that executed the task. If
     * non-null, the Throwable is the uncaught {@code RuntimeException}
     * or {@code Error} that caused execution to terminate abruptly.
     *
     * <p>This implementation does nothing, but may be customized in
     * subclasses. Note: To properly nest multiple overridings, subclasses
     * should generally invoke {@code super.afterExecute} at the
     * beginning of this method.
     *
     * <p><b>Note:</b> When actions are enclosed in tasks (such as
     * {@link java.util.concurrent.FutureTask}) either explicitly or via methods such as
     * {@code submit}, these task objects catch and maintain
     * computational exceptions, and so they do not cause abrupt
     * termination, and the internal exceptions are <em>not</em>
     * passed to this method. If you would like to trap both kinds of
     * failures in this method, you can further probe for such cases,
     * as in this sample subclass that prints either the direct cause
     * or the underlying exception if a task has been aborted:
     *
     * <pre> {@code
     * class ExtendedExecutor extends ThreadPoolExecutor {
     *   // ...
     *   protected void afterExecute(Runnable r, Throwable t) {
     *     super.afterExecute(r, t);
     *     if (t == null
     *         && r instanceof Future<?>
     *         && ((Future<?>)r).isDone()) {
     *       try {
     *         Object result = ((Future<?>) r).get();
     *       } catch (CancellationException ce) {
     *         t = ce;
     *       } catch (ExecutionException ee) {
     *         t = ee.getCause();
     *       } catch (InterruptedException ie) {
     *         // ignore/reset
     *         Thread.currentThread().interrupt();
     *       }
     *     }
     *     if (t != null)
     *       System.out.println(t);
     *   }
     * }}</pre>
     *
     * @param r the runnable that has completed
     * @param t the exception that caused termination, or null if
     *          execution completed normally
     */
    protected void afterExecute(Runnable r, Throwable t) {
    }

    /**
     * Method invoked when the Executor has terminated.  Default
     * implementation does nothing. Note: To properly nest multiple
     * overridings, subclasses should generally invoke
     * {@code super.terminated} within this method.
     */
    protected void terminated() {
    }

    /* Predefined RejectedExecutionHandlers */

    /**
     * A handler for rejected tasks that runs the rejected task
     * directly in the calling thread of the {@code execute} method,
     * unless the executor has been shut down, in which case the task
     * is discarded.
     */
    public static class CallerRunsPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code CallerRunsPolicy}.
         */
        public CallerRunsPolicy() {
        }

        /**
         * Executes task r in the caller's thread, unless the executor
         * has been shut down, in which case the task is discarded.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                r.run();
            }
        }
    }

    /**
     * A handler for rejected tasks that throws a
     * {@code RejectedExecutionException}.
     */
    public static class AbortPolicy implements RejectedExecutionHandler {
        /**
         * Creates an {@code AbortPolicy}.
         */
        public AbortPolicy() {
        }

        /**
         * Always throws RejectedExecutionException.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         * @throws RejectedExecutionException always
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            throw new RejectedExecutionException("Task " + r.toString() +
                    " rejected from " +
                    e.toString());
        }
    }

    /**
     * A handler for rejected tasks that silently discards the
     * rejected task.
     */
    public static class DiscardPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code DiscardPolicy}.
         */
        public DiscardPolicy() {
        }

        /**
         * Does nothing, which has the effect of discarding task r.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        }
    }

    /**
     * A handler for rejected tasks that discards the oldest unhandled
     * request and then retries {@code execute}, unless the executor
     * is shut down, in which case the task is discarded.
     */
    public static class DiscardOldestPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code DiscardOldestPolicy} for the given executor.
         */
        public DiscardOldestPolicy() {
        }

        /**
         * Obtains and ignores the next task that the executor
         * would otherwise execute, if one is immediately available,
         * and then retries execution of task r, unless the executor
         * is shut down, in which case task r is instead discarded.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                e.getQueue().poll();
                e.execute(r);
            }
        }
    }
}
