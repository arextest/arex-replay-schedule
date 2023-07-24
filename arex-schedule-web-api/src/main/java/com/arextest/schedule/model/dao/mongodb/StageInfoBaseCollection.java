package com.arextest.schedule.model.dao.mongodb;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author wildeslam.
 * @create 2023/7/24 16:38
 */
@Data
@Document
public class StageInfoBaseCollection {
    private int stageType;
    private String stageName;
    private String msg;
    private int stageStatus;
    private Long startTime;
    private Long endTime;
}
