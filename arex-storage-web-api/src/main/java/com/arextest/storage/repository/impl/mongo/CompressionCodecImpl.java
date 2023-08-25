package com.arextest.storage.repository.impl.mongo;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import com.arextest.common.utils.SerializationUtils;
import com.arextest.extension.desensitization.DataDesensitization;
import com.arextest.storage.beans.GetBeanFromIOC;
import com.arextest.storage.service.DesensitizeService;

final class CompressionCodecImpl<T> implements Codec<T> {
    private final Class<T> target;

    private static DataDesensitization desensitization = null;

    static {
        // DesensitizeService desensitizeService = new DesensitizeService();
        // desensitization = desensitizeService.loadDesensitization();
        DesensitizeService desensitizeService = GetBeanFromIOC.getBean(DesensitizeService.class);
        desensitization = desensitizeService.loadDesensitization();
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
        }
        return SerializationUtils.useZstdDeserialize(encodeWithDecryptString, this.target);

    }

    @Override
    public void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
        String base64Result = SerializationUtils.useZstdSerializeToBase64(value);
        try {
            base64Result = desensitization.encrypt(base64Result);
        } catch (Exception e) {
        }
        writer.writeString(base64Result);
    }

    @Override
    public Class<T> getEncoderClass() {
        return target;
    }
}