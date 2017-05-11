package ru.shishmakov.concurrent;

import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ru.shishmakov.concurrent.Threads.STOP_TIMEOUT_SEC;
import static ru.shishmakov.concurrent.Threads.sleepInterrupted;

/**
 * @author Dmitriy Shishmakov on 10.05.17
 */
public class WebAccessController {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int DEFAULT_CAPACITY = 60;
    private static final int BLOCK_OFFSET = DEFAULT_CAPACITY / 6;

    private final String NAME = this.getClass().getSimpleName();
    private final AtomicBoolean accessState = new AtomicBoolean(true);
    private final CountDownLatch awaitStop = new CountDownLatch(1);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Semaphore[] ring;
    private final int ratePerSecond;

    public WebAccessController(int ratePerSecond) {
        this.ratePerSecond = ratePerSecond;
        this.ring = IntStream.range(0, DEFAULT_CAPACITY)
                .boxed()
                .map(i -> new Semaphore(ratePerSecond))
                .toArray(Semaphore[]::new);
    }

    public void start() {
        executor.execute(() -> {
            logger.info("{} started", Thread.currentThread().getName());
            try {
                while (accessState.get() && !Thread.currentThread().isInterrupted()) {
                    release();
                    sleepInterrupted(5, SECONDS);
                }
            } catch (Exception e) {
                logger.error("{} error in time of processing", NAME, e);
            } finally {
                shutdownWebAccessor();
                awaitStop.countDown();
            }
        });
    }

    public void stop() {
        logger.info("{} stopping...", NAME);
        try {
            shutdownWebAccessor();
            MoreExecutors.shutdownAndAwaitTermination(executor, STOP_TIMEOUT_SEC, SECONDS);
            awaitStop.await(2, SECONDS);
            logger.info("{} stopped", NAME);
        } catch (Exception e) {
            logger.error("{} error in time of stopping", NAME, e);
        }
    }

    protected void acquireAccess(Runnable task) {
        while (!acquire()) {
            // nothing to do
        }
        logger.debug("thread: {} run task on {} sec",
                Thread.currentThread().getName(), System.currentTimeMillis() / 1000);
        task.run();
    }

    private void release() {
        final int currentBlock = defineCurrentBlock();
        final int left = (currentBlock - BLOCK_OFFSET + ring.length) % ring.length;
        final int right = (currentBlock + BLOCK_OFFSET + 1) % ring.length;
        logger.debug("thread: {} release blocks [{}] -> [{}] on {} sec",
                Thread.currentThread().getName(), right, left, System.currentTimeMillis() / 1000);
        // right -> left
        for (int block = right; block != left; block = (block + 1) % ring.length/*nextBlock*/) {
            Semaphore semaphore = ring[block];
            semaphore.drainPermits();
            semaphore.release(ratePerSecond);
        }
    }

    private boolean acquire() {
        int block = defineCurrentBlock();
        try {
            if (ring[block].tryAcquire(defineWaitTimeout(), MILLISECONDS)) {
                logger.debug("thread: {} acquire block [{}] on {} sec",
                        Thread.currentThread().getName(), block, System.currentTimeMillis() / 1000);
                return true;
            }
            return false;
        } catch (InterruptedException ex) {
            return false;
        }
    }

    private int defineCurrentBlock() {
        return Math.toIntExact((System.currentTimeMillis() / 1000) % ring.length);
    }

    private long defineWaitTimeout() {
        return 1000 / ratePerSecond;
    }


    private void shutdownWebAccessor() {
        if (accessState.compareAndSet(true, false)) {
            logger.debug("{} waiting for shutdown process to complete...", NAME);
        }
    }
}
