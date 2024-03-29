package com.arextest.schedule.sender;

import java.util.List;
import javax.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

/**
 * @author: miaolu
 * @create: 2021-12-08
 **/
@Component
public class ReplaySenderFactory {

  @Resource
  private List<ReplaySender> replaySenderList;

  public ReplaySender findReplaySender(String sendType) {
    if (CollectionUtils.isEmpty(replaySenderList)) {
      return null;
    }
    for (ReplaySender sender : replaySenderList) {
      if (sender.isSupported(sendType)) {
        return sender;
      }
    }
    return null;
  }
}