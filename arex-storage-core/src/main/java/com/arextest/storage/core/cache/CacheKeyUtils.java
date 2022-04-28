package com.arextest.storage.core.cache;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.enums.MockResultType;

import javax.validation.constraints.NotNull;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author jmo
 * @since 2021/11/17
 */
public final class CacheKeyUtils {
    private CacheKeyUtils() {

    }

    public static byte[] toUtf8Bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    public static String fromUtf8Bytes(byte[] value) {
        return value == null ? null : new String(value, StandardCharsets.UTF_8);
    }

    private static int mergeToInt(MockResultType resultType, MockCategoryType category) {
        int value = 1 << resultType.getCodeValue();
        value = value << Short.SIZE;
        value += category.getCodeValue();
        return value;
    }

    public static byte[] buildReplayKey(MockCategoryType category, String replayResultId) {
        return buildSourceKey(MockResultType.REPLAY_RESULT, category, toUtf8Bytes(replayResultId));
    }

    public static byte[] buildRecordKey(MockCategoryType category, String recordId) {
        return buildSourceKey(MockResultType.RECORD_RESULT, category, toUtf8Bytes(recordId));
    }

    public static byte[] buildRecordKey(MockCategoryType category, byte[] recordIdBytes) {
        return buildSourceKey(MockResultType.RECORD_RESULT, category, recordIdBytes);
    }

    public static byte[] buildConsumeKey(MockCategoryType category, byte[] recordIdBytes, byte[] replayIdBytes,
                                         byte[] mockKeyBytes) {
        int value = mergeToInt(MockResultType.CONSUME_RESULT, category);
        int capacity = recordIdBytes.length + mockKeyBytes.length + Integer.SIZE + replayIdBytes.length;
        return ByteBuffer.allocate(capacity)
                .put(recordIdBytes)
                .put(mockKeyBytes)
                .putInt(value)
                .put(replayIdBytes)
                .array();
    }

    public static byte[] buildRecordKey(MockCategoryType category, byte[] recordIdBytes,
                                        byte[] mockKeyBytes) {
        return buildSourceKey(MockResultType.RECORD_RESULT, category, recordIdBytes,
                mockKeyBytes);
    }

    public static byte[] buildSourceKey(MockResultType resultType, MockCategoryType category, byte[] id) {
        int value = mergeToInt(resultType, category);
        return merge(id, value);
    }

    public static byte[] buildSourceKey(MockResultType resultType, MockCategoryType category, byte[] id,
                                        byte[] mockKey) {
        int value = mergeToInt(resultType, category);
        int capacity = id.length + mockKey.length + Integer.SIZE;
        return ByteBuffer.allocate(capacity)
                .put(id)
                .put(mockKey)
                .putInt(value)
                .array();
    }

    public static byte[] merge(@NotNull String src, @NotNull MockCategoryType category) {
        return merge(src, category.getCodeValue());
    }

    public static byte[] merge(@NotNull String src, int value) {
        return merge(toUtf8Bytes(src), value);
    }


    public static byte[] merge(@NotNull byte[] src, int value) {
        return ByteBuffer.allocate(src.length + Integer.SIZE)
                .put(src)
                .putInt(value)
                .array();
    }
}
