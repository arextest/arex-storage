package com.arextest.storage.mock.internal.matchkey.impl;

import com.arextest.model.constants.MockAttributeNames;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.cache.CacheKeyUtils;
import com.arextest.storage.mock.MatchKeyBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(25)
@Slf4j
final class RedisMatchKeyBuilderImpl implements MatchKeyBuilder {

  private final ObjectMapper objectMapper;
  private static final String BODY = "body";
  @Value("${arex.storage.use.eigen.match}")
  private boolean useEigenMatch;
  RedisMatchKeyBuilderImpl(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean isSupported(MockCategoryType categoryType) {
    return Objects.equals(MockCategoryType.REDIS, categoryType);
  }

  @Override
  public List<byte[]> build(Mocker instance) {
    byte[] operationBytes = CacheKeyUtils.toUtf8Bytes(instance.getOperationName());
    Mocker.Target request = instance.getTargetRequest();
    if (request == null || StringUtils.isEmpty(request.getBody())) {
      return Collections.singletonList(operationBytes);
    }
    MessageDigest messageDigest = MessageDigestWriter.getMD5Digest();
    messageDigest.update(operationBytes);
    if (useEigenMatch && MapUtils.isNotEmpty(instance.getEigenMap())) {
      String eigenBody = request.getBody();
      try {
        eigenBody = objectMapper.writeValueAsString(instance.getEigenMap());
      } catch (JsonProcessingException e) {
        LOGGER.error("failed to get http client eigen map, recordId: {}", instance.getRecordId(), e);
      }
      messageDigest.update(CacheKeyUtils.toUtf8Bytes(eigenBody));
    } else {
      byte[] redisKeyBytes = CacheKeyUtils.toUtf8Bytes(request.getBody());
      messageDigest.update(redisKeyBytes);
      messageDigest.update(
          CacheKeyUtils.toUtf8Bytes(request.attributeAsString(MockAttributeNames.CLUSTER_NAME)));
    }
    return Arrays.asList(messageDigest.digest(), operationBytes);
  }

  /**
   * For the type of redis, it is necessary to concatenate request body and clusterName to calculate the feature values
   * @param instance
   * @return
   */
  @Override
  public String getEigenBody(Mocker instance) {
    Object clusterName = instance.getTargetRequest().getAttribute(MockAttributeNames.CLUSTER_NAME);
    ObjectNode objectNode = objectMapper.createObjectNode();
    objectNode.put(BODY, instance.getTargetRequest().getBody());
    if (clusterName != null) {
      objectNode.put(MockAttributeNames.CLUSTER_NAME, clusterName.toString());
    }
    return objectNode.toString();
  }
}