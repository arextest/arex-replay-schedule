package com.arextest.schedule.beans;

import com.arextest.common.model.classloader.RemoteJarClassLoader;
import com.arextest.common.utils.RemoteJarLoaderUtils;
import com.arextest.schedule.common.ClassLoaderUtils;
import com.arextest.schedule.extension.invoker.ReplayExtensionInvoker;
import com.arextest.schedule.sender.ReplaySender;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

  @Value("${load.invokers.switch}")
  private boolean loadInvokersSwitch;

  private static final String JAR_FILE_PATH = System.getProperty("replay.sender.extension.jarPath");
  private static final String TOMCAT_JAR_FILE_PATH = "/usr/local/tomcat/webapps/ROOT/WEB-INF/classes/lib/dubboInvoker.jar";

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
    if (!loadInvokersSwitch) {
      return invokers;
    }
    String loadJarFilePath =
        StringUtils.isEmpty(JAR_FILE_PATH) ? TOMCAT_JAR_FILE_PATH : JAR_FILE_PATH;
    ClassLoaderUtils.loadJar(loadJarFilePath);

    try {
      RemoteJarClassLoader classLoader = RemoteJarLoaderUtils.loadJar(loadJarFilePath);
      invokers.addAll(RemoteJarLoaderUtils.loadService(ReplayExtensionInvoker.class, classLoader));
    } catch (Throwable t) {
      LOGGER.error("Load invoker jar failed, application startup blocked", t);
      throw new RuntimeException("Load invoker jar failed");
    }
    LOGGER.info("Load invoker jar success, invokers: {}", invokers);
    return invokers;
  }
}
