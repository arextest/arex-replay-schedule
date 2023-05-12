package com.arextest.schedule.sender;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: miaolu
 * @create: 2021-12-08
 **/
public class ReplaySenderFactory {

    private static Map<String, ReplaySender> replaySenderMap = new ConcurrentHashMap<>();

    public static void put(String sendType, ReplaySender replaySender) {
        replaySenderMap.put(sendType, replaySender);
    }

    public static ReplaySender get(String sendType) {
        return replaySenderMap.get(sendType);
    }

}