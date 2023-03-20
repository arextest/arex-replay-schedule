package com.arextest.schedule.service;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author: sldu
 * @date: 2023/3/20 11:38
 **/
public class ReportRecordVersionService {
    private final List<RecordVersionUrlProvider> recordVersionUrlProviderList;

    public ReportRecordVersionService(List<RecordVersionUrlProvider> recordVersionUrlProviderList) {
        this.recordVersionUrlProviderList = recordVersionUrlProviderList;
    }

    public String getRecordVersionUrl(int caseSourceType) {
        if (CollectionUtils.isEmpty(recordVersionUrlProviderList)) {
            return StringUtils.EMPTY;
        }
        for (RecordVersionUrlProvider recordVersionUrlProvider : recordVersionUrlProviderList) {
            return recordVersionUrlProvider.getRecordVersionUrl(caseSourceType);
        }
        return StringUtils.EMPTY;
    }
}
