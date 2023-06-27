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
public class ReplayCompareResultRepositoryImpl implements RepositoryWriter<ReplayCompareResultCollection>, RepositoryField  {

    @Resource
    private MongoTemplate mongoTemplate;

    public void insertAllCompareResults(List<ReplayCompareResult> results) {
        List<ReplayCompareResultCollection> pes = results
                .stream().map(ReplayCompareResultConverter.INSTANCE::daoFromBo).collect(Collectors.toList());
        this.save(pes);
    }

    @Override
    public boolean save(List<ReplayCompareResultCollection> itemList) {
        mongoTemplate.insertAll(itemList);
        return true;
    }

    @Override
    public boolean save(ReplayCompareResultCollection item) {
        mongoTemplate.save(item);
        return true;
    }

    public ReplayCompareResult queryCompareResultsById(String objectId) {
        Query query = new Query();
        query.addCriteria(Criteria.where(DASH_ID).is(objectId));
        ReplayCompareResultCollection result = mongoTemplate.findOne(query, ReplayCompareResultCollection.class);
        return ReplayCompareResultConverter.INSTANCE.boFromDao(result);
    }
}