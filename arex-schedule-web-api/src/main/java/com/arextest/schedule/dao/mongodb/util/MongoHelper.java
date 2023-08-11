package com.arextest.schedule.dao.mongodb.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Update;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

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

    public static void appendFullProperties(Update update, Object obj) {
        Map<String, Field> allFields = getAllField(obj);
        for (Field field : allFields.values()) {
            try {
                field.setAccessible(true);
                if (field.get(obj) != null) {
                    update.set(field.getName(), field.get(obj));
                }
            } catch (IllegalAccessException e) {
                LOGGER.error(String.format("Class:[%s]. failed to get field %s", obj.getClass().getName(), field.getName()), e);
            }
        }
    }

    private static Map<String, Field> getAllField(Object bean) {
        Class<?> clazz = bean.getClass();
        Map<String, Field> fieldMap = new HashMap<>();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!fieldMap.containsKey(field.getName())) {
                    fieldMap.put(field.getName(), field);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return fieldMap;
    }

}