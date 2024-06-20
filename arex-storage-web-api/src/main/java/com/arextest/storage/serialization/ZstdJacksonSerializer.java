package com.arextest.storage.serialization;


import com.arextest.common.serialization.SerializationProvider;
import com.arextest.common.serialization.SerializationProviders;
import com.arextest.common.utils.SerializationUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author jmo
 * @since 2021/11/8
 */
@Component
@Slf4j
public final class ZstdJacksonSerializer {

  /**
   * The bytes of zstd  equal json string "{}",useful deserialize
   */
  public static final byte[] EMPTY_INSTANCE = SerializationUtils.EMPTY_INSTANCE;

  /**
   * The bytes of zstd  equal json string "[]",useful deserialize
   */
  public static final byte[] EMPTY_INSTANCE_LIST = SerializationUtils.EMPTY_INSTANCE_LIST;

  private final SerializationProvider serializationProvider;

  public ZstdJacksonSerializer(ObjectMapper objectMapper) {
    this.serializationProvider = SerializationProviders.jacksonProvider(objectMapper);
  }

  public <T> void serializeTo(T value, OutputStream outputStream) {
    if (value == null) {
      return;
    }
    SerializationUtils.useZstdSerializeTo(this.serializationProvider, outputStream, value);
  }

  public <T> byte[] serialize(T value) {
    if (value == null) {
      return null;
    }
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      serializeTo(value, out);
      return out.toByteArray();
    } catch (IOException e) {
      LOGGER.error("serialize error:{}", e.getMessage(), e);
    }
    return null;
  }

  public <T> T deserialize(InputStream inputStream, Class<T> clazz) {
    if (inputStream == null) {
      return null;
    }
    return SerializationUtils.useZstdDeserialize(this.serializationProvider, inputStream, clazz);
  }

  public <T> T deserialize(byte[] zstdValues, Class<T> clazz) {
    if (zstdValues == null) {
      return null;
    }
    return SerializationUtils.useZstdDeserialize(this.serializationProvider, zstdValues, clazz);
  }

  public <T> T deserialize(byte[] zstdValues, TypeReference<T> typeReference) {
    if (zstdValues == null) {
      return null;
    }
    return SerializationUtils.useZstdDeserialize(this.serializationProvider, zstdValues,
        typeReference);
  }

  public <T> T deserialize(InputStream inputStream, TypeReference<T> typeReference) {
    if (inputStream == null) {
      return null;
    }
    return SerializationUtils.useZstdDeserialize(this.serializationProvider, inputStream,
        typeReference);
  }
}