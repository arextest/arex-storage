package com.arextest.storage.repository.impl.mongo;

import com.arextest.common.utils.SerializationUtils;
import com.arextest.extension.desensitization.DataDesensitization;
import com.arextest.extension.desensitization.DefaultDataDesensitization;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

@Slf4j
final class CompressionCodecImpl<T> implements Codec<T> {
    private final Class<T> target;

    private DataDesensitization desensitization = new DefaultDataDesensitization();

    CompressionCodecImpl(Class<T> target) {
        this.target = target;
    }

    CompressionCodecImpl(Class<T> target, DataDesensitization desensitization) {
        this.target = target;
        this.desensitization = desensitization;
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