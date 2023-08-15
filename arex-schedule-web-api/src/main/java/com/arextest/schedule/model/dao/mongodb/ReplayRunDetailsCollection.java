package com.arextest.schedule.model.dao.mongodb;

import com.arextest.model.mock.Mocker.Target;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Created by rchen9 on 20/8/18.
 */
@Data
@NoArgsConstructor
@Document(ReplayRunDetailsCollection.COLLECTION_NAME)
@FieldNameConstants
public class ReplayRunDetailsCollection extends ModelBase {
    public static final String COLLECTION_NAME = "ReplayRunDetails";

    private String planId;
    private String planItemId;
    private String operationId;
    @NonNull
    private String recordId;
    @NonNull
    private String targetResultId;
    @NonNull
    private String sourceResultId;
    private String contextIdentifier;
    private int sendStatus;
    private int compareStatus;
    private String caseType;
    private long recordTime;
    @Field
    private Target targetRequest;
}