package com.arextest.storage.repository.impl.mongo.converters;

import com.arextest.common.utils.SerializationUtils;
import com.arextest.extension.desensitization.DataDesensitization;
import com.arextest.extension.desensitization.DefaultDataDesensitization;
import com.arextest.model.mock.Mocker.Target;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

@Slf4j
public class ArexMockerCompressionConverter {
  private static final DataDesensitization desensitization = new DefaultDataDesensitization();

  private static Target read(String source)  {
    String encodeWithDecryptString = null;
    try {
      encodeWithDecryptString = desensitization.decrypt(source);
    } catch (Exception e) {
      LOGGER.error("Data decrypt failed", e);
    }
    return SerializationUtils.useZstdDeserialize(encodeWithDecryptString, Target.class);
  }

  private static String write(Target source) {
    String base64Result = SerializationUtils.useZstdSerializeToBase64(source);
    try {
      base64Result = desensitization.encrypt(base64Result);
      return base64Result;
    } catch (Exception e) {
      LOGGER.error("Data encrypt failed", e);
    }
    return base64Result;
  }

  @ReadingConverter
  public static class Read implements Converter<String, Target> {

    @Override
    public Target convert(String source) {
      return read(source);
    }
  }
  @WritingConverter
  public static class Write implements Converter<Target, String> {

    @Override
    public String convert(Target source) {
      return write(source);
    }
  }
}