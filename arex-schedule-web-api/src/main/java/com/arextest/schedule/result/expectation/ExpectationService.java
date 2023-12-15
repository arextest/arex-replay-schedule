package com.arextest.schedule.result.expectation;

import com.arextest.schedule.dao.mongodb.ExpectationRepository;
import com.arextest.schedule.model.converter.ExpectationMapper;
import com.arextest.schedule.model.dao.mongodb.ExpectationResultCollection;
import com.arextest.schedule.model.expectation.ExpectationResultModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ExpectationService {

    @Resource
    private ExpectationRepository expectationRepository;

    public void save(String caseId, String category, String operation, String path, String message,
        String originalText, boolean result) {
        ExpectationResultCollection collection = new ExpectationResultCollection();
        collection.setCaseId(caseId);
        collection.setCategory(category);
        collection.setOperation(operation);
        collection.setPath(path);
        collection.setMessage(message);
        collection.setResult(result);
        collection.setAssertionText(originalText);
        collection.setDataChangeCreateTime(System.currentTimeMillis());

        try {
            expectationRepository.save(collection);
        } catch (Exception e) {
            LOGGER.error("Failed to save expectation result", e);
        }
    }

    public Collection<ExpectationResultModel> query(String caseId) {
        List<ExpectationResultCollection> collectionList = expectationRepository.query(caseId);
        if (collectionList == null) {
            return Collections.emptyList();
        }
        return CollectionUtils.collect(collectionList, ExpectationMapper.INSTANCE::toModel,
            new ArrayList<>(collectionList.size()));
    }

}
