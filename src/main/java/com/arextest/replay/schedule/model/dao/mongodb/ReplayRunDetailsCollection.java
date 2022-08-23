package com.arextest.replay.schedule.model.dao.mongodb;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

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
    private int caseType;
    private long recordTime;
    private String replayDependency;
    private String requestMessage;
    private String consumeGroup;
    private String requestMessageFormat;
    private Map<String, String> requestHeaders;
    private String requestMethod;
    private String requestPath;

}
