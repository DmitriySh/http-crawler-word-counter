package ru.shishmakov.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Dmitriy Shishmakov on 10.05.17
 */
public class WebAccessController {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int DEFAULT_CAPACITY = 60;

    private final Semaphore[] blocks;
    private final int ratePerSecond;

    public WebAccessController(int ratePerSecond) {
        this.ratePerSecond = ratePerSecond;
        this.blocks = IntStream.range(0, DEFAULT_CAPACITY)
                .boxed()
                .map(i -> new Semaphore(ratePerSecond))
                .toArray(Semaphore[]::new);
    }

    public void acquireAccess() {
        while (!acquire()) {

        }
    }

    private boolean acquire() {
        try {
            return tryAcquire(defineBlock());
        } catch (InterruptedException ex) {
            return false;
        }
    }

    private int defineBlock() {
        return Math.toIntExact((System.currentTimeMillis() / 1000) % blocks.length);
    }

    private boolean tryAcquire(int block) throws InterruptedException {
        Semaphore semaphore = blocks[block];
//        long ms = 20;
        long timeout = delay();
        return semaphore.tryAcquire(timeout, MILLISECONDS);
    }

    private long delay() {
        return 0;
    }
}
