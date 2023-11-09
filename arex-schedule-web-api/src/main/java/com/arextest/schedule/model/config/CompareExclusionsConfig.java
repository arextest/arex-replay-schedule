package com.arextest.schedule.model.config;

import java.util.List;
import lombok.Data;

/**
 * Created by rchen9 on 2022/9/19.
 */
@Deprecated
@Data
public class CompareExclusionsConfig extends AbstractCompareDetailConfig {

  private List<String> exclusions;
}