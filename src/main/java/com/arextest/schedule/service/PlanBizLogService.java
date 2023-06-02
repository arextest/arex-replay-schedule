package com.arextest.schedule.service;

import com.arextest.schedule.dao.mongodb.ReplayBizLogRepository;
import com.arextest.schedule.model.dao.mongodb.ReplayBizLogCollection;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * Created by Qzmo on 2023/6/2
 */
@Service
public class PlanBizLogService {
    @Resource
    private ReplayBizLogRepository replayBizLogRepository;

    public List<ReplayBizLogCollection> queryBizLogsByPlanId(String planId) {
        if (StringUtils.isEmpty(planId)) {
            return Collections.emptyList();
        }

        return replayBizLogRepository.queryByPlanId(planId);
    }
}
