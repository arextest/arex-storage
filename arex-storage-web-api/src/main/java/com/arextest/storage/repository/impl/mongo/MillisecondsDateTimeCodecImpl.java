package com.arextest.storage.repository.impl.mongo;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * The default DateCodec use local zone to create date instance,we direct use long of Milliseconds
 * to store
 *
 * @author jmo
 * @since 2022/1/18
 */
final class MillisecondsDateTimeCodecImpl implements Codec<Long> {

  @Override
  public Long decode(BsonReader reader, DecoderContext decoderContext) {
    return reader.readDateTime();
  }

  @Override
  public void encode(BsonWriter writer, Long value, EncoderContext encoderContext) {
    writer.writeDateTime(value);
  }

  @Override
  public Class<Long> getEncoderClass() {
    return Long.class;
  }
}