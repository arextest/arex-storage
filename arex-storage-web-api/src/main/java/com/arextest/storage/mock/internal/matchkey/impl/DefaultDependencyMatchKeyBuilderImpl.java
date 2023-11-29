package com.arextest.storage.mock.internal.matchkey.impl;

import static com.arextest.diff.utils.JacksonHelperUtil.objectMapper;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.cache.CacheKeyUtils;
import com.arextest.storage.mock.MatchKeyBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Order()
final class DefaultDependencyMatchKeyBuilderImpl implements MatchKeyBuilder {

  @Value("${arex.storage.use.eigen.match}")
  private boolean useEigenMatch;

  @Override
  public boolean isSupported(MockCategoryType categoryType) {
    return !categoryType.isEntryPoint() && !categoryType.isSkipComparison();
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
    String body = request.getBody();
    if (useEigenMatch && MapUtils.isNotEmpty(instance.getEigenMap())) {
      try {
        body = objectMapper.writeValueAsString(instance.getEigenMap());
      } catch (JsonProcessingException e) {
        LOGGER.error("failed to get default dependency eigen map, recordId: {}", instance.getRecordId(), e);
      }
    }
    StringReader stringReader = new StringReader(body);
    OutputStream output = new MessageDigestWriter(messageDigest);
    try {
      IOUtils.copy(stringReader, output, StandardCharsets.UTF_8);
      stringReader.close();
    } catch (IOException e) {
      LOGGER.error("Unknown dependency replay result match key build error:{}", e.getMessage(), e);
    }
    return Arrays.asList(messageDigest.digest(), operationBytes);
  }

  @Override
  public String getEigenBody(Mocker instance) {
    return instance.getTargetRequest().getBody();
  }
}