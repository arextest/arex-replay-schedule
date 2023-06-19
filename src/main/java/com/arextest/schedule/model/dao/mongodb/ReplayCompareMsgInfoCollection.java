package com.arextest.schedule.model.dao.mongodb;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by Qzmo on 2023/6/19
 */
@Document
@Data
public class ReplayCompareMsgInfoCollection {
    private int msgMiss;
}
