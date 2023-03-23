package com.arextest.storage.mock.internal.matchkey.impl;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.cache.CacheKeyUtils;
import com.arextest.storage.mock.MatchKeyBuilder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
@Order(10)
final class DynamicClassMatchKeyBuilderImpl implements MatchKeyBuilder {

    @Override
    public boolean isSupported(MockCategoryType categoryType) {
        return Objects.equals(categoryType, MockCategoryType.DYNAMIC_CLASS);
    }

    @Override
    public List<byte[]> build(Mocker instance) {
        MessageDigest messageDigest =MessageDigestWriter.getMD5Digest();
        byte[] operationNameBytes = CacheKeyUtils.toUtf8Bytes(instance.getOperationName());
        messageDigest.update(operationNameBytes);
        messageDigest.update(CacheKeyUtils.toUtf8Bytes(instance.getTargetRequest().getBody()));
        return Arrays.asList(messageDigest.digest(), operationNameBytes);
    }
}