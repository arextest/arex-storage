package com.arextest.storage.converter;

import com.arextest.storage.serialization.ZstdJacksonSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * custom a ZstdJacksonMessageConverter decode from request
 *
 * @author jmo
 * @since 2021/11/15
 */
@Slf4j
@Component
public final class ZstdJacksonMessageConverter extends AbstractHttpMessageConverter<Object> {

    public static final String ZSTD_JSON_MEDIA_TYPE = "application/zstd-json;charset=UTF-8";
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
}