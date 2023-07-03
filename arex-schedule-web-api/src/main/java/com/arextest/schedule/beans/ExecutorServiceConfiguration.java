package com.arextest.schedule.beans;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
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
    private static final int SEND_QUEUE_MAX_CAPACITY_SIZE = 4000;
    private static final int COMPARE_QUEUE_MAX_CAPACITY_SIZE = 2000;
    private static final int PRELOAD_QUEUE_MAX_CAPACITY_SIZE = 100;

    @Value("${arex.schedule.pool.io.cpuratio}")
    private int cpuRatio;

    @Bean
    public ExecutorService preloadExecutorService() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("replay-preload-%d")
                .setDaemon(true)
                .setUncaughtExceptionHandler(this)
                .build();
        return new ThreadPoolExecutor(CORE_POOL_SIZE,
                MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(PRELOAD_QUEUE_MAX_CAPACITY_SIZE),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy());
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
        return new ThreadPoolExecutor(calculateIOPoolSize(), calculateIOPoolSize(), KEEP_ALIVE_TIME,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(SEND_QUEUE_MAX_CAPACITY_SIZE),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy());
    }


    @Bean
    public ExecutorService compareExecutorService() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("replay-compare-%d")
                .setDaemon(true)
                .setUncaughtExceptionHandler(this)
                .build();
        return new ThreadPoolExecutor(CORE_POOL_SIZE, CORE_POOL_SIZE, KEEP_ALIVE_TIME,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(COMPARE_QUEUE_MAX_CAPACITY_SIZE),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy());

    }

    @Bean
    public ExecutorService actionItemParallelPool() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("replay-action-parallel-%d")
                .setDaemon(true)
                .setUncaughtExceptionHandler(this)
                .build();
        return new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(MAXIMUM_POOL_SIZE * 2),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        LOGGER.error("uncaughtException {} ,error :{}", t.getName(), e.getMessage(), e);
    }

    private int calculateIOPoolSize() {
        int nThreads = Runtime.getRuntime().availableProcessors();
        double targetCPUUtilization = 0.8;
        double wC = cpuRatio == 0 ? 3 : cpuRatio; // assume wait time is 5 times compute time
        int optimalThreadPoolSize = (int) Math.ceil(nThreads * targetCPUUtilization * (1 + wC));
        return optimalThreadPoolSize;
    }
}