package com.arextest.schedule.dao.mongodb.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Update;

@Slf4j
public class MongoHelper {
    public static Update getUpdate() {
        Update update = new Update();
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
}