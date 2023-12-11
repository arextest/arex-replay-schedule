package com.arextest.schedule.beans;

import com.google.common.eventbus.AsyncEventBus;
import java.util.concurrent.ExecutorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author wildeslam.
 * @create 2023/12/11 16:38
 */
@Configuration
public class EventBusConfiguration {

  @Bean
  public AsyncEventBus autoRerunAsyncEventBus(ExecutorService autoRerunExecutorService) {
    return new AsyncEventBus("replay-event-bus", autoRerunExecutorService);
  }

}
