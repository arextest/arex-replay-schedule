package com.arextest.schedule.dao.mongodb;

import com.arextest.schedule.dao.RepositoryWriter;
import com.arextest.schedule.model.ReplayCompareResult;
import com.arextest.schedule.model.converter.ReplayCompareResultConverter;
import com.arextest.schedule.model.dao.mongodb.ReplayCompareResultCollection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Component
public class ReplayCompareResultRepositoryImpl implements RepositoryWriter<ReplayCompareResult>, RepositoryField {
    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private ReplayCompareResultConverter replayCompareResultConverter;

    @Override
    public boolean save(List<ReplayCompareResult> itemList) {
        mongoTemplate.insertAll(itemList.stream().map(replayCompareResultConverter::daoFromBo)
                .collect(Collectors.toList()));
        return true;
    }

    @Override
    public boolean save(ReplayCompareResult item) {
        mongoTemplate.save(replayCompareResultConverter.daoFromBo(item));
        return true;
    }

    public boolean deleteByRecord(String recordId, String planItemId) {
        Query query = new Query();
        query.addCriteria(Criteria.where(ReplayCompareResult.Fields.recordId).is(recordId)
                .and(ReplayCompareResult.Fields.planItemId).is(planItemId));
        return mongoTemplate.remove(query, ReplayCompareResultCollection.class).getDeletedCount() > 0;
    }

    public ReplayCompareResult queryCompareResultsById(String objectId) {
        Query query = new Query();
        query.addCriteria(Criteria.where(DASH_ID).is(objectId));
        ReplayCompareResultCollection result = mongoTemplate.findOne(query, ReplayCompareResultCollection.class);
        return replayCompareResultConverter.boFromDao(result);
    }
}