package com.arextest.schedule.common;

import com.google.common.util.concurrent.RateLimiter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Max QPS = APP Config Max || DEFAULT MAX(20)
 * Initial QPS = Max QPS * STEP 0 RATIO (0.3)
 *
 * Consecutive 20 success API call -> try increase QPS to next step || increase by one if lower than the lowest ratio
 * Any failed API call -> try reducing QPS to previous step || reduce by one if already lower or equal to the lowest step ratio
 *
 * Consecutive 40 failed API call || 10% of total case failed -> break execution
 *
 * @author hzmeng
 * @since 2021/11/09
 */
@SuppressWarnings("UnstableApiUsage")
@Slf4j
public final class SendSemaphoreLimiter {
    private static final double[] QPS_STEP_RATIO = {0.3, 0.5, 0.7, 1};
    private static final int QPS_MAX_STEP = QPS_STEP_RATIO.length - 1;
    private static final double QPS_INITIAL_RATIO = QPS_STEP_RATIO[0];
    private static final int DEFAULT_MAX_RATE = 20;
    private static final int ABSOLUTE_MIN_GUARD = 1;
    private final int sendMaxRate;
    private final int sendInitialRate;
    private volatile int permits;
    private volatile int currentStep = 0;
    private final ReplayHealthy checker = new ReplayHealthy();
    private final RateLimiter rateLimiter;

    @Setter
    private int totalTasks;

    public SendSemaphoreLimiter(Integer maxQps) {
        Integer actualMaxQps = Optional.ofNullable(maxQps).filter(qps -> qps > 0).orElse(DEFAULT_MAX_RATE);
        Integer actualInitialMinQps = SendSemaphoreLimiter.calculateInitialRate(actualMaxQps);

        this.sendInitialRate = actualInitialMinQps;
        this.sendMaxRate = actualMaxQps;
        this.rateLimiter = RateLimiter.create(actualInitialMinQps);
        this.permits = actualInitialMinQps;
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

    public void batchRelease(boolean success, int size) {
        if (success) {
            checker.batchSuccess(size);
        } else {
            checker.batchFail(size);
        }
    }

    private synchronized void tryModifyRate(boolean increase) {
        Integer originalQps = this.permits;
        Integer newQps = originalQps;

        if (increase) {
            // try step forward
            if (this.currentStep < QPS_MAX_STEP && originalQps.equals(getRatioOfStep(this.currentStep))) {
                this.currentStep += 1;
                newQps = getRatioOfStep(this.currentStep);
                // try increase by one until reach Min step
            } else if (this.currentStep < QPS_MAX_STEP) {
                newQps = originalQps + 1;
            }
        } else {
            // try step back
            if (this.currentStep > 1) {
                this.currentStep -= 1;
                newQps = getRatioOfStep(this.currentStep);
                // try reducing to absolute min
            } else if (originalQps > ABSOLUTE_MIN_GUARD) {
                newQps = originalQps - 1;
            } else {
                newQps = ABSOLUTE_MIN_GUARD;
            }
        }

        if (!newQps.equals(originalQps)) {
            this.permits = newQps;
            this.rateLimiter.setRate(newQps);
            LOGGER.info("send rate permits: {} -> {} (per second)", originalQps, newQps);
        }
    }

    private Integer getRatioOfStep(int step) {
        return (int) Math.floor(sendMaxRate * QPS_STEP_RATIO[step]);
    }

    private static Integer calculateInitialRate(int sendMaxRate) {
        return (int) Math.floor(sendMaxRate * QPS_INITIAL_RATIO);
    }

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

        private void batchFail(int size) {
            hasError = true;

            continuousSuccessCounter.set(0);
            continuousFailCounter.addAndGet(size);
            failCounter.addAndGet(size);
        }

        private void batchSuccess(int size) {
            continuousSuccessCounter.addAndGet(size);
            if (continuousFailCounter.get() < CONTINUOUS_FAIL_TOTAL) {
                // reset to zero else pin down
                continuousFailCounter.set(0);
            }
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
                tryModifyRate(false);
                hasError = false;
                checkCount = SUCCESS_COUNT_TO_BALANCE_WITH_ERROR;
                return;
            }
            if (continuousSuccessCounter.get() > checkCount) {
                tryModifyRate(true);
                continuousSuccessCounter.set(0);
                checkCount = SUCCESS_COUNT_TO_BALANCE_NO_ERROR;
            }
        }
    }
}