package com.arextest.schedule.model.dao.mongodb;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author wildeslam.
 * @create 2023/7/24 16:41
 */
@Data
@Document
@EqualsAndHashCode(callSuper = true)
public class ReplayPlanStageInfoCollection extends StageInfoBaseCollection{
    private List<StageInfoBaseCollection> subStageInfoList;
}
