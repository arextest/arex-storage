package com.arextest.storage.repository.impl.mongo;

import lombok.extern.slf4j.Slf4j;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import com.arextest.common.utils.SerializationUtils;
import com.arextest.extension.desensitization.DataDesensitization;
import com.arextest.storage.beans.GetBeanFromIOC;
import com.arextest.storage.service.DesensitizeService;

@Slf4j
final class CompressionCodecImpl<T> implements Codec<T> {
    private final Class<T> target;

    private static DataDesensitization desensitization = null;

    static {
        DesensitizeService desensitizeService = GetBeanFromIOC.getBean(DesensitizeService.class);
        String remoteJarUrl = desensitizeService.getRemoteJarUrl();
        desensitization = desensitizeService.loadDesensitization(remoteJarUrl);
    }

    CompressionCodecImpl(Class<T> target) {
        this.target = target;
    }

    @Override
    public T decode(BsonReader reader, DecoderContext decoderContext) {
        String encodeWithEncryptString = reader.readString();
        String encodeWithDecryptString = null;
        try {
            encodeWithDecryptString = desensitization.decrypt(encodeWithEncryptString);
        } catch (Exception e) {
            LOGGER.error("Data decrypt failed", e);
        }
        return SerializationUtils.useZstdDeserialize(encodeWithDecryptString, this.target);

    }

    @Override
    public void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
        String base64Result = SerializationUtils.useZstdSerializeToBase64(value);
        try {
            base64Result = desensitization.encrypt(base64Result);
        } catch (Exception e) {
            LOGGER.error("Data encrypt failed", e);
        }
        writer.writeString(base64Result);
    }

    @Override
    public Class<T> getEncoderClass() {
        return target;
    }
}