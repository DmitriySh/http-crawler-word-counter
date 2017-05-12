package ru.shishmakov.core;

import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.shishmakov.BaseTest;
import ru.shishmakov.config.Config;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

/**
 * @author Dmitriy Shishmakov on 11.05.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Config.class)
public class RateAccessControllerTest extends BaseTest {

    private ExecutorService pool;

    @Inject
    private RateAccessController controller;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        pool = buildExecutorService();
    }

    @After
    public void tearDown() {
        MoreExecutors.shutdownAndAwaitTermination(pool, 30, SECONDS);
    }

    @Test
    public void acquireAccessShouldHaveThresholdTaskCountPerSecond() throws Exception {
        final int ratePerSecond = 20;
        final int taskCount = 100;
        Map<Long, Integer> statistics = new ConcurrentHashMap<>();
        CountDownLatch awaitTasks = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            pool.submit(() -> controller.acquireAccess(buildTask(statistics, awaitTasks)));
        }
        awaitTasks.await();
        logger.info("Result map: {}", statistics);

        statistics.forEach((second, tasks) -> assertEquals("Excess rate per second", ratePerSecond, tasks.intValue()));
    }

    private Runnable buildTask(Map<Long, Integer> statistics, CountDownLatch awaitTasks) {
        return () -> {
            long currentSec = System.currentTimeMillis() / 1000;
            statistics.merge(currentSec, 1, (old, inc) -> old + inc);
            awaitTasks.countDown();
        };
    }

    private static ExecutorService buildExecutorService() {
        return Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
                new BasicThreadFactory.Builder()
                        .namingPattern("request-worker %d")
                        .build());
    }

}
