package com.arextest.schedule.web.controller;

import com.arextest.common.model.response.Response;
import com.arextest.common.utils.ResponseUtils;
import com.arextest.schedule.model.collection.CompareMsgRequestType;
import com.arextest.schedule.service.collection.CollectionReportService;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(path = "/api/collection", produces = {MediaType.APPLICATION_JSON_VALUE})
@CrossOrigin(origins = "*", maxAge = 3600)
public class CollectionReportController {

  @Resource
  CollectionReportService collectionReportService;

  @PostMapping("/compareMsg")
  public Response compareMsg(@RequestBody CompareMsgRequestType request) {
    return ResponseUtils.successResponse(collectionReportService.compareMsg(request));
  }
}
