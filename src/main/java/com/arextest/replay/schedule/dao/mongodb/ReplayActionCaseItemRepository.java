package com.arextest.replay.schedule.dao.mongodb;

import com.arextest.replay.schedule.dao.RepositoryWriter;
import com.arextest.replay.schedule.dao.mongodb.util.MongoHelper;
import com.arextest.replay.schedule.model.CaseSendStatusType;
import com.arextest.replay.schedule.model.ReplayActionCaseItem;
import com.arextest.replay.schedule.model.converter.ReplayRunDetailsConverter;
import com.arextest.replay.schedule.model.dao.mongodb.ReplayRunDetailsCollection;
import com.mongodb.client.result.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rchen9 on 2022/8/19.
 */
@Repository
public class ReplayActionCaseItemRepository implements RepositoryWriter<ReplayActionCaseItem>, RepositoryField {

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public boolean save(ReplayActionCaseItem replayActionCaseItem) {
        ReplayRunDetailsCollection replayRunDetailsCollection = ReplayRunDetailsConverter.INSTANCE.daoFromDto(replayActionCaseItem);
        ReplayRunDetailsCollection insert = mongoTemplate.insert(replayRunDetailsCollection);
        if (insert.getId() != null) {
            replayActionCaseItem.setId(insert.getId());
        }
        return insert.getId() != null;
    }

    public List<ReplayActionCaseItem> waitingSendList(String planItemId, int pageSize) {
        Query query = Query.query(Criteria.where("planItemId").is(planItemId));
        query.addCriteria(Criteria.where("sendStatus").is(CaseSendStatusType.WAIT_HANDLING.getValue()));
        query.limit(pageSize);
        query.with(Sort.by(
                Sort.Order.asc(DASH_ID),
                Sort.Order.asc("replayDependence")
        ));
        List<ReplayRunDetailsCollection> replayRunDetailsCollections = mongoTemplate.find(query, ReplayRunDetailsCollection.class);
        return replayRunDetailsCollections.stream().map(ReplayRunDetailsConverter.INSTANCE::dtoFromDao).collect(Collectors.toList());
    }

    public boolean updateSendResult(ReplayActionCaseItem replayActionCaseItem) {
        Query query = Query.query(Criteria.where(DASH_ID).is(replayActionCaseItem.getId()));
        Update update = MongoHelper.getUpdate();
        MongoHelper.assertNull("update parameter is nul", replayActionCaseItem.getSourceResultId(),
                replayActionCaseItem.getTargetResultId());
        update.set("sendStatus", replayActionCaseItem.getSendStatus());
        update.set("sourceResultId", replayActionCaseItem.getSourceResultId());
        update.set("targetResultId", replayActionCaseItem.getTargetResultId());
        UpdateResult updateResult = mongoTemplate.updateMulti(query, update, ReplayRunDetailsCollection.class);
        return updateResult.getModifiedCount() > 0;
    }

    public boolean updateCompareStatus(String id, int comparedStatus) {
        Query query = Query.query(Criteria.where(DASH_ID).is(id));
        Update update = MongoHelper.getUpdate();
        update.set("compareStatus", comparedStatus);
        UpdateResult updateResult = mongoTemplate.updateMulti(query, update, ReplayRunDetailsCollection.class);
        return updateResult.getModifiedCount() > 0;
    }

    public ReplayActionCaseItem lastOne(String planItemId) {
        Query query = Query.query(Criteria.where("planItemId").is(planItemId));
        query.addCriteria(Criteria.where("sendStatus").is(CaseSendStatusType.WAIT_HANDLING.getValue()));
        query.limit(1);
        query.with(Sort.by(
                Sort.Order.desc(DASH_ID)
        ));
        ReplayRunDetailsCollection replayRunDetailsCollections = mongoTemplate.findOne(query, ReplayRunDetailsCollection.class);
        return ReplayRunDetailsConverter.INSTANCE.dtoFromDao(replayRunDetailsCollections);
    }

}
