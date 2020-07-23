/*
 * Copyright (c) 2017 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.malong.moses.utils;

import android.os.SystemClock;

import java.util.Locale;

/** 计算下载速度 */
public class SpeedCalculator {

    /** 时间戳 */
    long timestamp;
    /** 增长的字节数，每周期会清除 */
    long increaseBytes;

    /** 每秒增长的字节数 */
    long bytesPerSecond;

    /** 下载开始时的时间戳 */
    long beginTimestamp;
    long endTimestamp;
    /** 增长的字节数，自下载开始 */
    long allIncreaseBytes;

    public synchronized void reset() {
        timestamp = 0;
        increaseBytes = 0;
        bytesPerSecond = 0;

        beginTimestamp = 0;
        endTimestamp = 0;
        allIncreaseBytes = 0;
    }

    /** 当前时间戳 */
    long nowMillis() {
        return SystemClock.uptimeMillis();// 开机时间
    }

    /**
     * 添加增长字节数
     * @param increaseBytes 增长量
     */
    public synchronized void downloading(long increaseBytes) {
        if (timestamp == 0) {
            this.timestamp = nowMillis();
            this.beginTimestamp = timestamp;
        }

        this.increaseBytes += increaseBytes;
        this.allIncreaseBytes += increaseBytes;
    }

    /** 清除上一秒的记录 */
    public synchronized void flush() {
        final long nowMillis = nowMillis();
        final long sinceNowIncreaseBytes = increaseBytes;
        final long durationMillis = Math.max(1, nowMillis - timestamp);

        increaseBytes = 0;
        timestamp = nowMillis;

        // precision loss
        bytesPerSecond = (long) ((float) sinceNowIncreaseBytes / durationMillis * 1000f);
    }

    /** 获得每秒即时字节数*/
    public long getInstantBytesPerSecondAndFlush() {
        flush();
        return bytesPerSecond;
    }

    /** 获取上一秒的数据（字节），只有间隔大于1秒才会重新计算*/
    public synchronized long getBytesPerSecondAndFlush() {
        final long interval = nowMillis() - timestamp;
        // 间隔小于一秒，不计算
        if (interval < 1000 && bytesPerSecond != 0) return bytesPerSecond;

        // the first time we using 500 milliseconds to let speed valid more quick
        if (bytesPerSecond == 0 && interval < 500) return 0;

        return getInstantBytesPerSecondAndFlush();
    }

    public synchronized long getBytesPerSecondFromBegin() {
        final long endTimestamp = this.endTimestamp == 0 ? nowMillis() : this.endTimestamp;
        final long sinceNowIncreaseBytes = allIncreaseBytes;
        final long durationMillis = Math.max(1, endTimestamp - beginTimestamp);

        // precision loss
        return (long) ((float) sinceNowIncreaseBytes / durationMillis * 1000f);
    }

    public synchronized void endTask() {
        endTimestamp = nowMillis();
    }

    /**
     * Get instant speed
     */
    public String instantSpeed() {
        return getSpeedWithSIAndFlush();
    }

    /**
     * Get speed with at least one second duration.
     */
    public String speed() {
        return humanReadableSpeed(getBytesPerSecondAndFlush(), true);
    }

    /**
     * Get last time calculated speed.
     */
    public String lastSpeed() {
        return humanReadableSpeed(bytesPerSecond, true);
    }

    public synchronized long getInstantSpeedDurationMillis() {
        return nowMillis() - timestamp;
    }


    /**
     * With wikipedia: https://en.wikipedia.org/wiki/Kibibyte
     * <p>
     * 1KiB = 2^10B = 1024B
     * 1MiB = 2^10KB = 1024KB
     */
    public String getSpeedWithBinaryAndFlush() {
        return humanReadableSpeed(getInstantBytesPerSecondAndFlush(), false);
    }

    /**
     * With wikipedia: https://en.wikipedia.org/wiki/Kilobyte
     * <p>
     * 1KB = 1000B
     * 1MB = 1000KB
     */
    public String getSpeedWithSIAndFlush() {
        return humanReadableSpeed(getInstantBytesPerSecondAndFlush(), true);
    }

    public String averageSpeed() {
        return speedFromBegin();
    }

    public String speedFromBegin() {
        return humanReadableSpeed(getBytesPerSecondFromBegin(), true);
    }

    /**
     * 速度没秒
     * 字节转成其他单位KB、MB、G。eg：3MB/s
     * @param si true 千进位 ；false 1024进位.
     */
    private static String humanReadableSpeed(long bytes, boolean si) {
        return humanReadableBytes(bytes, si) + "/s";
    }
    /**
     * 字节转成其他单位KB、MB、G
     * @param si true 千进位 ；false 1024进位.
     */
    public static String humanReadableBytes(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format(Locale.ENGLISH, "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
