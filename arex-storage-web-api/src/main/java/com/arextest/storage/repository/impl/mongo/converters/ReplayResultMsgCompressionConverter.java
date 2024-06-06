package com.arextest.storage.repository.impl.mongo.converters;

import com.arextest.common.serialization.SerializationProviders;
import com.arextest.common.utils.SerializationUtils;
import com.arextest.common.utils.StreamWrapUtils;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.convert.PropertyValueConverter;
import org.springframework.data.convert.ValueConversionContext;
import org.apache.commons.lang3.StringUtils;

/**
 * Refactor from the original codec, logic is remained the same
 * Convert eigenMap to encrypted string for storage
 *
 * @author xinyuan_wang
 * @since 2023/11/28
 */
@Slf4j
public class ReplayResultMsgCompressionConverter implements
    PropertyValueConverter<String, String, ValueConversionContext<?>> {
  @Override
  public String read(String value, ValueConversionContext<?> context) {
    return value;
  }

  @Override
  public String write(String value, ValueConversionContext<?> context) {
    if (StringUtils.isEmpty(value)) {
      return value;
    }

    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStream base64Stream = StreamWrapUtils.wrapBase64(out)) {
      SerializationUtils.useZstdSerializeTo(SerializationProviders.UTF8_TEXT_PROVIDER, base64Stream, value);
      return out.toString(StandardCharsets.UTF_8.name());
    } catch (Throwable e) {
      LOGGER.error("zstd compress error: {}, source: {}", e.getMessage(), value, e);
    }
    return null;
  }
}