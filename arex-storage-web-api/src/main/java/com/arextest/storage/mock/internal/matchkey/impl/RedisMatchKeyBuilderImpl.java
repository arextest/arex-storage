package com.arextest.storage.mock.internal.matchkey.impl;

import com.arextest.model.constants.MockAttributeNames;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.cache.CacheKeyUtils;
import com.arextest.storage.mock.MatchKeyBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
@Order(25)
final class RedisMatchKeyBuilderImpl implements MatchKeyBuilder {

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
        byte[] redisKeyBytes = CacheKeyUtils.toUtf8Bytes(request.getBody());
        messageDigest.update(redisKeyBytes);
        messageDigest.update(CacheKeyUtils.toUtf8Bytes(request.attributeAsString(MockAttributeNames.CLUSTER_NAME)));
        return Arrays.asList(messageDigest.digest(), operationBytes);
    }
}