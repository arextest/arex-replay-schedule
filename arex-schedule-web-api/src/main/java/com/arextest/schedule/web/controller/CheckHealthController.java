package com.arextest.schedule.web.controller;

import com.arextest.common.model.response.Response;
import com.arextest.common.utils.ResponseUtils;
import lombok.extern.slf4j.Slf4j;
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


    @GetMapping(value = "/health", produces = "application/json")
    @ResponseBody
    public Response checkHealth() {
        LOGGER.error("test schedule error checkHealth");
        return ResponseUtils.successResponse(true);
    }
}
