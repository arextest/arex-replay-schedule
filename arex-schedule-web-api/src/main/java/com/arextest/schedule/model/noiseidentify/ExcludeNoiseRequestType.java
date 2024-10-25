package com.arextest.schedule.model.noiseidentify;

import com.arextest.diff.model.log.NodeEntity;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.schedule.model.noiseidentify.QueryNoiseResponseType.InterfaceNoiseItem;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class ExcludeNoiseRequestType {

  @NotBlank(message = "appId cannot be blank")
  private String appId;
  @NotBlank(message = "planId cannot be blank")
  private String planId;

  private List<InterfaceNoiseItem> interfaceNoiseItemList;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class InterfaceExcludeNoiseItem {
    private String operationId;
    private List<QueryNoiseResponseType.MockerNoiseItem> randomNoise;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MockerExcludeNoiseItem {

    private MockCategoryType mockCategoryType;
    private String operationName;
    private String operationType;
    private List<QueryNoiseResponseType.NoiseItem> noiseItemList;
  }

  @Data
  public static class ExcludeNoiseItem {

    private String identifier;

    private List<NodeEntity> nodeEntity;

  }

}
