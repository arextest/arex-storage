package com.arextest.storage.core.repository.impl.mongo;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * @author jmo
 * @since 2021/11/29
 */
final class StringObjectIdCodesProvider implements CodecProvider {
    private final Codec<?> stringObjectIdCodec = new StringObjectIdCodec();

    @SuppressWarnings("unchecked")
    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        return clazz == String.class ? (Codec<T>) stringObjectIdCodec : null;
    }
}
