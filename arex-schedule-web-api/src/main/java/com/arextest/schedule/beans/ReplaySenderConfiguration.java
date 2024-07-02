package com.arextest.schedule.beans;

import com.arextest.schedule.common.ClassLoaderUtils;
import com.arextest.schedule.extension.invoker.ReplayExtensionInvoker;
import com.arextest.schedule.sender.ReplaySender;
import com.arextest.schedule.sender.ReplaySenderFactory;
import com.arextest.schedule.sender.impl.DefaultDubboReplaySender;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * @author: sldu
 * @date: 2023/7/19 11:18
 **/
@Configuration
@Slf4j
public class ReplaySenderConfiguration {
  @Value("${replay.sender.extension.jarPath}")
  private String jarFilePath;
  @Value("${replay.sender.extension.jarPath.remote}")
  private String remoteJarFilePath;

  private static final String LOCAL_INVOKER_PATH = "lib/dubboInvoker.jar";
  private static final String NEW_INVOKER_PATH = "dubboInvoker.jar";

  @Bean("replayExtensionInvoker")
  @ConditionalOnProperty(name = "replay.sender.extension.switch", havingValue = "true")
  public List<ReplayExtensionInvoker> invokers() {
    List<ReplayExtensionInvoker> invokers = new ArrayList<>();

    try {
      URL classPathResource;
      if (StringUtils.isNotBlank(remoteJarFilePath)) {
        classPathResource = new URL(jarFilePath);
      } else if (StringUtils.isNotBlank(jarFilePath)) {
        classPathResource = new File(jarFilePath).toURI().toURL();
      } else {
        classPathResource = loadLocalInvokerJar();
      }
      ClassLoaderUtils.loadJar(classPathResource);
      ServiceLoader.load(ReplayExtensionInvoker.class).forEach(invokers::add);
    } catch (Throwable t) {
      LOGGER.error("Load invoker jar failed, application startup blocked", t);
    }
    if (invokers.isEmpty()) {
      LOGGER.error("No invoker found, application startup blocked");
      throw new RuntimeException("No invoker found");
    }
    LOGGER.info("Load invoker jar success, invokers: {}", invokers);
    return invokers;
  }

  @Bean
  @ConditionalOnProperty(name = "replay.sender.extension.switch", havingValue = "true")
  public DefaultDubboReplaySender dubboReplaySender(
      @Value("#{'${arex.replay.header.excludes.dubbo}'.split(',')}") List<String> excludes,
      List<ReplayExtensionInvoker> invokers) {

    return new DefaultDubboReplaySender(excludes, invokers);
  }

  @Bean
  public ReplaySenderFactory replaySenderFactory(List<ReplaySender> senders) {
    return new ReplaySenderFactory(senders);
  }

  private void inputStreamToFile(InputStream inputStream, String filePath) throws IOException {
    File file = new File(filePath);
    try (FileOutputStream fos = new FileOutputStream(file)) {
      byte[] buffer = new byte[1024];
      int length;
      while ((length = inputStream.read(buffer)) > 0) {
        fos.write(buffer, 0, length);
      }
      inputStream.close();
    }
  }

  // load .jar by inputstream and write to file.
  private URL loadLocalInvokerJar() throws IOException {
    ClassPathResource classPathResource = new ClassPathResource(LOCAL_INVOKER_PATH);
    InputStream inputStream = classPathResource.getInputStream();
    inputStreamToFile(inputStream, NEW_INVOKER_PATH);

    File file = new File(NEW_INVOKER_PATH);
    return file.toURI().toURL();
  }
}
