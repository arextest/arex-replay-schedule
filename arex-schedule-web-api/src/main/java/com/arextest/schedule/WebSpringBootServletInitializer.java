package com.arextest.schedule;

import com.arextest.common.metrics.PrometheusConfiguration;
import java.awt.Desktop;
import java.net.URI;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.retry.annotation.EnableRetry;

/**
 * @author jmo
 * @since 2021/8/18
 */
@Slf4j
@EnableRetry
@SpringBootApplication(scanBasePackages = "com.arextest.schedule", exclude = {
    DataSourceAutoConfiguration.class})
public class WebSpringBootServletInitializer extends SpringBootServletInitializer {

  private static final String AES_KEY_FIELD_NAME = "arex.desensitization.aesKey";

  @Value("${arex.prometheus.port}")
  String prometheusPort;

  @Value("${arex.desensitization.aesKey:}")
  private String aesKey;

  public static void main(String[] args) {
    System.setProperty("java.awt.headless", "false");
    try {
      SpringApplication.run(WebSpringBootServletInitializer.class, args);
    } catch (BeanCreationException e) {
      LOGGER.error("Application create error", e);
      System.exit(0);
    }

    try {
      Desktop.getDesktop().browse(new URI("http://localhost:8080/vi/health"));
    } catch (Exception e) {
      LOGGER.error("browse error", e);
    }
  }

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    return application.sources(WebSpringBootServletInitializer.class);
  }

  @PostConstruct
  public void init() {
    PrometheusConfiguration.initMetrics(prometheusPort);
    importConfigurationToEnv();
  }

  public void importConfigurationToEnv() {
    String aesKeyProperty = System.getProperty(AES_KEY_FIELD_NAME);
    if (StringUtils.isEmpty(aesKeyProperty) && StringUtils.isNotEmpty(aesKey)) {
      System.setProperty(AES_KEY_FIELD_NAME, aesKey);
    }
  }

}