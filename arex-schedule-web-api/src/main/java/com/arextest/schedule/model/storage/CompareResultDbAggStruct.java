package com.arextest.schedule.model.storage;

import java.util.List;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants
public class CompareResultDbAggStruct {

  private AggId _id;
  private int diffResultCode;
  private String categoryName;
  private List<ResultCodeGroup.IdPair> relatedIds;

  @Data
  public static class AggId {

    private int diffResultCode;
    private String categoryName;
  }
}
