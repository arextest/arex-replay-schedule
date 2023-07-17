package com.arextest.schedule.beans;

import com.arextest.schedule.sender.ReplaySender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: sldu
 * @date: 2023/7/19 11:18
 **/
@Configuration
public class ReplaySenderConfiguration {

    @Bean
    public List<ReplaySender> replaySenderList(List<ReplaySender> replaySenders) {
        // sort by order
        return replaySenders.stream().sorted(Comparator.comparing(ReplaySender::getOrder, Comparator.reverseOrder())).collect(Collectors.toList());
    }
}
