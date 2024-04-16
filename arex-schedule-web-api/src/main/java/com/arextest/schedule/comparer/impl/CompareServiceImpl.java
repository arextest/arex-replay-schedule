package com.arextest.schedule.comparer.impl;

import com.arextest.diff.model.CompareOptions;
import com.arextest.diff.model.CompareResult;
import com.arextest.diff.sdk.CompareSDK;
import com.arextest.schedule.comparer.CompareConfigService;
import com.arextest.schedule.comparer.CompareService;
import com.arextest.web.model.contract.contracts.config.SystemConfigWithProperties;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CompareServiceImpl implements CompareService {

  private CompareConfigService compareConfigService;

  protected CompareSDK compareSDK = new CompareSDK();

  public CompareServiceImpl(CompareConfigService compareConfigService) {
    this.compareConfigService = compareConfigService;
    addGlobalOptionToSDK();
  }


  protected void addGlobalOptionToSDK() {
    SystemConfigWithProperties comparisonSystemConfig = getComparisonSystemConfig();
    configureSDK(comparisonSystemConfig);
  }

  protected SystemConfigWithProperties getComparisonSystemConfig() {
    return compareConfigService.getComparisonSystemConfig();
  }

  protected void configureSDK(SystemConfigWithProperties comparisonSystemConfig) {
    compareSDK.getGlobalOptions()
        .putPluginJarUrl(comparisonSystemConfig.getComparePluginInfo() == null ? null
            : comparisonSystemConfig.getComparePluginInfo().getComparePluginUrl())
        .putNameToLower(comparisonSystemConfig.getCompareNameToLower())
        .putNullEqualsEmpty(comparisonSystemConfig.getCompareNullEqualsEmpty())
        .putIgnoredTimePrecision(comparisonSystemConfig.getCompareIgnoreTimePrecisionMillis())
        .putIgnoreNodeSet(comparisonSystemConfig.getIgnoreNodeSet())
        .putSelectIgnoreCompare(comparisonSystemConfig.getSelectIgnoreCompare())
        .putOnlyCompareCoincidentColumn(comparisonSystemConfig.getOnlyCompareCoincidentColumn())
        .putUuidIgnore(comparisonSystemConfig.getUuidIgnore())
        .putIpIgnore(comparisonSystemConfig.getIpIgnore());
  }

  @Override
  public CompareResult compare(String baseMsg, String testMsg) {
    return compareSDK.compare(baseMsg, testMsg);
  }

  @Override
  public CompareResult compare(String baseMsg, String testMsg, CompareOptions compareOptions) {
    return compareSDK.compare(baseMsg, testMsg, compareOptions);
  }

  @Override
  public CompareResult quickCompare(String baseMsg, String testMsg) {
    return compareSDK.quickCompare(baseMsg, testMsg);
  }

  @Override
  public CompareResult quickCompare(String baseMsg, String testMsg, CompareOptions compareOptions) {
    return compareSDK.quickCompare(baseMsg, testMsg, compareOptions);
  }
}
