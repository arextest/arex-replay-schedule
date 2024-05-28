package com.arextest.schedule.common;

import java.util.concurrent.TimeUnit;

/**
 * @author wyc王业超
 * @date 2020-09-20
 */
public final class CommonConstant {

  public static final int MAX_PAGE_SIZE = 1000;
  public static final String AREX_REPLAY_ID = "arex-replay-id";
  public static final String AREX_RECORD_ID = "arex-record-id";
  public static final String CONFIG_VERSION_HEADER_NAME = "arex_replay_prepare_dependency";
  public static final String AREX_REPLAY_WARM_UP = "arex-replay-warm-up";
  public static final String AREX_SCHEDULE_REPLAY = "arex-schedule-replay";
  public static final String X_AREX_EXCLUSION_OPERATIONS = "X-AREX-Exclusion-Operations";

  public static final long ONE_DAY_MILLIS = 60 * 60 * 24 * 1000L;
  public static final long ONE_HOUR_MILLIS = 60 * 60 * 1000L;
  public static final long THREE_SECOND_MILLIS = 3 * 1000L;
  public static final int OPERATION_MAX_CASE_COUNT = 1000;

  public static final int GROUP_SENT_WAIT_TIMEOUT_SECONDS = 500;

  // region redis
  public static final String COMPARE_CONFIG_REDIS_KEY = "compare.config.";
  public static final String COMPARE_GLOBAL_CONFIG_REDIS_KEY = "compare.config.global";

  public static final long CREATE_PLAN_REDIS_EXPIRE = TimeUnit.MINUTES.toSeconds(5);
  public static final long STOP_PLAN_REDIS_EXPIRE = TimeUnit.DAYS.toSeconds(1);
  public static final String STOP_PLAN_REDIS_KEY = "arex.stop.plan.";
  // endregion

  public static final String DOT = ".";
  public static final String JSON_START = "{";
  public static final String JSON_ARRAY_START = "[";
  public static final long DEFAULT_COUNT = 1L;
  public static final String URL = "url";

  public static final String NOISE_HANDLER = "-noisehandler";
}