package com.arextest.storage.repository.impl.mongo.converters;

import static com.arextest.diff.utils.JacksonHelperUtil.objectMapper;

import com.arextest.common.utils.SerializationUtils;
import com.arextest.model.mock.Mocker.Target;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mongodb.lang.NonNull;
import java.util.Collections;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.PropertyValueConverter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.ValueConversionContext;
import org.springframework.data.convert.WritingConverter;

/**
 * Refactor from the original codec, logic is remained the same
 * Convert eigenMap to encrypted string for storage
 *
 * @author xinyuan_wang
 * @since 2023/11/28
 */
@Slf4j
public class ArexEigenCompressionConverter implements
    PropertyValueConverter<Map<Integer, Long>, String, ValueConversionContext<?>> {

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

  @Override
  public Map<Integer, Long> read(@NonNull String value, @NonNull ValueConversionContext context) {
    return read(value);
  }

  @Override
  public String write(@NonNull Map<Integer, Long> value, @NonNull ValueConversionContext<?> context) {
    return write(value);
  }
}