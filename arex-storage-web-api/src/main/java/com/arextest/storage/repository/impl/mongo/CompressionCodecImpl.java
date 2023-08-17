package com.arextest.storage.repository.impl.mongo;

import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.util.List;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import com.arextest.common.utils.SerializationUtils;
import com.arextest.desensitization.extension.DataDesensitization;

final class CompressionCodecImpl<T> implements Codec<T> {
    private final Class<T> target;

    private static DataDesensitization desensitization = null;

    static {
        URLClassLoader urlClassLoader = null;
        try {
            urlClassLoader =
                RemoteJarLoader.loadJar("./lib/arex-desensitization-core-0.0.0-SNAPSHOT-jar-with-dependencies.jar");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        List<DataDesensitization> dataDesensitizations =
            RemoteJarLoader.loadService(DataDesensitization.class, urlClassLoader);
        desensitization = dataDesensitizations.get(0);
        try {
            String encrypt = desensitization.encrypt("123");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println();
    }

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