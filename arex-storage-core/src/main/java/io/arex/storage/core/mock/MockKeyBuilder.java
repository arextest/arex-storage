package io.arex.storage.core.mock;

import io.arex.storage.core.cache.CacheKeyUtils;
import io.arex.storage.model.mocker.MainEntry;
import io.arex.storage.model.mocker.impl.ABtMocker;
import io.arex.storage.model.mocker.impl.DalResultMocker;
import io.arex.storage.model.mocker.impl.DynamicResultMocker;
import io.arex.storage.model.mocker.impl.MessageMocker;
import io.arex.storage.model.mocker.impl.RedisMocker;
import io.arex.storage.model.mocker.impl.ServiceMocker;
import io.arex.storage.model.mocker.impl.DatabaseMocker;
import io.arex.storage.model.mocker.impl.HttpClientMocker;
import io.arex.storage.model.mocker.impl.ServletMocker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author jmo
 * @since 2021/11/25
 */
@Slf4j
@Component
public final class MockKeyBuilder {
    @Resource
    private DbMockKeyBuilder dbMockKeyBuilder;
    private static final String MD5_ALGORITHM_NAME = "MD5";
    static final int MAX_MOCK_KEY_CAPACITY = 3;

    public List<byte[]> build(@NotNull Object instance) {
        if (instance instanceof ServiceMocker) {
            return serviceMockKeyBuild((ServiceMocker) instance);
        }
        if (instance instanceof MessageMocker) {
            return messageMockKeyBuild((MessageMocker) instance);
        }
        if (instance instanceof DynamicResultMocker) {
            return dynamicMockKeyBuild((DynamicResultMocker) instance);
        }
        if (instance instanceof DalResultMocker) {
            return dbMockKeyBuilder.dalMockKeyBuild((DalResultMocker) instance);
        }
        if (instance instanceof ABtMocker) {
            return abMockKeyBuild((ABtMocker) instance);
        }
        if (instance instanceof RedisMocker) {
            return redisMockKeyBuild((RedisMocker) instance);
        }
        if (instance instanceof ServletMocker) {
            return servletMockKeyBuild((ServletMocker) instance);
        }
        if (instance instanceof HttpClientMocker) {
            return httpClientMockKeyBuild((HttpClientMocker) instance);
        }
        if (instance instanceof DatabaseMocker) {
            return dataBaseMockKeyBuild((DatabaseMocker) instance);
        }
        throw new UnsupportedOperationException(instance.getClass().getName());
    }

    private List<byte[]> dataBaseMockKeyBuild(DatabaseMocker instance) {
        return dbMockKeyBuilder.dataBaseMockKeyBuild(instance);
    }

    private List<byte[]> httpClientMockKeyBuild(HttpClientMocker instance) {
        return Collections.singletonList(CacheKeyUtils.toUtf8Bytes(instance.getUrl()));
    }

    private List<byte[]> servletMockKeyBuild(ServletMocker instance) {
        byte[] serviceOperationBytes = CacheKeyUtils.toUtf8Bytes(instance.getPath() + instance.getMethod());
        if (instance instanceof MainEntry || StringUtils.isEmpty(instance.getRequest())) {
            return Collections.singletonList(serviceOperationBytes);
        }
        // The request should be large use streaming to md5
        List<byte[]> keys = new ArrayList<>(MAX_MOCK_KEY_CAPACITY);
        MessageDigest messageDigest = getMD5Digest();
        messageDigest.update(serviceOperationBytes);
        StringReader stringReader = new StringReader(instance.getRequest());
        OutputStream output = new MessageDigestWriter(messageDigest);
        try {
            IOUtils.copy(stringReader, output, StandardCharsets.UTF_8);
            stringReader.close();
        } catch (IOException e) {
            LOGGER.error("serviceMockKeyBuild error:{}", e.getMessage(), e);
        }
        byte[] fullRequestHashValue = messageDigest.digest();
        keys.add(fullRequestHashValue);
        keys.add(serviceOperationBytes);
        return keys;
    }

    private List<byte[]> abMockKeyBuild(ABtMocker instance) {
        return Collections.singletonList(CacheKeyUtils.toUtf8Bytes(instance.getExpCode()));
    }

    private List<byte[]> dynamicMockKeyBuild(DynamicResultMocker instance) {
        return Collections.singletonList(CacheKeyUtils.toUtf8Bytes(instance.getClazzName() + instance.getOperation() + instance.getOperationKey()));
    }

    private List<byte[]> serviceMockKeyBuild(ServiceMocker instance) {
        byte[] serviceOperationBytes = CacheKeyUtils.toUtf8Bytes(instance.getService() + instance.getOperation());
        if (instance instanceof MainEntry || StringUtils.isEmpty(instance.getRequest())) {
            return Collections.singletonList(serviceOperationBytes);
        }
        // The request should be large use streaming to md5
        List<byte[]> keys = new ArrayList<>(MAX_MOCK_KEY_CAPACITY);
        MessageDigest messageDigest = getMD5Digest();
        messageDigest.update(serviceOperationBytes);
        StringReader stringReader = new StringReader(instance.getRequest());
        OutputStream output = new MessageDigestWriter(messageDigest);
        try {
            IOUtils.copy(stringReader, output, StandardCharsets.UTF_8);
            stringReader.close();
        } catch (IOException e) {
            LOGGER.error("serviceMockKeyBuild error:{}", e.getMessage(), e);
        }
        byte[] fullRequestHashValue = messageDigest.digest();
        keys.add(fullRequestHashValue);
        keys.add(serviceOperationBytes);
        return keys;
    }

    private final static class MessageDigestWriter extends OutputStream {

        private final MessageDigest messageDigest;

        private MessageDigestWriter(MessageDigest messageDigest) {
            this.messageDigest = messageDigest;
        }

        @Override
        public void write(int b) {
            messageDigest.update((byte) b);
        }
    }

    private List<byte[]> messageMockKeyBuild(MessageMocker instance) {
        return Collections.singletonList(CacheKeyUtils.toUtf8Bytes(instance.getSubject()));
    }

    private List<byte[]> redisMockKeyBuild(RedisMocker instance) {
        return Collections.singletonList(CacheKeyUtils.toUtf8Bytes(instance.getClusterName() + instance.getRedisKey()));
    }

    static MessageDigest getMD5Digest() {
        try {
            return MessageDigest.getInstance(MD5_ALGORITHM_NAME);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Could not find MessageDigest with algorithm \"" + MD5_ALGORITHM_NAME +
                    "\"", exception);
        }
    }
}
