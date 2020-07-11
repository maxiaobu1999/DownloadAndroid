package com.malong.download.utils;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class FileUtils {
    public static final String TAG = "【FileUtils】";
    /** invalid index */
    public static int INVALID_INDEX = -1;
    /** increament one step */
    public static int ONE_INCREAMENT = 1;

    /**
     * 从文件路径中获取文件名(包括文件后缀)
     *
     * @param path 文件路径
     * @return
     */
    public static String getFileNameFromPath(String path) {
        if (TextUtils.isEmpty(path) || path.endsWith(File.separator)) {
            return "";
        }
        int start = path.lastIndexOf(File.separator);
        int end = path.length();
        if (start != INVALID_INDEX && end > start) {
            return path.substring(start + ONE_INCREAMENT, end);
        } else {
            return path;
        }
    }

    /**
     * 从输入流中读取字节写入输出流
     *
     * @param is 输入流
     * @param os 输出流
     * @return 复制大字节数
     */
    public static long copyStream(InputStream is, OutputStream os) {
        if (null == is || null == os) {
            return 0;
        }

        try {
            final int defaultBufferSize = 1024 * 3;
            byte[] buf = new byte[defaultBufferSize];
            long size = 0;
            int len;
            while ((len = is.read(buf)) > 0) {
                os.write(buf, 0, len);
                size += len;
            }
            os.flush();
            return size;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 从输入流中读取字节写入输出流
     *
     * @param is    输入流
     * @param raf   输出流
     * @param index 写入的指针
     * @return 写入的字节数
     */
    public static long copyStream(InputStream is, RandomAccessFile raf, long index) {
        if (null == is || null == raf) {
            return 0;
        }
        try {
            raf.seek(index);
            final int defaultBufferSize = 1024 * 3;
            byte[] buf = new byte[defaultBufferSize];
            long size = 0;
            int len;
            while ((len = is.read(buf)) > 0) {
                raf.write(buf, 0, len);
                size += len;
            }
            return size;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 判断文件是否存在
     *
     * @param fileName 文件全路径
     * @return if true 存在
     */
    public static boolean isExistFile(String fileName) {
        return !TextUtils.isEmpty(fileName) && new File(fileName).exists();
    }

    /**
     * 删除指定文件、文件夹内容
     *
     * @param file 文件或文件夹
     * @return 是否成功删除
     */
    public static boolean deleteFile(File file) {
        if (file == null) {
            return false;
        }
        boolean isDeletedAll = true;

        if (file.exists()) {
            // 判断是否是文件,直接删除文件
            if (file.isFile()) {
                isDeletedAll &= file.delete();

                // 遍历删除一个文件目录
            } else if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        isDeletedAll &= deleteFile(files[i]); // 迭代删除文件夹内容
                    }
                }

                isDeletedAll &= file.delete();

            } else {
                Log.d(TAG, "a special file:" + file);
            }
        } else {
            Log.d(TAG, "not found the file to delete:" + file);
        }

        return isDeletedAll;
    }
}
