package com.arextest.storage.mock.internal.matchkey.impl;

import com.arextest.model.constants.MockAttributeNames;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.cache.CacheKeyUtils;
import com.arextest.storage.mock.MatchKeyBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
final class HttpClientMatchKeyBuilderImpl implements MatchKeyBuilder {

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
        byte[] httpMethodBytes=CacheKeyUtils.toUtf8Bytes(request.attributeAsString(MockAttributeNames.HTTP_METHOD));
        MessageDigest messageDigest =MessageDigestWriter.getMD5Digest();
        messageDigest.update(operationBytes);
        messageDigest.update(queryStringBytes);
        messageDigest.update(httpMethodBytes);
        byte[] httpMethodWithUrlBytes = messageDigest.digest();
        if (StringUtils.isEmpty(request.getBody())) {
           return Arrays.asList(httpMethodWithUrlBytes,operationBytes);
        }
        messageDigest.reset();
        StringReader stringReader = new StringReader(request.getBody());
        OutputStream output = new MessageDigestWriter(messageDigest);
        try {
            IOUtils.copy(stringReader, output, StandardCharsets.UTF_8);
            stringReader.close();
        } catch (IOException e) {
            LOGGER.error("Http Client replay result match key build error:{}", e.getMessage(), e);
        }
        messageDigest.update(httpMethodWithUrlBytes);
        return Arrays.asList(messageDigest.digest(), httpMethodWithUrlBytes,operationBytes);

    }
}