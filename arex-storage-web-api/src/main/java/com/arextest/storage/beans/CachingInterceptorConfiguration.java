package com.arextest.storage.beans;

import com.arextest.storage.filter.ContentCachingFilter;
import com.arextest.storage.interceptor.MetricInterceptor;
import com.arextest.storage.metric.MetricListener;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration class that registers and sets up the ContentCachingFilter interceptor.
 * created by xinyuan_wang on 2023/12/25
 */
@Configuration
public class CachingInterceptorConfiguration implements WebMvcConfigurer {

  private final MetricInterceptor metricInterceptor;
  public final List<MetricListener> metricListeners;

  public CachingInterceptorConfiguration(MetricInterceptor metricInterceptor, List<MetricListener> metricListeners) {
    this.metricInterceptor = metricInterceptor;
    this.metricListeners = metricListeners;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    if (CollectionUtils.isEmpty(metricListeners)) {
      return;
    }
    registry.addInterceptor(metricInterceptor);
  }

  /**
   * Register the ContentCachingFilter filter and add it to the Filter chain
   */
  @Bean
  public FilterRegistrationBean<ContentCachingFilter> contentCachingFilter() {
    FilterRegistrationBean<ContentCachingFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new ContentCachingFilter(metricListeners));
    registrationBean.addUrlPatterns("/*");
    return registrationBean;
  }
}
