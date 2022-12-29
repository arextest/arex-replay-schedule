package com.arextest.schedule.model.dao.mongodb;

import com.arextest.model.mock.Mocker.Target;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Created by rchen9 on 20/8/18.
 */
@Data
@NoArgsConstructor
@Document("ReplayRunDetails")
public class ReplayRunDetailsCollection extends ModelBase {
    private String planItemId;
    private String operationId;
    @NonNull
    private String recordId;
    @NonNull
    private String targetResultId;
    @NonNull
    private String sourceResultId;
    private int sendStatus;
    private int compareStatus;
    private String caseType;
    private long recordTime;
    @Field
    private Target targetRequest;
}