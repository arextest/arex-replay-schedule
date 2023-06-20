package com.arextest.schedule.web.boot;

import com.arextest.schedule.common.ClassLoaderUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import java.awt.*;
import java.net.URI;

/**
 * @author jmo
 * @since 2021/8/18
 */
@SpringBootApplication(scanBasePackages = "com.arextest.schedule", exclude = {DataSourceAutoConfiguration.class})
public class WebSpringBootServletInitializer extends SpringBootServletInitializer {
    private static final String JAR_FILE_PATH = System.getProperty("replay.sender.extension.jarPath");

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        ClassLoaderUtils.loadJar(JAR_FILE_PATH);
        return application.sources(WebSpringBootServletInitializer.class);
    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        try {
            SpringApplication.run(WebSpringBootServletInitializer.class, args);
            Desktop.getDesktop().browse(new URI("http://localhost:8080/api/createPlan"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}