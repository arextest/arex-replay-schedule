package com.arextest.schedule.service;

/**
 * @author: sldu
 * @date: 2023/3/20 11:39
 **/
public interface RecordVersionUrlProvider {

    /**
     * 环境切换时获取不同URL
     * @param caseSourceType 0 for prod,1 for test
     * @return url
     */
    String getRecordVersionUrl(int caseSourceType);
}
