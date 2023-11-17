package com.arextest.schedule.web.controller;

import com.arextest.common.model.response.Response;
import com.arextest.common.utils.ResponseUtils;
import java.io.FileReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
@RequestMapping("/vi/")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CheckHealthController {

  private static final String POM_PATH_TOMCAT = "/usr/local/tomcat/webapps/ROOT/META-INF/maven"
      + "/com.arextest/arex-schedule-web-api/pom.xml";
  private static String VERSION;

  static {
    try {
      Model pomModel = new MavenXpp3Reader().read(new FileReader(POM_PATH_TOMCAT));
      VERSION = pomModel.getVersion();
    } catch (Exception e) {
      LOGGER.error("Read pom failed!", e);
    }
  }

  @GetMapping(value = "/health", produces = "application/json")
  @ResponseBody
  public Response checkHealth() {
    return ResponseUtils.successResponse(VERSION);
  }
}
