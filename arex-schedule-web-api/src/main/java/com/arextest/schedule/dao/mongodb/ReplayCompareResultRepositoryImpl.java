package com.arextest.schedule.dao.mongodb;

import com.arextest.schedule.dao.RepositoryWriter;
import com.arextest.schedule.model.ReplayCompareResult;
import com.arextest.schedule.model.converter.ReplayCompareResultConverter;
import com.arextest.schedule.model.dao.mongodb.ReplayCompareResultCollection;
import com.arextest.schedule.model.storage.CompareResultDbAggStruct;
import com.arextest.schedule.model.storage.ResultCodeGroup;
import com.mongodb.BasicDBObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
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
        query.addCriteria(Criteria.where(ReplayCompareResult.FIELD_RECORD_ID).is(recordId)
                .and(ReplayCompareResult.FIELD_PLAN_ITEM_ID).is(planItemId));
        return mongoTemplate.remove(query, ReplayCompareResultCollection.class).getDeletedCount() > 0;
    }

    public ReplayCompareResult queryCompareResultsById(String objectId) {
        Query query = new Query();
        query.addCriteria(Criteria.where(DASH_ID).is(objectId));
        ReplayCompareResultCollection result = mongoTemplate.findOne(query, ReplayCompareResultCollection.class);
        return replayCompareResultConverter.boFromDao(result);
    }

    public List<CompareResultDbAggStruct> calculateResultCodeGroup(String planId) {
        MatchOperation match = Aggregation.match(Criteria.where(ReplayCompareResult.FIELD_PLAN_ID).is(planId));

        GroupOperation group = Aggregation.group(ReplayCompareResult.FIELD_DIFF_RESULT_CODE, ReplayCompareResult.FIELD_CATEGORY_NAME)
                .first(ReplayCompareResult.FIELD_DIFF_RESULT_CODE).as(ReplayCompareResult.FIELD_DIFF_RESULT_CODE)
                .first(ReplayCompareResult.FIELD_CATEGORY_NAME).as(ReplayCompareResult.FIELD_CATEGORY_NAME)
                .addToSet(new BasicDBObject("recordId", "$recordId").append("targetId", "$replayId")).as(CompareResultDbAggStruct.FIELD_RELATED_IDS);
        Aggregation aggregation = Aggregation.newAggregation(match, group);
        return mongoTemplate.aggregate(aggregation, mongoTemplate.getCollectionName(ReplayCompareResultCollection.class), CompareResultDbAggStruct.class).getMappedResults();
    }
}