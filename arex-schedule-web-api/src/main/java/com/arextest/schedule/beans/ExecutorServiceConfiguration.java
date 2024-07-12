package com.arextest.schedule.beans;

import com.alibaba.ttl.threadpool.TtlExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jmo
 * @since 2021/9/16
 */
@Configuration
@Slf4j
class ExecutorServiceConfiguration implements Thread.UncaughtExceptionHandler {

  private static final long KEEP_ALIVE_TIME = 60L;
  private static final int CORE_POOL_SIZE = 2 * Runtime.getRuntime().availableProcessors();
  private static final int CPU_INTENSIVE_CORE_POOL_SIZE =
      Runtime.getRuntime().availableProcessors() + 1;
  private static final int MAXIMUM_POOL_SIZE = 2 * CORE_POOL_SIZE;
  private static final int SEND_QUEUE_MAX_CAPACITY_SIZE = 4000;
  private static final int COMPARE_QUEUE_MAX_CAPACITY_SIZE = 2000;
  private static final int PRELOAD_QUEUE_MAX_CAPACITY_SIZE = 100;
  private static final int NOISE_ANALYSIS_QUEUE_MAX_CAPACITY_SIZE = 100;
  private static final int AUTO_RERUN_QUEUE_MAX_CAPACITY_SIZE = 100;
  private static final int POST_SEND_QUEUE_MAX_CAPACITY_SIZE = 100;
  private static final int DISTRIBUTE_QUEUE_MAX_CAPACITY_SIZE = 100;


  @Value("${arex.schedule.pool.io.cpuratio}")
  private int cpuRatio;

  @Bean
  public ExecutorService preloadExecutorService() {
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("replay-preload-%d")
        .setDaemon(true)
        .setUncaughtExceptionHandler(this).build();
    ExecutorService executorService = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
        KEEP_ALIVE_TIME,
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(PRELOAD_QUEUE_MAX_CAPACITY_SIZE), threadFactory,
        new ThreadPoolExecutor.CallerRunsPolicy());
    return TtlExecutors.getTtlExecutorService(executorService);
  }

  /**
   * This bean should be overridden according to the actual implementation of distribute service
   */
  @Bean
  public ExecutorService distributeExecutorService() {
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("replay-distribute-%d")
            .setDaemon(true)
            .setUncaughtExceptionHandler(this).build();
    ExecutorService executorService = new ThreadPoolExecutor(calculateIOPoolSize(),
            calculateIOPoolSize(), KEEP_ALIVE_TIME,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(DISTRIBUTE_QUEUE_MAX_CAPACITY_SIZE),
            threadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy());
    return TtlExecutors.getTtlExecutorService(executorService);
  }

  /**
   * This bean should be overridden according to the actual implementation of compare service
   */
  @Bean
  public ExecutorService sendExecutorService() {
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("replay-send-%d")
        .setDaemon(true)
        .setUncaughtExceptionHandler(this).build();
    ExecutorService executorService = new ThreadPoolExecutor(calculateIOPoolSize(),
        calculateIOPoolSize(), KEEP_ALIVE_TIME,
        TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(SEND_QUEUE_MAX_CAPACITY_SIZE),
        threadFactory,
        new ThreadPoolExecutor.CallerRunsPolicy());
    return TtlExecutors.getTtlExecutorService(executorService);
  }

  @Bean
  public ExecutorService compareExecutorService() {
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("replay-compare-%d")
        .setDaemon(true)
        .setUncaughtExceptionHandler(this).build();
    ExecutorService executorService = new ThreadPoolExecutor(CORE_POOL_SIZE, CORE_POOL_SIZE,
        KEEP_ALIVE_TIME,
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(COMPARE_QUEUE_MAX_CAPACITY_SIZE), threadFactory,
        new ThreadPoolExecutor.CallerRunsPolicy());
    return TtlExecutors.getTtlExecutorService(executorService);
  }

  @Bean
  public ExecutorService compareScheduleExecutorService() {
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(
            "replay-compare-schedule-%d")
        .setDaemon(true)
        .setUncaughtExceptionHandler(this).build();
    ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(CORE_POOL_SIZE,
        threadFactory);
    return TtlExecutors.getTtlScheduledExecutorService(executorService);
  }

  @Bean
  public ExecutorService rerunPrepareExecutorService() {
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(
            "replay-rerun-prepare-%d")
        .setDaemon(true).setUncaughtExceptionHandler(this).build();
    ExecutorService executorService = new ThreadPoolExecutor(CORE_POOL_SIZE, CORE_POOL_SIZE,
        KEEP_ALIVE_TIME,
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(COMPARE_QUEUE_MAX_CAPACITY_SIZE), threadFactory,
        new ThreadPoolExecutor.CallerRunsPolicy());
    return TtlExecutors.getTtlExecutorService(executorService);
  }

  @Bean
  public ExecutorService analysisNoiseExecutorService() {
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("noise-analysis-%d")
        .setDaemon(true)
        .setUncaughtExceptionHandler(this).build();
    ExecutorService executorService = new ThreadPoolExecutor(CPU_INTENSIVE_CORE_POOL_SIZE,
        CORE_POOL_SIZE, KEEP_ALIVE_TIME,
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(NOISE_ANALYSIS_QUEUE_MAX_CAPACITY_SIZE), threadFactory,
        new ThreadPoolExecutor.CallerRunsPolicy());

    return TtlExecutors.getTtlExecutorService(executorService);
  }

  @Bean
  public ExecutorService autoRerunExecutorService() {
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("replay-auto-rerun-%d")
        .setDaemon(true)
        .setUncaughtExceptionHandler(this).build();
    ExecutorService executorService = new ThreadPoolExecutor(CORE_POOL_SIZE, CORE_POOL_SIZE,
        KEEP_ALIVE_TIME,
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(AUTO_RERUN_QUEUE_MAX_CAPACITY_SIZE), threadFactory,
        new ThreadPoolExecutor.CallerRunsPolicy());
    return TtlExecutors.getTtlExecutorService(executorService);
  }

  @Bean
  public ExecutorService postSendExecutorService() {
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("post-send-%d")
        .setDaemon(true)
        .setUncaughtExceptionHandler(this).build();
    ExecutorService executorService = new ThreadPoolExecutor(CPU_INTENSIVE_CORE_POOL_SIZE,
        CORE_POOL_SIZE, KEEP_ALIVE_TIME,
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(POST_SEND_QUEUE_MAX_CAPACITY_SIZE), threadFactory,
        new ThreadPoolExecutor.CallerRunsPolicy());
    return TtlExecutors.getTtlExecutorService(executorService);
  }

  @Bean(name = "custom-fork-join-executor")
  public ExecutorService customForkJoinExecutor() {
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("forkJoin-handler-%d")
        .setDaemon(true)
        .setUncaughtExceptionHandler(this).build();
    int parallelism = Runtime.getRuntime().availableProcessors();
    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(parallelism, parallelism,
        60, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1000),
        threadFactory, new CallerRunsPolicy());
    return TtlExecutors.getTtlExecutorService(threadPoolExecutor);
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

  @Bean
  public ScheduledExecutorService monitorScheduler() {
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("replay-monitor-%d")
        .setDaemon(true)
        .setUncaughtExceptionHandler(this).build();
    ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(2,
        threadFactory);
    return TtlExecutors.getTtlScheduledExecutorService(scheduledThreadPoolExecutor);
  }
}