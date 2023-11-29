package com.arextest.storage.mock.internal.matchkey.impl;

import com.arextest.model.constants.MockAttributeNames;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.cache.CacheKeyUtils;
import com.arextest.storage.mock.MatchKeyBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Order(15)
final class HttpClientMatchKeyBuilderImpl implements MatchKeyBuilder {

  @Value("${arex.storage.use.eigen.match}")
  private boolean useEigenMatch;

  private final ObjectMapper objectMapper;
  private static final String BODY = "body";
  HttpClientMatchKeyBuilderImpl(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean isSupported(MockCategoryType categoryType) {
    return Objects.equals(MockCategoryType.HTTP_CLIENT, categoryType);
  }

  @Override
  public List<byte[]> build(Mocker instance) {
    byte[] operationBytes = CacheKeyUtils.toUtf8Bytes(instance.getOperationName());
    Mocker.Target request = instance.getTargetRequest();
    if (request == null) {
      return Collections.singletonList(operationBytes);
    }
    byte[] queryStringBytes =
        CacheKeyUtils.toUtf8Bytes(request.attributeAsString(MockAttributeNames.HTTP_QUERY_STRING));
    byte[] httpMethodBytes = CacheKeyUtils.toUtf8Bytes(
        request.attributeAsString(MockAttributeNames.HTTP_METHOD));
    MessageDigest messageDigest = MessageDigestWriter.getMD5Digest();
    messageDigest.update(operationBytes);
    messageDigest.update(queryStringBytes);
    messageDigest.update(httpMethodBytes);
    byte[] httpMethodWithUrlBytes = messageDigest.digest();
    if (StringUtils.isEmpty(request.getBody())) {
      return Arrays.asList(httpMethodWithUrlBytes, operationBytes);
    }
    String body = request.getBody();
    if (useEigenMatch && MapUtils.isNotEmpty(instance.getEigenMap())) {
      try {
        body = objectMapper.writeValueAsString(instance.getEigenMap());
      } catch (JsonProcessingException e) {
        LOGGER.error("failed to get http client eigen map, recordId: {}", instance.getRecordId(), e);
      }
    }
    StringReader stringReader = new StringReader(body);
    OutputStream output = new MessageDigestWriter(messageDigest);
    try {
      IOUtils.copy(stringReader, output, StandardCharsets.UTF_8);
      stringReader.close();
    } catch (IOException e) {
      LOGGER.error("Http Client replay result match key build error:{}", e.getMessage(), e);
    }
    messageDigest.update(httpMethodWithUrlBytes);
    return Arrays.asList(messageDigest.digest(), httpMethodWithUrlBytes, operationBytes);

  }

  /**
   * For the type of httpClient,
   * it is necessary to concatenate queryString, method and requestBody to calculate the eigen values
   * @param instance
   * @return
   */
  @Override
  public String getEigenBody(Mocker instance) {
    Object queryString = instance.getTargetRequest().getAttribute(MockAttributeNames.HTTP_QUERY_STRING);
    Object method = instance.getTargetRequest().getAttribute(MockAttributeNames.HTTP_METHOD);
    ObjectNode objectNode = objectMapper.createObjectNode();
    objectNode.put(BODY, instance.getTargetRequest().getBody());
    if (queryString != null) {
      objectNode.put(MockAttributeNames.HTTP_QUERY_STRING, queryString.toString());
    }
    if (method != null) {
      objectNode.put(MockAttributeNames.HTTP_METHOD, method.toString());
    }
    return objectNode.toString();
  }
}