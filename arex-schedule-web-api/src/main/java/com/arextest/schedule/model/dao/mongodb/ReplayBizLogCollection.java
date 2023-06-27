package com.arextest.schedule.model.dao.mongodb;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Created by qzmo on 2023/5/31.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Document("ReplayBizLog")
public class ReplayBizLogCollection extends ModelBase {
    private Date date;
    private int level;
    private String message;
    private int logType;

    private String planId;
    private boolean resumedExecution;
    private String contextName;

    private String contextIdentifier;
    private String caseItemId;
    private String actionItemId;
    private String operationName;

    private String exception;
    private String request;
    private String response;
    private String traceId;
    private String extra;
}