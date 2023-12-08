package com.arextest.schedule.common;

/**
 * @author wildeslam.
 * @create 2023/12/1 15:37
 */
public interface SendLimiter {

  boolean failBreak();

  void acquire();

  void release(boolean success);

  void batchRelease(boolean success, int size);

  int totalError();

  int continuousError();

  void reset();

}
