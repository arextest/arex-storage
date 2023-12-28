package com.arextest.storage.mock.internal.matchkey.impl;

import static com.arextest.diff.utils.JacksonHelperUtil.objectMapper;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.model.mock.Mocker.Target;
import com.arextest.storage.cache.CacheKeyUtils;
import com.arextest.storage.mock.MatchKeyBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
@Slf4j
final class DynamicClassMatchKeyBuilderImpl implements MatchKeyBuilder {

  @Override
  public boolean isSupported(MockCategoryType categoryType) {
    return Objects.equals(categoryType, MockCategoryType.DYNAMIC_CLASS);
  }

  @Override
  public List<byte[]> build(Mocker instance) {
    byte[] operationNameBytes = CacheKeyUtils.toUtf8Bytes(instance.getOperationName());
    Target targetRequest = instance.getTargetRequest();
    if (targetRequest == null || StringUtils.isEmpty(targetRequest.getBody())) {
      return Collections.singletonList(operationNameBytes);
    }

    MessageDigest messageDigest = MessageDigestWriter.getMD5Digest();
    messageDigest.update(operationNameBytes);

    String body = targetRequest.getBody();
    if (MapUtils.isNotEmpty(instance.getEigenMap())) {
      try {
        body = objectMapper.writeValueAsString(instance.getEigenMap());
      } catch (JsonProcessingException e) {
        LOGGER.error("failed to get dynamic class eigen map, recordId: {}", instance.getRecordId(), e);
      }
    }
    messageDigest.update(CacheKeyUtils.toUtf8Bytes(body));
    return Arrays.asList(messageDigest.digest(), operationNameBytes);
  }

  @Override
  public String getEigenBody(Mocker instance) {
    return instance.getTargetRequest().getBody();
  }
}