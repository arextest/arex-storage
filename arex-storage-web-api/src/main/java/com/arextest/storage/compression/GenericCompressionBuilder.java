//package com.arextest.storage.compression;
//
//import com.arextest.common.utils.CompressionUtils;
//import com.arextest.model.mock.Mocker;
//import com.arextest.storage.model.annotations.FieldCompression;
//import com.arextest.storage.model.mocker.MockItem;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.collections4.CollectionUtils;
//import org.apache.commons.lang3.StringUtils;
//
//import jakarta.validation.constraints.NotNull;
//import java.lang.reflect.Field;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * @author jmo
// * @since 2021/11/8
// */
//@Slf4j
//public final class GenericCompressionBuilder<T extends Mocker> {
//    public static final GenericCompressionBuilder<? extends Mocker> DEFAULT = new GenericCompressionBuilder<>();
//    private static final int DEFAULT_INITIAL_CAPACITY = 2;
//
//    public void compress(T instance, List<Field> fieldList) {
//        if (CollectionUtils.isEmpty(fieldList)) {
//            return;
//        }
//        for (int i = 0; i < fieldList.size(); i++) {
//            Field field = fieldList.get(i);
//            try {
//                String value = (String) field.get(instance);
//                if (StringUtils.isNotEmpty(value)) {
//                    field.set(instance, CompressionUtils.useZstdCompress(value));
//                }
//            } catch (IllegalAccessException e) {
//                LOGGER.error("field: {} compress error:{}", field, e.getMessage(), e);
//            }
//        }
//    }
//
//    public void decompress(T instance, List<Field> fieldList) {
//        if (CollectionUtils.isEmpty(fieldList)) {
//            return;
//        }
//        for (int i = 0; i < fieldList.size(); i++) {
//            Field field = fieldList.get(i);
//            try {
//                String value = (String) field.get(instance);
//
//                field.set(instance, CompressionUtils.useZstdDecompress(value));
//
//            } catch (IllegalAccessException e) {
//                LOGGER.error("field: {} decompress error:{}", field, e.getMessage(), e);
//            }
//        }
//    }
//
//    public List<Field> discoverCompression(@NotNull Class<?> target) {
//        List<Field> fieldList = null;
//        Class<?> current = target;
//        while (current != null) {
//            Field[] fields = current.getDeclaredFields();
//            for (int i = 0; i < fields.length; i++) {
//                Field field = fields[i];
//                if (field.getType() != String.class) {
//                    continue;
//                }
//                if (!field.isAnnotationPresent(FieldCompression.class)) {
//                    continue;
//                }
//                if (fieldList == null) {
//                    fieldList = new ArrayList<>(DEFAULT_INITIAL_CAPACITY);
//                }
//                field.setAccessible(true);
//                fieldList.add(field);
//            }
//            current = current.getSuperclass();
//        }
//        return fieldList;
//    }
//}