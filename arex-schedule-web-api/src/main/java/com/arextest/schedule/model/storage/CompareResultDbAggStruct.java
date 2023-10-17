package com.arextest.schedule.model.storage;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.List;

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
