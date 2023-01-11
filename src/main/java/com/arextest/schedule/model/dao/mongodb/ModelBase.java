package com.arextest.schedule.model.dao.mongodb;

import lombok.Data;
import org.springframework.data.annotation.Id;

/**
 * Created by rchen9 on 2022/8/9.
 */
@Data
public class ModelBase {
    @Id
    private String id;
    private Long dataChangeCreateTime;
    private Long dataChangeUpdateTime;
}