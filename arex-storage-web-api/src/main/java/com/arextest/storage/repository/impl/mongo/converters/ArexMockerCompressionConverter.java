package com.arextest.storage.repository.impl.mongo.converters;

import com.arextest.common.utils.SerializationUtils;
import com.arextest.model.mock.Mocker.Target;
import com.arextest.storage.repository.impl.mongo.DesensitizationLoader;
import com.mongodb.lang.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

@Slf4j
public class ArexMockerCompressionConverter {
  private static Target read(String source)  {
    String encodeWithDecryptString = null;
    try {
      encodeWithDecryptString = DesensitizationLoader.DESENSITIZATION_SERVICE.decrypt(source);
    } catch (Exception e) {
      LOGGER.error("Data decrypt failed", e);
    }
    return SerializationUtils.useZstdDeserialize(encodeWithDecryptString, Target.class);
  }

  private static String write(Target source) {
    String base64Result = SerializationUtils.useZstdSerializeToBase64(source);
    try {
      base64Result = DesensitizationLoader.DESENSITIZATION_SERVICE.encrypt(base64Result);
      return base64Result;
    } catch (Exception e) {
      LOGGER.error("Data encrypt failed", e);
    }
    return base64Result;
  }

  @ReadingConverter
  public static class Read implements Converter<String, Target> {

    @Override
    public Target convert(@NonNull String source) {
      return read(source);
    }
  }
  @WritingConverter
  public static class Write implements Converter<Target, String> {

    @Override
    public String convert(@NonNull Target source) {
      return write(source);
    }
  }
}