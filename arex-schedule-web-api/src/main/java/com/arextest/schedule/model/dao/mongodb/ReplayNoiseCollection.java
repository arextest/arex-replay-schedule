package com.arextest.schedule.model.dao.mongodb;

import com.arextest.diff.model.log.NodeEntity;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by coryhh on 2023/10/17.
 */
@Document(collection = "ReplayNoise")
@Data
@FieldNameConstants
public class ReplayNoiseCollection extends ModelBase {

  private String planId;

  private String planItemId;

  private String categoryName;

  private String operationId;

  private String operationName;

  // k: path v: data
  private Map<String, ReplayNoiseItemDao> mayIgnoreItems;

  private Map<String, ReplayNoiseItemDao> mayDisorderItems;

  @Data
  @FieldNameConstants
  public static class ReplayNoiseItemDao {

    private List<NodeEntity> nodePath;

    private String compareResultId;

    private List<Integer> logIndexes;

    private Map<String, Integer> subPaths;

    private Integer pathCount;

    private Integer caseCount;

    private Integer status;

  }
}
