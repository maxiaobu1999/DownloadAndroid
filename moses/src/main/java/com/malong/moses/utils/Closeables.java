package com.malong.moses.utils;

import android.database.Cursor;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;

/**
 * 用于关闭流对象。
 */
@SuppressWarnings("unused")
public final class Closeables {

    /**
     * Log TAG
     */
    private static final String TAG = "【Closeables】";

    /**
     * 私有构造函数。
     */
    private Closeables() {
    }

    /**
     * 安全关闭流对象。
     *
     * @param closeable the {@code Closeable} object to be closed, or null, in which case this method does nothing.
     */
    public static void closeSafely(@Nullable Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes a {@link Closeable}, with control over whether an {@code IOException} may be thrown.
     * This is primarily useful in a finally block, where a thrown exception needs to be logged but
     * not propagated (otherwise the original exception will be lost).
     *
     * <p>If {@code swallowIOException} is true then we never throw {@code IOException} but merely log
     * it.
     *
     * <p>Example: <pre>   {@code
     *
     *   public void useStreamNicely() throws IOException {
     *     SomeStream stream = new SomeStream("foo");
     *     boolean threw = true;
     *     try {
     *       // ... code which does something with the stream ...
     *       threw = false;
     *     } finally {
     *       // If an exception occurs, rethrow it only if threw==false:
     *       Closeables.close(stream, threw);
     *     }
     *   }}</pre>
     *
     * @param closeable          the {@code Closeable} object to be closed, or null, in which case this method
     *                           does nothing
     * @param swallowIOException if true, don't propagate IO exceptions thrown by the {@code close}
     *                           methods
     * @throws IOException if {@code swallowIOException} is false and {@code close} throws an
     *                     {@code IOException}.
     */
    public static void close(@Nullable Closeable closeable,
                             boolean swallowIOException) throws IOException {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            if (swallowIOException) {
                Log.d(TAG, "IOException thrown while closing Closeable.", e);
            } else {
                throw e;
            }
        }
    }

    /**
     * 安全关闭游标。
     *
     * @param cursor Cursor
     */
    public static void closeSafely(Cursor cursor) {
        try {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
