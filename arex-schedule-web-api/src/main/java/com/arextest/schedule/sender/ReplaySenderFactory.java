package com.arextest.schedule.sender;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

/**
 * @author: miaolu
 * @create: 2021-12-08
 **/
@Slf4j
public class ReplaySenderFactory {

  private final List<ReplaySender> senders;

  public ReplaySenderFactory(List<ReplaySender> senders) {
    this.senders = senders.stream()
        .sorted(Comparator.comparing(ReplaySender::getOrder, Comparator.reverseOrder()))
        .collect(Collectors.toList());

    LOGGER.info("ReplaySenderFactory init success, senders: {}", this.senders);
  }

  public ReplaySender findReplaySender(String sendType) {
    if (CollectionUtils.isEmpty(senders)) {
      return null;
    }
    for (ReplaySender sender : senders) {
      if (sender.isSupported(sendType)) {
        return sender;
      }
    }
    return null;
  }
}