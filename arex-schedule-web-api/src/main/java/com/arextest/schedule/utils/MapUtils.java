package com.arextest.schedule.utils;

import java.util.HashMap;
import java.util.Map;

public class MapUtils {
    public static <K, V> Map<K, V> createMap(Object... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid number of arguments. Must be an even number.");
        }

        Map<K, V> map = new HashMap<>();

        for (int i = 0; i < keyValuePairs.length; i += 2) {
            K key = (K) keyValuePairs[i];
            V value = (V) keyValuePairs[i + 1];
            map.put(key, value);
        }

        return map;
    }

    public static boolean isEmpty(Map<?,?> map) {
        return !isNotEmpty(map);
    }

    public static boolean isNotEmpty(Map<?,?> map) {
        return map != null && !map.isEmpty();
    }


}
