package com.arextest.storage.mock.internal.matchkey.impl;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.cache.CacheKeyUtils;
import com.arextest.storage.mock.MatchKeyBuilder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

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
}