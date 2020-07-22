package com.malong.moses.utils;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.malong.moses.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileUtils {
    public static final String TAG = "【FileUtils】";
    /** File buffer stream size */
    public static final int FILE_STREAM_BUFFER_SIZE = 8192;
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
    public static boolean checkFileExist(String fileName) {
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
            Log.d(TAG, "not found the file to deleteDownload:" + file);
        }

        return isDeletedAll;
    }
    /**
     * 删除指定文件
     *
     * @param path 文件路径
     * @return 是否成功删除
     */
    public static boolean deleteFile(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        File file = new File(path);
        if (file.exists()) {
            return deleteFile(file);
        }
        return false;
    }

    /**
     * 从url从抽取文件名
     *
     * @param url String
     * @return /xxxx.mp4?yyy, 返回xxxx.mp4
     */
    @Nullable
    public static String getFileNameFromUrl(String url) {
        String filename = null;
        String decodedUrl = Uri.decode(url);
        if (decodedUrl != null) {
            int queryIndex = decodedUrl.indexOf('?');
            // If there is a query string strip it, same as desktop browsers
            if (queryIndex > 0) {
                decodedUrl = decodedUrl.substring(0, queryIndex);
            }
            if (!decodedUrl.endsWith("/")) {
                int index = decodedUrl.lastIndexOf('/') + 1;
                if (index > 0) {
                    filename = decodedUrl.substring(index);
                }
            }
        }

        return filename;
    }

    public static boolean checkFileExist(String dir, String filename) {
        return !TextUtils.isEmpty(dir + filename) && new File(dir + filename).exists();
    }

    /**
     * 计算文件Md5 32位 十六进制字符串，单个字节小于0xf，高位补0
     * ETag : "66cbdb8598353b2cd579e408cf42d52f"
     * @param file 文件
     * @param upperCase true：大写， false 小写字符串
     * @return Md5 32位 十六进制字符串，单个字节小于0xf，高位补0
     */
    public static String toMd5(File file, boolean upperCase) {
        InputStream is = null;
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            is = new FileInputStream(file);
            byte[] buffer = new byte[FILE_STREAM_BUFFER_SIZE];
            int read = 0;
            while ((read = is.read(buffer)) > 0) {
                algorithm.update(buffer, 0, read);
            }
            return toHexString(algorithm.digest(), "", upperCase);
        } catch (NoSuchAlgorithmException e) {
            if (Constants.DEBUG) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            if (Constants.DEBUG) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            if (Constants.DEBUG) {
                e.printStackTrace();
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    if (Constants.DEBUG) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return null;
    }
    /**
     * 把二进制byte数组生成十六进制字符串，单个字节小于0xf，高位补0。
     *
     * @param bytes 输入
     * @param separator 分割线
     * @param upperCase true：大写， false 小写字符串
     * @return 把二进制byte数组生成十六进制字符串，单个字节小于0xf，高位补0。
     */
    @SuppressWarnings("SameParameterValue")
    private static String toHexString(byte[] bytes, String separator, boolean upperCase) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String str = Integer.toHexString(0xFF & b); // SUPPRESS CHECKSTYLE
            if (upperCase) {
                str = str.toUpperCase();
            }
            if (str.length() == 1) {
                hexString.append("0");
            }
            hexString.append(str).append(separator);
        }
        return hexString.toString();
    }
}
