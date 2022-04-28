package com.arextest.replay.schedule.model.config;

import lombok.Data;

import java.util.List;

/**
 * Created by wang_yc on 2021/9/26
 */
@Data
public class CompareConfigForReport {
    private Integer appId;
    private List<CompareIgnoreConfig> ignoreConfigList;
}
