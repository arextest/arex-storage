package com.arextest.storage.repository.impl.mongo.converters;

import static com.arextest.diff.utils.JacksonHelperUtil.objectMapper;

import com.arextest.common.utils.SerializationUtils;
import com.arextest.model.mock.Mocker.Target;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

/**
 * Refactor from the original codec, logic is remained the same
 * Convert eigenMap to encrypted string for storage
 *
 * @author xinyuan_wang
 * @since 2023/11/28
 */
@Slf4j
public class ArexEigenCompressionConverter {

  private static Map<Integer, Long> read(String source)  {
    String eigenMapStr = SerializationUtils.useZstdDeserialize(source, String.class);
    try {
      return objectMapper.readValue(eigenMapStr, new TypeReference<Map<Integer, Long>>() {});
    } catch (JsonProcessingException e) {
      LOGGER.error("failed to decode eigen map, {}, eigenMapStr: {}", e.getMessage(), eigenMapStr, e);
      return null;
    }
  }

  private static String write(Map<Integer, Long> source) {
    String jsonString = null;
    try {
      jsonString = objectMapper.writeValueAsString(source);
    } catch (JsonProcessingException e) {
      LOGGER.error("failed to encode eigen map. {}", e.getMessage(), e);
      return null;
    }
    return SerializationUtils.useZstdSerializeToBase64(jsonString);
  }

  @ReadingConverter
  public static class Read implements Converter<String, Map<Integer, Long>> {

    @Override
    public Map<Integer, Long> convert(String source) {
      return read(source);
    }
  }
  @WritingConverter
  public static class Write implements Converter<Map<Integer, Long>, String> {

    @Override
    public String convert(Map<Integer, Long> source) {
      return write(source);
    }
  }
}