package com.malong.moses;

import org.junit.Test;

import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.assertEquals;

/**
 * 显式锁
 * Lock接口中定义了一组抽象的加锁操作，ReentrantLock是实现类
 */
public class ReentrantLockTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }
}
