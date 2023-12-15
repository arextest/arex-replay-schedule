package com.arextest.schedule.beans;

import com.arextest.common.model.classloader.RemoteJarClassLoader;
import com.arextest.common.utils.RemoteJarLoaderUtils;
import com.arextest.schedule.common.ClassLoaderUtils;
import com.arextest.schedule.extension.invoker.ReplayExtensionInvoker;
import com.arextest.schedule.sender.ReplaySender;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: sldu
 * @date: 2023/7/19 11:18
 **/
@Configuration
@Slf4j
public class ReplaySenderConfiguration {

  @Value("${replay.sender.extension.jarPath}")
  private String jarFilePath;

  @Bean
  public List<ReplaySender> replaySenderList(List<ReplaySender> replaySenders) {
    // sort by order
    return replaySenders.stream()
        .sorted(Comparator.comparing(ReplaySender::getOrder, Comparator.reverseOrder()))
        .collect(Collectors.toList());
  }

  @Bean
  public List<ReplayExtensionInvoker> invokers() {
    List<ReplayExtensionInvoker> invokers = new ArrayList<>();
    if (StringUtils.isEmpty(jarFilePath)) {
      return invokers;
    }
    try {
      ClassLoaderUtils.loadJar(jarFilePath);
      ServiceLoader.load(ReplayExtensionInvoker.class).forEach(invokers::add);
    } catch (Throwable t) {
      LOGGER.error("Load invoker jar failed, application startup blocked", t);
      throw new RuntimeException("Load invoker jar failed");
    }
    LOGGER.info("Load invoker jar success, invokers: {}", invokers);
    return invokers;
  }
}
