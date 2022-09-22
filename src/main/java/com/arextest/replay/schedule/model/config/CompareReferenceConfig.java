package com.arextest.replay.schedule.model.config;

import lombok.Data;

import java.util.List;

/**
 * Created by rchen9 on 2022/9/20.
 */
@Data
public class CompareReferenceConfig extends AbstractCompareDetailConfig {

    private List<String> pkPath;

    private List<String> fkPath;

}
