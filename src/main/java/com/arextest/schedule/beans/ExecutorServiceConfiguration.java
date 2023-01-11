package com.arextest.schedule.beans;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    @Bean
    public ExecutorService sendExecutorService() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("replay-send-%d")
                .setDaemon(true)
                .setUncaughtExceptionHandler(this)
                .build();
        return new ThreadPoolExecutor(MAXIMUM_POOL_SIZE,
                MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(SEND_QUEUE_MAX_CAPACITY_SIZE), threadFactory);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        LOGGER.error("uncaughtException {} ,error :{}", t.getName(), e.getMessage(), e);
    }
}