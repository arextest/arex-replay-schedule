package io.arex.replay.schedule.common;

import com.google.common.util.concurrent.RateLimiter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author hzmeng
 * @since 2021/11/09
 */
@SuppressWarnings("UnstableApiUsage")
@Slf4j
public final class SendSemaphoreLimiter {
    private static final int DEFAULT_MIN_RATE = 1;
    private static final int DEFAULT_MAX_RATE = 20;
    private int sendMaxRate;
    private volatile int permits;
    private final ReplayHealthy checker = new ReplayHealthy();
    private final RateLimiter rateLimiter;
    @Setter
    private int totalTasks;

    public SendSemaphoreLimiter() {
        this.rateLimiter = RateLimiter.create(DEFAULT_MIN_RATE);
        this.permits = DEFAULT_MIN_RATE;
    }

    public boolean failBreak() {
        return checker.failBreak();
    }

    public void acquire() {
        checker.tryChangeRate();
        rateLimiter.acquire();
    }

    public void release(boolean success) {
        checker.statistic(success);
    }

    public void reset() {
        checker.reset();
        permits = DEFAULT_MIN_RATE;
        LOGGER.info("reset rate to default permits: {}", permits);
        this.changeRateWithLog(permits);
    }

    private synchronized void tryReduceRate() {
        LOGGER.info("try reduce rate , permits: {}", this.permits);
        if (this.permits <= DEFAULT_MIN_RATE) {
            return;
        }
        this.changeRateWithLog(this.permits--);
    }

    private synchronized void tryIncreaseRate() {
        if (this.permits >= sendMaxRate) {
            return;
        }
        this.changeRateWithLog(this.permits++);
    }

    private void changeRateWithLog(double newPermitsPerSecond) {
        double beforeValue = this.rateLimiter.getRate();
        this.rateLimiter.setRate(newPermitsPerSecond);
        LOGGER.info("send rate permits: {} -> {} (per second)", beforeValue, newPermitsPerSecond);
    }


    public void setSendMaxRate(int replaySendMaxQps) {
        sendMaxRate = replaySendMaxQps > 0 ? replaySendMaxQps : DEFAULT_MAX_RATE;
    }

    /**
     * todo: 临时
     * 最初以最低频率执行， 没错误发生时，连需成功N(20)次就频率+1，直至最高频率
     * 出错时，降低频率，需要连需5N次成功才提升频率
     * 让出2N次出错，使其有机会降频去恢复，所以，设连续出错2N次或整个计划任务累计出错超10%，则中断回放
     */
    private final class ReplayHealthy {
        private final static int SUCCESS_COUNT_TO_BALANCE_NO_ERROR = 20;
        private final static int CONTINUOUS_FAIL_TOTAL = 2 * SUCCESS_COUNT_TO_BALANCE_NO_ERROR;
        private final static int SUCCESS_COUNT_TO_BALANCE_WITH_ERROR = 5 * SUCCESS_COUNT_TO_BALANCE_NO_ERROR;
        private final static double ERROR_BREAK_RATE = 0.1;
        private volatile boolean hasError;
        private final AtomicInteger continuousSuccessCounter = new AtomicInteger();
        private final AtomicInteger failCounter = new AtomicInteger();
        private final AtomicInteger continuousFailCounter = new AtomicInteger();
        private volatile int checkCount = SUCCESS_COUNT_TO_BALANCE_NO_ERROR;

        private void statistic(boolean success) {
            if (success) {
                continuousSuccessCounter.incrementAndGet();
                if (continuousFailCounter.get() < CONTINUOUS_FAIL_TOTAL) {
                    // reset to zero else pin down
                    continuousFailCounter.set(0);
                }
                return;
            }
            continuousFailCounter.incrementAndGet();
            continuousSuccessCounter.set(0);
            hasError = true;
            failCounter.incrementAndGet();
        }

        private boolean failBreak() {
            return continuousFailCounter.get() > CONTINUOUS_FAIL_TOTAL ||
                    (totalTasks > 0 && (failCounter.doubleValue() / totalTasks) > ERROR_BREAK_RATE);
        }

        private void reset() {
            continuousSuccessCounter.set(0);
            continuousFailCounter.set(0);
            checkCount = SUCCESS_COUNT_TO_BALANCE_NO_ERROR;
        }

        private void tryChangeRate() {
            if (hasError) {
                tryReduceRate();
                hasError = false;
                checkCount = SUCCESS_COUNT_TO_BALANCE_WITH_ERROR;
                return;
            }
            if (continuousSuccessCounter.get() > checkCount) {
                tryIncreaseRate();
                continuousSuccessCounter.set(0);
                checkCount = SUCCESS_COUNT_TO_BALANCE_NO_ERROR;
            }
        }
    }
}
