package com.arextest.schedule.dao.mongodb.util;

import org.springframework.data.mongodb.core.query.Update;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MongoHelper {

    private static final String DOT = ".";

    public static Update getUpdate() {
        Update update = new Update();
        update.setOnInsert("dataChangeCreateTime", System.currentTimeMillis());
        update.set("dataChangeUpdateTime", System.currentTimeMillis());
        return update;
    }

    public static void assertNull(String msg, Object... obj) {
        for (Object o : obj) {
            if (o == null) {
                throw new RuntimeException(msg);
            }
        }
    }

    // keys are spliced with "."
    public static String appendDot(String... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            sb.append(key).append(DOT);
        }
        return sb.substring(0, sb.length() - 1);
    }

}