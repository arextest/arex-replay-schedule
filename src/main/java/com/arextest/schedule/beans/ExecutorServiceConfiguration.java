package com.arextest.schedule.beans;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * @author jmo
 * @since 2021/9/16
 */
@Configuration
@Slf4j
class ExecutorServiceConfiguration implements Thread.UncaughtExceptionHandler {
    private static final long KEEP_ALIVE_TIME = 60L;
    private static final int CORE_POOL_SIZE = 2 * Runtime.getRuntime().availableProcessors();
    private static final int MAXIMUM_POOL_SIZE = 2 * CORE_POOL_SIZE;
    private static final int SEND_QUEUE_MAX_CAPACITY_SIZE = 2000;
    private static final int PRELOAD_QUEUE_MAX_CAPACITY_SIZE = 100;
    private static final int ACTION_ITEM_QUEUE_MAX_CAPACITY_SIZE = 200;

    private static final int SEND_POOL_SIZE = ExecutorServiceConfiguration.calculateIOPoolSize();

    @Bean
    public ExecutorService preloadExecutorService() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("replay-preload-%d")
                .setDaemon(true)
                .setUncaughtExceptionHandler(this)
                .build();
        return new ThreadPoolExecutor(CORE_POOL_SIZE,
                MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(PRELOAD_QUEUE_MAX_CAPACITY_SIZE), threadFactory);
    }

    /**
     * This bean should be overridden according to the actual implementation of compare service
     */
    @Bean
    public ExecutorService sendExecutorService() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("replay-send-%d")
                .setDaemon(true)
                .setUncaughtExceptionHandler(this)
                .build();
        return new ThreadPoolExecutor(SEND_POOL_SIZE, SEND_POOL_SIZE, KEEP_ALIVE_TIME,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(SEND_QUEUE_MAX_CAPACITY_SIZE), threadFactory);
    }

    @Bean
    public ExecutorService actionItemParallelPool() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("replay-action-parallel-%d")
                .setDaemon(true)
                .setUncaughtExceptionHandler(this)
                .build();
        return new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(ACTION_ITEM_QUEUE_MAX_CAPACITY_SIZE),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        LOGGER.error("uncaughtException {} ,error :{}", t.getName(), e.getMessage(), e);
    }

    private static int calculateIOPoolSize() {
        int nThreads = Runtime.getRuntime().availableProcessors();
        double targetCPUUtilization = 0.8;
        double wC = 3.0; // assume wait time is 5 times compute time
        int optimalThreadPoolSize = (int) Math.ceil(nThreads * targetCPUUtilization * (1 + wC));
        return optimalThreadPoolSize;
    }
}