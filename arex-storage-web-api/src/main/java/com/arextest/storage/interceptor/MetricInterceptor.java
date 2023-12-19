package com.arextest.storage.interceptor;

import com.arextest.storage.metric.MetricListener;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * record request and response interceptor
 * created by xinyuan_wang on 2023/12/25
 */
@Component
public class MetricInterceptor implements HandlerInterceptor {
  private static final String CLIENT_APP_HEADER = "client-app";
  private static final String CATEGORY_TYPE_HEADER = "category-type";
  private static final String START_TIME = "startTime";
  private static final String ENTRY_PAYLOAD_NAME = "service.entry.payload";
  private static final String TYPE = "type";
  private static final String REQUEST_TAG = "request";
  private static final String RESPONSE_TAG = "response";
  private static final String CLIENT_APP_ID = "clientAppId";
  private static final String PATH = "path";
  private static final String CATEGORY = "category";
  public final List<MetricListener> metricListeners;

  public MetricInterceptor(List<MetricListener> metricListeners) {
    this.metricListeners = metricListeners;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    request.setAttribute(START_TIME, System.currentTimeMillis());
    return true;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
      @Nullable ModelAndView modelAndView) throws Exception {
    if (!(request instanceof ContentCachingRequestWrapper && response instanceof ContentCachingResponseWrapper)) {
      return;
    }

    long endTime = System.currentTimeMillis();

    byte[] requestBody = ((ContentCachingRequestWrapper) request).getContentAsByteArray();
    int requestLength = requestBody.length;

    ContentCachingResponseWrapper responseWrapper = (ContentCachingResponseWrapper) response;
    int responseLength = responseWrapper.getContentSize();
    responseWrapper.copyBodyToResponse();

    long startTime = (Long) request.getAttribute(START_TIME);

    String clientApp = request.getHeader(CLIENT_APP_HEADER);
    String category = request.getHeader(CATEGORY_TYPE_HEADER);
    recordPayloadInfo(clientApp, category, request.getRequestURI(), requestLength, responseLength, endTime - startTime);
  }

  public void recordPayloadInfo(String clientApp, String category,
      String path, int requestLength, int responseLength, long executeMillis) {
    if (CollectionUtils.isEmpty(metricListeners)) {
      return;
    }

    Map<String, String> tags = Maps.newHashMapWithExpectedSize(5);
    putIfValueNotEmpty(clientApp, CLIENT_APP_ID, tags);
    putIfValueNotEmpty(category, CATEGORY, tags);
    putIfValueNotEmpty(path, PATH, tags);
    for (MetricListener metricListener : metricListeners) {
      if (executeMillis > 0) {
        metricListener.recordTime(ENTRY_PAYLOAD_NAME, tags, executeMillis);
      }

      tags.put(TYPE, REQUEST_TAG);
      metricListener.recordSize(ENTRY_PAYLOAD_NAME, tags, requestLength);
      tags.put(TYPE, RESPONSE_TAG);
      metricListener.recordSize(ENTRY_PAYLOAD_NAME, tags, responseLength);
    }
  }

  private void putIfValueNotEmpty(String value, String tagName, Map<String, String> tags) {
    if (StringUtils.isEmpty(value)) {
      return;
    }
    tags.put(tagName, value);
  }
}
