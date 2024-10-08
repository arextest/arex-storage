package com.arextest.storage.repository.impl.mongo.converters;

import com.arextest.common.utils.SerializationUtils;
import com.arextest.extension.desensitization.DataDesensitization;
import com.arextest.model.mock.Mocker.Target;
import com.mongodb.lang.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

@Slf4j
public class ArexMockerCompressionConverter {

  @ReadingConverter
  @RequiredArgsConstructor
  public static class Read implements Converter<String, Target> {

    private final DataDesensitization dataDesensitization;

    @Override
    public Target convert(@NonNull String source) {
      return read(source);
    }

    private Target read(String source) {
      String encodeWithDecryptString = null;
      try {
        encodeWithDecryptString = dataDesensitization.decrypt(source);
      } catch (Exception e) {
        LOGGER.error("Data decrypt failed", e);
      }
      return SerializationUtils.useZstdDeserialize(encodeWithDecryptString, Target.class);
    }
  }

  @WritingConverter
  @RequiredArgsConstructor
  public static class Write implements Converter<Target, String> {

    private final DataDesensitization dataDesensitization;

    @Override
    public String convert(@NonNull Target source) {
      return write(source);
    }

    private String write(Target source) {
      String base64Result = SerializationUtils.useZstdSerializeToBase64(source);
      try {
        base64Result = dataDesensitization.encrypt(base64Result);
        return base64Result;
      } catch (Exception e) {
        LOGGER.error("Data encrypt failed", e);
      }
      return base64Result;
    }

  }

}