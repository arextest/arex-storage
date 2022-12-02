package com.arextest.storage.repository.impl.mongo;

import com.arextest.common.utils.SerializationUtils;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

final class CompressionCodecImpl<T> implements Codec<T> {
    private final Class<T> target;

    CompressionCodecImpl(Class<T> target) {
        this.target = target;
    }

    @Override
    public T decode(BsonReader reader, DecoderContext decoderContext) {
        return SerializationUtils.useZstdDeserialize(reader.readString(), this.target);

    }

    @Override
    public void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
        String base64Result = SerializationUtils.useZstdSerializeToBase64(value);
        writer.writeString(base64Result);
    }

    @Override
    public Class<T> getEncoderClass() {
        return target;
    }
}