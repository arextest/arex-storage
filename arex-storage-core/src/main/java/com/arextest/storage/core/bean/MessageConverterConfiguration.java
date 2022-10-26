package com.arextest.storage.core.bean;

import com.arextest.storage.core.converter.ZstdJacksonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * configure bean for zstd-jackson message converter
 *
 * @author jmo
 * @since 2021/11/15
 */
@Configuration
class MessageConverterConfiguration {
    @Resource
    private ZstdJacksonMessageConverter zstdJacksonMessageConverter;

    /**
     * used for web api provider how to decode the request before processing.
     * we add a zstd-jackson converter.
     *
     * @return HttpMessageConverters
     */
    @Bean
    @ConditionalOnMissingBean
    HttpMessageConverters customConverters() {
        return new HttpMessageConverters(zstdJacksonMessageConverter);
    }

}