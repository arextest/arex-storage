package com.arextest.storage.web.boot;

import com.arextest.common.metrics.PrometheusConfiguration;
import java.awt.Desktop;
import java.net.URI;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.retry.annotation.EnableRetry;

/**
 * SpringBoot web Application Servlet Initializer
 *
 * @author jmo
 * @since 2021/8/18
 */
@EnableRetry
@SpringBootApplication(scanBasePackages = {"com.arextest.storage", "com.arextest.common"}, exclude = {
    MongoAutoConfiguration.class})
public class WebSpringBootServletInitializer extends SpringBootServletInitializer {

  @Value("${arex.prometheus.port}")
  String prometheusPort;

  private static final String AES_KEY_FIELD_NAME = "arex.desensitization.aesKey";

  @Value("${arex.desensitization.aesKey:}")
  private String aesKey;

  public static void main(String[] args) {
    System.setProperty("java.awt.headless", "false");

    try {
      SpringApplication.run(WebSpringBootServletInitializer.class, args);
      Desktop.getDesktop()
          .browse(new URI("http://localhost:8093/api/storage/record/saveServletTest"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

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

  @PostConstruct
  public void init() {
    PrometheusConfiguration.initMetrics(prometheusPort);
    importConfigurationToEnv();
  }

  public void importConfigurationToEnv() {
    String aesKeyProperty = System.getProperty(AES_KEY_FIELD_NAME);
    if (StringUtils.isEmpty(aesKeyProperty) && StringUtils.isNotEmpty(aesKey)) {
      System.setProperty(AES_KEY_FIELD_NAME, aesKey);
    }
  }
}