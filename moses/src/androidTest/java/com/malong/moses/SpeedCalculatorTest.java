package com.malong.moses;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.malong.moses.utils.SpeedCalculator;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SpeedCalculatorTest {
    @Test
    public void testHumanReadableBytes() {
        String s = SpeedCalculator
                .humanReadableBytes(1000 * 1000 * 1000, true);
        Assert.assertEquals(s,"1.0 GB");//千进制：
        String s1 = SpeedCalculator
                .humanReadableBytes(1000 * 1000 * 1000, false);
        Assert.assertEquals(s1,"953.7 MiB");// 1024进制
    }

    @Test
    public void testHumanReadableBytes1() {
        SpeedCalculator calculator = new SpeedCalculator();
        for (int i = 0; i < 100; i++) {
            calculator.downloading(i);
            String speed = calculator.speed();
            Log.d("SpeedCalculatorTest", speed);
            long bytesPerSecondFromBegin = calculator.getBytesPerSecondFromBegin();
            Log.d("SpeedCalculatorTest", "bytesPerSecondFromBegin:" + bytesPerSecondFromBegin);

        }
    }


}
