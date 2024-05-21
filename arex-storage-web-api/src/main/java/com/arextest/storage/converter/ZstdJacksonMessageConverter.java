package com.arextest.storage.converter;

import com.arextest.storage.serialization.ZstdJacksonSerializer;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/**
 * custom a ZstdJacksonMessageConverter decode from request
 *
 * @author jmo
 * @since 2021/11/15
 */
@Slf4j
@Component
public final class ZstdJacksonMessageConverter extends AbstractHttpMessageConverter<Object>
    implements GenericHttpMessageConverter<Object> {

  public static final String ZSTD_JSON_MEDIA_TYPE = "application/zstd-json;charset=UTF-8";
  private final Map<Type, TypeReference<Object>> typeReferenceCache = new ConcurrentHashMap<>();
  @Resource
  private ZstdJacksonSerializer zstdJacksonSerializer;


  /**
   * create a application/zstd-json;charset=UTF-8
   */
  public ZstdJacksonMessageConverter() {
    super(MediaType.parseMediaType(ZSTD_JSON_MEDIA_TYPE));
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    return !(clazz == byte[].class || clazz.isPrimitive());
  }

  @Override
  protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException,
      HttpMessageNotReadableException {
    return zstdJacksonSerializer.deserialize(inputMessage.getBody(), clazz);
  }

  @Override
  protected void writeInternal(Object o, HttpOutputMessage outputMessage) throws IOException,
      HttpMessageNotWritableException {
    zstdJacksonSerializer.serializeTo(o, outputMessage.getBody());
  }
  @Override
  public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
    return super.canRead(contextClass, mediaType);
  }

  @Override
  public Object read(Type type, Class<?> contextClass, HttpInputMessage inputMessage)
      throws IOException, HttpMessageNotReadableException {
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      if (List.class.isAssignableFrom((Class<?>) parameterizedType.getRawType())) {
        return readInternalWithType(getTypeReference(type), inputMessage);
      }
    }
    return super.read(contextClass, inputMessage);
  }

  @Override
  public boolean canWrite(Type type, Class<?> clazz, MediaType mediaType) {
    return super.canWrite(clazz, mediaType);
  }

  @Override
  public void write(Object o, Type type, MediaType contentType, HttpOutputMessage outputMessage)
      throws IOException, HttpMessageNotWritableException {
    super.write(o, contentType, outputMessage);
  }

  protected <T> T readInternalWithType(TypeReference<T> type, HttpInputMessage inputMessage) throws IOException,
      HttpMessageNotReadableException {
    return zstdJacksonSerializer.deserialize(inputMessage.getBody(), type);
  }

  private TypeReference<Object> getTypeReference(Type type) {
    return typeReferenceCache.computeIfAbsent(type, CustomTypeReference::new);
  }

  private static class CustomTypeReference extends TypeReference<Object> {
    private final Type type;

    CustomTypeReference(Type type) {
      this.type = type;
    }

    @Override
    public Type getType() {
      return type;
    }
  }

}