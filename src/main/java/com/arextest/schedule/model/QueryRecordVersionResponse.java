package com.arextest.schedule.model;

import lombok.Data;


import java.util.List;

/**
 * @author: sldu
 * @date: 2023/3/14 17:58
 **/
@Data
public class QueryRecordVersionResponse {


    private List<ApplicationInstance> body;
    
    @Data
    public static class ApplicationInstance{
        private String appId;
        private String recordVersion;
  }

}
