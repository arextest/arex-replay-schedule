package com.arextest.schedule.comparer.impl;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.replay.CompareRelationResult;
import com.arextest.schedule.comparer.CompareItem;
import com.arextest.schedule.comparer.converter.CompareItemConvertFactory;
import com.arextest.schedule.comparer.converter.CompareItemConverter;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * @author jmo
 * @since 2021/11/23
 */
@Component
public class PrepareCompareItemBuilder {

  @Resource
  private CompareItemConvertFactory factory;

  CompareItem build(AREXMocker mocker) {
    CompareItemConverter converter = factory.getConvert(mocker.getCategoryType().getName());
    return converter.convert(mocker);
  }

  public CompareItem build(CompareRelationResult result, boolean recordCompareItem) {
    CompareItemConverter converter = factory.getConvert(result.getCategoryType().getName());
    return converter.convert(result, recordCompareItem);
  }

  public final static class CompareItemImpl implements CompareItem {
    private final String compareMessage;
    private final String compareOperation;
    private final String compareService;
    private final String compareKey;
    private final long createTime;
    private final boolean entryPointCategory;

    public CompareItemImpl(String compareOperation, String compareMessage, String compareKey,
        long createTime, boolean entryPointCategory) {
      this(compareOperation, compareMessage, null, compareKey, createTime, entryPointCategory);
    }

    public CompareItemImpl(CompareRelationResult result, boolean recordCompareItem) {
      this.compareOperation = result.getOperationName();
      this.compareService = null;
      this.compareKey = null;
      this.entryPointCategory = result.getCategoryType().isEntryPoint();
      this.compareMessage = recordCompareItem ? result.getRecordMessage() : result.getReplayMessage();
      this.createTime = recordCompareItem ? result.getRecordTime() : result.getReplayTime();
    }

    private CompareItemImpl(String compareOperation, String compareMessage, String compareService,
        String compareKey,
        long createTime, boolean entryPointCategory) {
      this.compareMessage = compareMessage;
      this.compareOperation = compareOperation;
      this.compareService = compareService;
      this.compareKey = compareKey;
      this.createTime = createTime;
      this.entryPointCategory = entryPointCategory;
    }

    @Override
    public String getCompareContent() {
      return compareMessage;
    }

    @Override
    public String getCompareOperation() {
      return compareOperation;
    }

    @Override
    public String getCompareService() {
      return compareService;
    }

    @Override
    public String getCompareKey() {
      return compareKey;
    }

    @Override
    public long getCreateTime() {
      return createTime;
    }

    @Override
    public boolean isEntryPointCategory() {
      return entryPointCategory;
    }
  }
}