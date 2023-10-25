package com.arextest.schedule.model.config;

import java.util.List;
import lombok.Data;

/**
 * Created by rchen9 on 2022/9/20.
 */
@Data
public class CompareListSortConfig extends AbstractCompareDetailConfig {

  private List<String> listPath;

  private List<List<String>> keys;

}