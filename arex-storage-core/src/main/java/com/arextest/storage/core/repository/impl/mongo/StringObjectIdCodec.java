package com.arextest.storage.core.repository.impl.mongo;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.StringCodec;

/**
 * we should read string compatible with object Id when decode from old version.
 * NOTE: this is temp solution, should be remove,after project stable running.
 *
 * @author jmo
 * @since 2021/11/29
 */
final class StringObjectIdCodec extends StringCodec {

    @Override
    public String decode(BsonReader reader, DecoderContext decoderContext) {
        if (reader.getCurrentBsonType() == BsonType.OBJECT_ID) {
            reader.readObjectId();
            return null;
        }
        return super.decode(reader, decoderContext);
    }
}
