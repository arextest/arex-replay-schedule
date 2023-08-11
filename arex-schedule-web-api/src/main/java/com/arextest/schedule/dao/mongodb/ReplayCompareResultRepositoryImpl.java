package com.arextest.schedule.dao.mongodb;

import com.arextest.schedule.dao.RepositoryWriter;
import com.arextest.schedule.dao.mongodb.util.MongoHelper;
import com.arextest.schedule.model.ReplayCompareResult;
import com.arextest.schedule.model.converter.ReplayCompareResultConverter;
import com.arextest.schedule.model.dao.mongodb.ReplayCompareResultCollection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Component
public class ReplayCompareResultRepositoryImpl implements RepositoryWriter<ReplayCompareResult>, RepositoryField  {
    private static final String PLAN_ID = "planId";
    private static final String PLAN_ITEM_ID = "planItemId";
    private static final String OPERATION_ID = "operationId";
    private static final String CATEGORY_NAME = "categoryName";
    private static final String RECORD_ID = "recordId";

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

    public boolean upsert(List<ReplayCompareResult> itemList) {
        List<ReplayCompareResultCollection> replayCompareResultCollections = itemList.stream()
                .map(ReplayCompareResultConverter.INSTANCE::daoFromBo).collect(Collectors.toList());

        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ReplayCompareResultCollection.class);

        List<Pair<Query, Update>> upserts = new ArrayList<>();
        for (ReplayCompareResultCollection replayCompareResult : replayCompareResultCollections) {
            Query query = new Query();
            query.addCriteria(Criteria.where(PLAN_ID).is(replayCompareResult.getPlanId())
                .and(PLAN_ITEM_ID).is(replayCompareResult.getPlanItemId())
                .and(OPERATION_ID).is(replayCompareResult.getOperationId())
                .and(CATEGORY_NAME).is(replayCompareResult.getCategoryName())
                .and(RECORD_ID).is(replayCompareResult.getRecordId()));

            Update update = MongoHelper.getUpdate();
            MongoHelper.appendFullProperties(update, replayCompareResult);
            upserts.add(Pair.of(query, update));
        }
        bulkOps.upsert(upserts);
        bulkOps.execute();
        return true;
    }

    public ReplayCompareResult queryCompareResultsById(String objectId) {
        Query query = new Query();
        query.addCriteria(Criteria.where(DASH_ID).is(objectId));
        ReplayCompareResultCollection result = mongoTemplate.findOne(query, ReplayCompareResultCollection.class);
        return ReplayCompareResultConverter.INSTANCE.boFromDao(result);
    }
}