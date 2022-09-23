package com.arextest.replay.schedule.model.config;

import lombok.Data;

import java.util.List;

/**
 * Created by rchen9 on 2022/9/19.
 */
@Data
public class CompareExclusionsConfig extends AbstractCompareDetailConfig {
    private List<String> exclusions;
}
