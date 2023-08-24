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
public class ReplayCompareResultRepositoryImpl
        extends DesensitizationRepo implements RepositoryWriter<ReplayCompareResult>, RepositoryField {
    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public boolean save(List<ReplayCompareResult> itemList) {
        mongoTemplate.insertAll(itemList.stream().map(ReplayCompareResultConverter.INSTANCE::daoFromBo)
                .collect(Collectors.toList()));
        return true;
    }

    @Override
    public boolean save(ReplayCompareResult item) {
        mongoTemplate.save(ReplayCompareResultConverter.INSTANCE.daoFromBo(item));
        return true;
    }

    public boolean deleteByRecord(String recordId, String planItemId) {
        Query query = new Query();
        query.addCriteria(Criteria.where(ReplayCompareResult.FIELD_RECORD_ID).is(recordId)
                .and(ReplayCompareResult.FIELD_PLAN_ITEM_ID).is(planItemId));
        return mongoTemplate.remove(query, ReplayCompareResultCollection.class).getDeletedCount() > 0;
    }

    public ReplayCompareResult queryCompareResultsById(String objectId) {
        Query query = new Query();
        query.addCriteria(Criteria.where(DASH_ID).is(objectId));
        ReplayCompareResultCollection result = mongoTemplate.findOne(query, ReplayCompareResultCollection.class);
        return ReplayCompareResultConverter.INSTANCE.boFromDao(result);
    }
}