package com.arextest.schedule.dao.mongodb;

import com.arextest.schedule.dao.RepositoryWriter;
import com.arextest.schedule.model.ReplayCompareResult;
import com.arextest.schedule.model.converter.ReplayCompareResultConverter;
import com.arextest.schedule.model.dao.mongodb.ReplayCompareResultCollection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Component
public class ReplayCompareResultRepositoryImpl implements RepositoryWriter<ReplayCompareResultCollection>, RepositoryField  {

    @Resource
    private MongoTemplate mongoTemplate;

    public List<String> insertAllCompareResults(List<ReplayCompareResult> results) {
        List<ReplayCompareResultCollection> pes = results
                .stream().map(ReplayCompareResultConverter.INSTANCE::daoFromDto).collect(Collectors.toList());
        this.save(pes);
        return pes.stream().map(ReplayCompareResultCollection::getId).collect(Collectors.toList());
    }

    @Override
    public boolean save(List<ReplayCompareResultCollection> itemList) {
        mongoTemplate.insertAll(itemList);
        return true;
    }

    @Override
    public boolean save(ReplayCompareResultCollection item) {
        mongoTemplate.insert(item);
        return true;
    }
}
