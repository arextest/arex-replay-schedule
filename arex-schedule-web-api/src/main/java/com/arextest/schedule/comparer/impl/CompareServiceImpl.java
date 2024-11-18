package com.arextest.schedule.comparer.impl;

import com.arextest.diff.model.CompareOptions;
import com.arextest.diff.model.CompareResult;
import com.arextest.diff.model.script.ScriptContentInfo;
import com.arextest.diff.sdk.CompareSDK;
import com.arextest.schedule.comparer.CompareConfigService;
import com.arextest.schedule.comparer.CompareService;
import com.arextest.web.model.contract.contracts.config.SystemConfigWithProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

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
        .putIpIgnore(comparisonSystemConfig.getIpIgnore())
        .putCompareScript(
            convertToScriptContentInfos(comparisonSystemConfig.getScriptContentInfos()));
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

  public List<ScriptContentInfo> convertToScriptContentInfos(
      List<SystemConfigWithProperties.ScriptContentInfo> scriptContentInfos) {

    if (CollectionUtils.isEmpty(scriptContentInfos)) {
      return null;
    }

    List<ScriptContentInfo> result = new ArrayList<>();
    for (SystemConfigWithProperties.ScriptContentInfo scriptContentInfo : scriptContentInfos) {
      ScriptContentInfo contentInfo = new ScriptContentInfo();
      contentInfo.setFunctionName(scriptContentInfo.getFunctionName());
      contentInfo.setScriptContent(scriptContentInfo.getScriptContent());
      result.add(contentInfo);
    }
    return result;
  }


}
