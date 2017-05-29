package ru.shishmakov.core;

import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.config.CrawlerConfig;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ru.shishmakov.util.Threads.STOP_TIMEOUT_SEC;
import static ru.shishmakov.util.Threads.sleepInterrupted;

/**
 * @author Dmitriy Shishmakov on 10.05.17
 */
public class RateAccessController {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int DEFAULT_RING_CAPACITY = 60;
    private static final int BLOCK_OFFSET = DEFAULT_RING_CAPACITY / 6;
    private static final String NAME = RateAccessController.class.getSimpleName();

    @Inject
    private CrawlerConfig crawlerConfig;

    private final AtomicBoolean accessState = new AtomicBoolean(true);
    private final CountDownLatch awaitStop = new CountDownLatch(1);
    private ExecutorService executor;
    private Semaphore[] ring;

    @PostConstruct
    public void setUp() {
        logger.info("{} starting ...", NAME);
        this.executor = buildExecutorService();
        this.ring = buildRingSemaphores(crawlerConfig.requestPerSecond());
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
                logger.info("{} stopped", Thread.currentThread().getName());
            }
        });
        logger.info("{} started", NAME);
    }

    @PreDestroy
    public void tearDown() {
        logger.info("{} stopping ...", NAME);
        try {
            shutdownWebAccessor();
            MoreExecutors.shutdownAndAwaitTermination(executor, STOP_TIMEOUT_SEC, SECONDS);
            awaitStop.await(2, SECONDS);
            logger.info("{} stopped", NAME);
        } catch (Exception e) {
            logger.error("{} error in time of stopping", NAME, e);
        }
    }

    public <T> T acquireAccess(Callable<T> task) throws Exception {
        while (!acquire()) {
            // wait acquire
            logger.trace("Thread: {} is waiting to acquire the monitor", Thread.currentThread().getName());
        }
        logger.debug("Thread: {} run task on {} sec",
                Thread.currentThread().getName(), System.currentTimeMillis() / 1000);
        return task.call();
    }

    private void release() {
        final int currentBlock = defineCurrentBlock();
        final int left = (currentBlock - BLOCK_OFFSET + ring.length) % ring.length;
        final int right = (currentBlock + BLOCK_OFFSET + 1) % ring.length;
        logger.debug("Thread: {} release blocks [{}] -> [{}] on {} sec",
                Thread.currentThread().getName(), right, left, System.currentTimeMillis() / 1000);
        // right -> left
        for (int block = right; block != left; block = (block + 1) % ring.length/*nextBlock*/) {
            Semaphore semaphore = ring[block];
            semaphore.drainPermits();
            semaphore.release(crawlerConfig.requestPerSecond());
        }
    }

    private boolean acquire() {
        int block = defineCurrentBlock();
        try {
            if (ring[block].tryAcquire(defineWaitTimeout(), MILLISECONDS)) {
                logger.debug("Thread: {} acquire block [{}] on {} sec",
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
        return 1000 / crawlerConfig.requestPerSecond();
    }

    private void shutdownWebAccessor() {
        if (accessState.compareAndSet(true, false)) {
            logger.debug("{} waiting for shutdown process to complete...", NAME);
        }
    }

    private static Semaphore[] buildRingSemaphores(int requestPerSecond) {
        Semaphore[] semaphores = new Semaphore[DEFAULT_RING_CAPACITY];
        for (int i = 0; i < semaphores.length; i++) {
            semaphores[i] = new Semaphore(requestPerSecond);
        }
        return semaphores;
    }

    private static ExecutorService buildExecutorService() {
        return Executors.newSingleThreadExecutor(new BasicThreadFactory.Builder()
                .namingPattern("access-worker %d")
                .build());
    }
}
