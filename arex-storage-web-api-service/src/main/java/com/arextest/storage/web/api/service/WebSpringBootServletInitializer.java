package com.arextest.storage.web.api.service;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

/**
 * SpringBoot web Application Servlet Initializer
 *
 * @author jmo
 * @since 2021/8/18
 */
@SpringBootApplication(scanBasePackages = "com.arextest.storage")
public class WebSpringBootServletInitializer extends SpringBootServletInitializer {

    /**
     * configure for our Servlet
     *
     * @param application builder
     * @return build a source
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(WebSpringBootServletInitializer.class);
    }
}
