package com.arextest.replay.schedule.utils;


import com.arextest.replay.schedule.model.config.CompareConfigDetail;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wang_yc on 2021/9/16
 */
@Slf4j
public class FormatUtil {
    public static List<String> toIgnoreList(List<CompareConfigDetail> diffPaths) {

        List<String> list = new ArrayList<>(diffPaths.size());
        for (CompareConfigDetail diffPath : diffPaths) {
            list.add(diffPath.getPathValue().get(0));
        }
        return list;
    }


    public static Map<String, String> toSortKeys(List<CompareConfigDetail> configList) {
        Map<String, String> listKeyList = new HashMap<>(configList.size());
        for (CompareConfigDetail c : configList) {
            String key = c.getPathName();
            listKeyList.put(key, String.join(",", c.getPathValue()));
        }

        return listKeyList;
    }

    public static Map<String, String> toReferenceMap(List<CompareConfigDetail> configList) {
        Map<String, String> referenceList = new HashMap<>(configList.size());
        for (CompareConfigDetail c : configList) {
            referenceList.put(c.getPathName(), c.getPathValue().get(0));
        }
        return referenceList;
    }

    public static Map<String, List<String>> toDecompressMap(List<CompareConfigDetail> configList) {
        Map<String, List<String>> decompressMap = new HashMap<>(configList.size());
        for (CompareConfigDetail c : configList) {
            List<String> pathValues = decompressMap.computeIfAbsent(c.getPathValue().get(0),
                    (key -> new ArrayList<>()
                    ));
            pathValues.add(c.getPathName());
        }
        return decompressMap;
    }
}
