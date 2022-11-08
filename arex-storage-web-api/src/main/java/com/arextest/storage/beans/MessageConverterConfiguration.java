package com.arextest.storage.beans;

import com.arextest.storage.converter.ZstdJacksonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * configure bean for zstd-jackson message converter
 *
 * @author jmo
 * @since 2021/11/15
 */
@Configuration
@ConditionalOnMissingBean(HttpMessageConverters.class)
class MessageConverterConfiguration {

    /**
     * used for web api provider how to decode the request before processing.
     * we add a zstd-jackson converter.
     *
     * @return HttpMessageConverters
     */
    @Bean
    HttpMessageConverters customConverters(ZstdJacksonMessageConverter zstdJacksonMessageConverter) {
        return new HttpMessageConverters(zstdJacksonMessageConverter);
    }

}