package com.arextest.storage.repository.impl.mongo;

import static com.arextest.diff.utils.JacksonHelperUtil.objectMapper;
import com.arextest.common.utils.SerializationUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * Convert eigenMap to encrypted string for storage
 *
 * @author xinyuan_wang
 * @since 2023/11/28
 */
@Slf4j
final class EigenMapCodecImpl implements Codec<Map> {

  @Override
  public Map decode(BsonReader reader, DecoderContext decoderContext) {
    String encodeWithEncryptString = reader.readString();
    String eigenMapStr = SerializationUtils.useZstdDeserialize(encodeWithEncryptString, String.class);
    if (StringUtils.isEmpty(eigenMapStr)) {
      LOGGER.info("decode eigen map is null. encodeWithEncryptString: {}", encodeWithEncryptString);
    }
    try {
      return objectMapper.readValue(eigenMapStr, new TypeReference<Map<Integer, Long>>() {});
    } catch (JsonProcessingException e) {
      LOGGER.error("failed to decode eigen map, {}, eigenMapStr: {}", e.getMessage(), eigenMapStr, e);
      return null;
    }
  }

  @Override
  public void encode(BsonWriter writer, Map value, EncoderContext encoderContext) {
    String string = null;
    try {
      string = objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      LOGGER.error("failed to encode eigen map. {}", e.getMessage(), e);
      return;
    }
    String base64Result = SerializationUtils.useZstdSerializeToBase64(string);
    writer.writeString(base64Result);
  }

  @Override
  public Class<Map> getEncoderClass() {
    return Map.class;
  }
}