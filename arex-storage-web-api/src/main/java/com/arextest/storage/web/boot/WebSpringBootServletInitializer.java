package com.arextest.storage.web.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import java.awt.*;
import java.net.URI;

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

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");

        try {
            SpringApplication.run(WebSpringBootServletInitializer.class, args);
            Desktop.getDesktop().browse(new URI("http://localhost:8093/api/storage/record/saveServletTest"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}