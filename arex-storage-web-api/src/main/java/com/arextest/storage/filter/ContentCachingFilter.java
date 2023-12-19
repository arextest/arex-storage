package com.arextest.storage.filter;

import com.arextest.storage.metric.MetricListener;
import java.io.IOException;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Cache the response and record the size of the request and response
 * created by xinyuan_wang on 2023/12/25
 */
public class ContentCachingFilter implements Filter {
  private static final String GET_METHOD = "GET";
  private final List<MetricListener> metricListeners;
  public ContentCachingFilter(List<MetricListener> metricListeners) {
    this.metricListeners = metricListeners;
  }

  @Override
  public void init(FilterConfig filterConfig) {
    // do nothing
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (CollectionUtils.isEmpty(metricListeners)) {
      chain.doFilter(request, response);
      return;
    }
    if (skipMetric(((HttpServletRequest) request).getMethod())) {
      chain.doFilter(request, response);
      return;
    }
    chain.doFilter(new ContentCachingRequestWrapper((HttpServletRequest) request),
        new ContentCachingResponseWrapper((HttpServletResponse) response));
  }

  @Override
  public void destroy() {
    // do nothing
  }

  private boolean skipMetric(String method) {
    return StringUtils.isEmpty(method) || GET_METHOD.equalsIgnoreCase(method);
  }
}
