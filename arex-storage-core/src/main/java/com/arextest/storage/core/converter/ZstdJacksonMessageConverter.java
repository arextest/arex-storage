package com.arextest.storage.core.converter;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.core.serialization.ZstdJacksonSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

import static org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter.DEFAULT_CHARSET;

/**
 * custom a ZstdJacksonMessageConverter decode from request
 *
 * @author jmo
 * @since 2021/11/15
 */
@Slf4j
@Component
public final class ZstdJacksonMessageConverter extends AbstractHttpMessageConverter<Object> {

    @Resource
    private ZstdJacksonSerializer zstdJacksonSerializer;

    public static final String ZSTD_JSON_MEDIA_TYPE = "application/zstd-json;charset=UTF-8";
    public static final String AREX_MOCKER_CATEGORY_HEADER = "arex-mocker-category";

    /**
     * create a application/zstd-json;charset=UTF-8
     */
    public ZstdJacksonMessageConverter() {
        super(DEFAULT_CHARSET, MediaType.parseMediaType(ZSTD_JSON_MEDIA_TYPE));
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return !(clazz == byte[].class || clazz.isPrimitive());
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException,
            HttpMessageNotReadableException {
        String shortName = inputMessage.getHeaders().getFirst(AREX_MOCKER_CATEGORY_HEADER);
        if (StringUtils.isNotEmpty(shortName)) {
            MockCategoryType category = MockCategoryType.of(shortName);
            if (category == null) {
                LOGGER.warn("Zstd deserialize not found category for requested :{}", shortName);
                return null;
            }
            return zstdJacksonSerializer.deserialize(inputMessage.getBody(), category.getMockImplClassType());
        }
        return zstdJacksonSerializer.deserialize(inputMessage.getBody(), clazz);
    }

    @Override
    protected void writeInternal(Object o, HttpOutputMessage outputMessage) throws IOException,
            HttpMessageNotWritableException {
        zstdJacksonSerializer.serializeTo(o, outputMessage.getBody());
    }
}