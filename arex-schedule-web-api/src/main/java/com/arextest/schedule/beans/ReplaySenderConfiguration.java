package com.arextest.schedule.beans;

import com.arextest.schedule.extension.invoker.ReplayExtensionInvoker;
import com.arextest.schedule.sender.ReplaySender;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
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

  private static final String LOCAL_INVOKER_PATH = "lib/dubboInvoker.jar";
  private static final String NEW_INVOKER_PATH = "dubboInvoker.jar";

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

    try {
      URL classPathResource;
      if (StringUtils.isEmpty(jarFilePath)) {
        classPathResource = loadLocalInvokerJar();
      } else {
        classPathResource = new File(jarFilePath).toURI().toURL();
      }

      ClassLoader original = Thread.currentThread().getContextClassLoader();
      URLClassLoader urlLoader = new URLClassLoader(new URL[]{classPathResource},
          this.getClass().getClassLoader());
      Thread.currentThread().setContextClassLoader(urlLoader);
      Class.forName(ReplayExtensionInvoker.class.getName(), true, urlLoader);
      ServiceLoader.load(ReplayExtensionInvoker.class).forEach(invokers::add);
      Thread.currentThread().setContextClassLoader(original);
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

  private void inputStreamToFile(InputStream inputStream, String filePath) throws IOException {
    File file = new File(filePath);
    FileOutputStream fos = new FileOutputStream(file);
    byte[] buffer = new byte[1024];
    int length;
    while ((length = inputStream.read(buffer)) > 0) {
      fos.write(buffer, 0, length);
    }
    inputStream.close();
    fos.close();
  }

  private URL loadLocalInvokerJar() throws IOException {
    ClassPathResource classPathResource = new ClassPathResource(LOCAL_INVOKER_PATH);
    InputStream inputStream = classPathResource.getInputStream();
    inputStreamToFile(inputStream, NEW_INVOKER_PATH);

    File file = new File(NEW_INVOKER_PATH);
    return file.toURI().toURL();
  }


}
