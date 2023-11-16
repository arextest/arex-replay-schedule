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

  private static Model POM_MODEL;

  static {
    try {
      POM_MODEL = new MavenXpp3Reader().read(new FileReader("pom.xml"));
    } catch (Exception e) {
      LOGGER.error("Read pom failed!", e);
    }
  }

  @GetMapping(value = "/health", produces = "application/json")
  @ResponseBody
  public Response checkHealth() {
    return ResponseUtils.successResponse(getVersion());
  }

  private static String getVersion() {
    if (POM_MODEL != null) {
      return POM_MODEL.getVersion();
    } else {
      return "error";
    }
  }
}
