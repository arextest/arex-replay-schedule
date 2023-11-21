package com.arextest.schedule.model.noiseidentify;

import com.arextest.diff.model.log.NodeEntity;
import com.arextest.model.mock.MockCategoryType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by coryhh on 2023/10/17.
 */
@Data
public class QueryNoiseResponseType {

  private List<InterfaceNoiseItem> interfaceNoiseItemList;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class InterfaceNoiseItem {

    private String operationId;
    private List<MockerNoiseItem> randomNoise;
    private List<MockerNoiseItem> disorderedArrayNoise;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MockerNoiseItem {

    private MockCategoryType mockCategoryType;
    private String operationName;
    private String operationType;
    private List<NoiseItem> noiseItemList;
  }

  @Data
  public static class NoiseItem {

    private String identifier;

    private List<NodeEntity> nodeEntity;

    private List<Integer> logIndexes;

    private String compareResultId;

    /**
     * @see ReplayNoiseStatus
     */
    private Integer status;

  }

}
