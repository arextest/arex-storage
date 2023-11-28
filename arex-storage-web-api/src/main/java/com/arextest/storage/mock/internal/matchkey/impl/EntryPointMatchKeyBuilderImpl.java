package com.arextest.storage.mock.internal.matchkey.impl;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.cache.CacheKeyUtils;
import com.arextest.storage.mock.MatchKeyBuilder;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
final class EntryPointMatchKeyBuilderImpl implements MatchKeyBuilder {

  @Override
  public boolean isSupported(MockCategoryType categoryType) {
    return categoryType.isEntryPoint();
  }

  @Override
  public List<byte[]> build(Mocker instance) {
    byte[] operationBytes = CacheKeyUtils.toUtf8Bytes(instance.getOperationName());
    return Collections.singletonList(operationBytes);
  }

  @Override
  public String getEigenBody(Mocker instance) {
    return instance.getTargetRequest().getBody();
  }
}