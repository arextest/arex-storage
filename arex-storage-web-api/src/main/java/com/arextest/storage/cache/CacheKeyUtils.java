package com.arextest.storage.cache;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.storage.model.MockResultType;

import javax.validation.constraints.NotNull;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;


public final class CacheKeyUtils {

  public static final byte[] EMPTY_BYTE = new byte[]{};

  private CacheKeyUtils() {

  }

  public static byte[] toUtf8Bytes(String value) {
    return value == null ? EMPTY_BYTE : value.getBytes(StandardCharsets.UTF_8);
  }

  public static String fromUtf8Bytes(byte[] value) {
    return value == null ? null : new String(value, StandardCharsets.UTF_8);
  }

  private static byte[] categoryBytes(MockResultType resultType, MockCategoryType category) {
    return toUtf8Bytes(category.getName() + resultType.getCodeValue());
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

  public static byte[] buildRecordOperationKey(MockCategoryType category, String recordId,
      String operation) {
    return buildSourceKey(MockResultType.RECORD_WITH_OPERATION, category,
        toUtf8Bytes(recordId + operation));
  }

  public static byte[] buildConsumeKey(MockCategoryType category, byte[] recordIdBytes,
      byte[] replayIdBytes,
      byte[] mockKeyBytes) {
    byte[] value = categoryBytes(MockResultType.CONSUME_RESULT, category);
    int capacity = recordIdBytes.length + mockKeyBytes.length + value.length + replayIdBytes.length;
    return ByteBuffer.allocate(capacity)
        .put(recordIdBytes)
        .put(mockKeyBytes)
        .put(value)
        .put(replayIdBytes)
        .array();
  }

  public static byte[] buildRecordKey(MockCategoryType category, byte[] recordIdBytes,
      byte[] mockKeyBytes) {
    return buildSourceKey(MockResultType.RECORD_RESULT, category, recordIdBytes,
        mockKeyBytes);
  }

  public static byte[] buildSourceKey(MockResultType resultType, MockCategoryType category,
      byte[] id) {
    byte[] value = categoryBytes(resultType, category);
    return ByteBuffer.allocate(id.length + value.length)
        .put(id)
        .put(value)
        .array();
  }

  public static byte[] buildSourceKey(MockResultType resultType, MockCategoryType category,
      byte[] id,
      byte[] mockKey) {
    byte[] value = categoryBytes(resultType, category);
    int capacity = value.length + id.length + mockKey.length;
    return ByteBuffer.allocate(capacity)
        .put(id)
        .put(mockKey)
        .put(value)
        .array();
  }

  public static byte[] buildMatchedRecordInstanceIdsKey(MockCategoryType category,
      byte[] recordIdBytes, byte[] replayIdBytes, byte[] operationNameBytes) {
    byte[] value = categoryBytes(MockResultType.RECORD_INSTANCE_ID_HAS_BEEN_MATCHED, category);
    int capacity =
        recordIdBytes.length + operationNameBytes.length + value.length + replayIdBytes.length;
    return ByteBuffer.allocate(capacity)
        .put(recordIdBytes)
        .put(operationNameBytes)
        .put(value)
        .put(replayIdBytes)
        .array();
  }

  public static byte[] merge(@NotNull byte[] src, int value) {
    return ByteBuffer.allocate(src.length + Integer.SIZE)
        .put(src)
        .putInt(value)
        .array();
  }
}