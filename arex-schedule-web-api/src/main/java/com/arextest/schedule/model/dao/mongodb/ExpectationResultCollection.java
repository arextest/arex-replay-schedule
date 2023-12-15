package com.arextest.schedule.model.dao.mongodb;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @since 2023/12/15
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Document("ReplayExpectationResult")
@FieldNameConstants
public class ExpectationResultCollection extends ModelBase {

    /**
     * @see ReplayRunDetailsCollection#id
     */
    private String caseId;
    private String category;
    private String operation;
    private String path;
    private String message;
    private boolean result;
    private String assertionText;
}
