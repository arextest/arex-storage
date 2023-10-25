package com.arextest.config.utils;

import com.arextest.config.model.dao.BaseEntity;
import com.mongodb.client.model.Updates;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MongoHelper {
    public static Bson getUpdate() {
        return Updates.combine(Updates.set(BaseEntity.Fields.dataChangeUpdateTime, System.currentTimeMillis()),
                Updates.setOnInsert(BaseEntity.Fields.dataChangeCreateTime, System.currentTimeMillis()));
    }

    public static Bson getFullProperties(Object obj) {
        List<Bson> updates = new ArrayList<>();
        Map<String, Field> allFields = getAllField(obj);
        for (Field field : allFields.values()) {
            try {
                field.setAccessible(true);
                if (field.get(obj) != null) {
                    updates.add(Updates.set(field.getName(), field.get(obj)));
                }
            } catch (IllegalAccessException e) {
                LOGGER.error(
                        String.format("Class:[%s]. failed to get field %s", obj.getClass().getName(), field.getName()), e);
            }
        }
        return Updates.combine(updates);
    }

    // This method is disabled for fields with the same name in parent and child classes
    public static Bson getSpecifiedProperties(Object obj, String... fieldNames) {
        List<Bson> updates = new ArrayList<>();
        Map<String, Field> allField = getAllField(obj);
        for (String fieldName : fieldNames) {
            try {
                if (allField.containsKey(fieldName)) {
                    Field declaredField = allField.get(fieldName);
                    declaredField.setAccessible(true);
                    Object targetObj = declaredField.get(obj);
                    if (targetObj != null) {
                        updates.add(Updates.set(fieldName, targetObj));
                    }
                }
            } catch (IllegalAccessException e) {
                LOGGER.error(String.format("Class:[%s]. failed to get field %s", obj.getClass().getName(), fieldName),
                        e);
            }
        }
        return Updates.combine(updates);
    }

    private static Map<String, Field> getAllField(Object bean) {
        Class<?> clazz = bean.getClass();
        Map<String, Field> fieldMap = new HashMap<>();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                // ignore static and synthetic field such as $jacocoData
                if (field.isSynthetic()) {
                    continue;
                }
                if (!fieldMap.containsKey(field.getName())) {
                    fieldMap.put(field.getName(), field);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return fieldMap;
    }

    public static void assertNull(String msg, Object... obj) {
        for (Object o : obj) {
            if (o == null) {
                throw new RuntimeException(msg);
            }
        }
    }
}
