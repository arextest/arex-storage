package com.arextest.storage.mock.impl;

import static com.arextest.storage.model.Constants.AREX_CONFIG_MOCKERCONVERT_ENABLED;
import static com.arextest.storage.model.Constants.AREX_CONFIG_MOCKERCONVERT_ENABLED_DEFAULT;

import com.arextest.diff.model.classloader.RemoteJarClassLoader;
import com.arextest.diff.utils.RemoteJarLoaderUtils;
import com.arextest.extension.mockconvert.MockerConverter;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.mapper.AREXMockerMapper;
import com.arextest.storage.mock.MockerResultConverter;
import com.arextest.storage.service.QueryConfigService;
import com.arextest.storage.service.QueryConfigService.ScheduleReplayConfigurationResponse;
import com.arextest.storage.service.config.ApplicationDefaultConfig;
import com.arextest.storage.trace.MDCTracer;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.ap.internal.util.Strings;

@Slf4j
@RequiredArgsConstructor
public class DefaultMockerResultConverterImpl implements MockerResultConverter {

  final QueryConfigService queryConfigService;

  final ApplicationDefaultConfig applicationDefaultConfig;

  private LoadingCache<String, MockerConvertSummary> mockerConvertSummaryCache =
      Caffeine.newBuilder().maximumSize(500).removalListener(((key, value, cause) -> {
            LOGGER.info("mockerConvertSummaryCache expire, key : {}, cause : {}", key, cause);
            try {
              if (value instanceof MockerConvertSummary) {
                RemoteJarClassLoader serviceClassLoader = ((MockerConvertSummary) value)
                    .getServiceClassLoader();
                if (serviceClassLoader != null) {
                  serviceClassLoader.close();
                }
              }
            } catch (Exception e) {
              LOGGER.warn("close serviceClassLoader error, appId : {}, exception : {}", key,
                  e.getMessage());
            }
          }))
          .expireAfterWrite(2, TimeUnit.HOURS)
          .build(new MockerConvertCacheLoader());

  @Override
  public <T extends Mocker> T convert(MockCategoryType category, T mocker) {
    return applicationDefaultConfig.getConfigAsBoolean(AREX_CONFIG_MOCKERCONVERT_ENABLED,
        AREX_CONFIG_MOCKERCONVERT_ENABLED_DEFAULT)
        ? convertFromJar(category, mocker) : mocker;
  }

  @SuppressWarnings("unchecked")
  protected <T extends Mocker> T convertFromJar(MockCategoryType category, T mocker) {
    String appId = mocker.getAppId();
    if (StringUtils.isEmpty(appId)) {
      return mocker;
    }

    MDCTracer.addConvertCacheKey(appId);
    MockerConvertSummary mockerConvertSummary = getMockerConvertSummary(appId);
    if (mockerConvertSummary == null || mockerConvertSummary.getMockerConverter() == null) {
      return mocker;
    }

    T convertedMocker = null;
    MockerConverter mockerConverter = mockerConvertSummary.getMockerConverter();
    try {
      convertedMocker = (T) mockerConverter.mockConvert(category, deepMockerCopy(mocker));
    } catch (Exception e) {
      LOGGER.error("convert mocker error, appId : {}, exception : {}", appId, e.getMessage());
    }
    MDCTracer.removeConvertCacheKey();
    return convertedMocker != null ? convertedMocker : mocker;
  }

  protected MockerConvertSummary getMockerConvertSummary(String appId) {
    return mockerConvertSummaryCache.get(appId);
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MockerConvertSummary {

    private RemoteJarClassLoader serviceClassLoader;
    private MockerConverter mockerConverter;
  }

  private class MockerConvertCacheLoader implements CacheLoader<String, MockerConvertSummary> {

    @Override
    public MockerConvertSummary load(String key) throws Exception {
      ScheduleReplayConfigurationResponse response =
          queryConfigService.queryScheduleReplayConfiguration(key);
      return loadMockerConverter(response);
    }

    private MockerConvertSummary loadMockerConverter(ScheduleReplayConfigurationResponse response) {
      if (response == null || response.getBody() == null || Strings.isEmpty(
          response.getBody().getMockHandlerJarUrl())) {
        LOGGER.info("load mockerConverter failed, scheduleReplayConfiguration is null");
        return new MockerConvertSummary();
      }
      String mockHandlerJarUrl = response.getBody().getMockHandlerJarUrl();
      RemoteJarClassLoader serviceClassLoader;
      try {
        serviceClassLoader = RemoteJarLoaderUtils.loadJar(mockHandlerJarUrl);
        List<MockerConverter> mockerConverters =
            RemoteJarLoaderUtils.loadService(MockerConverter.class, serviceClassLoader);

        if (CollectionUtils.isEmpty(mockerConverters)) {
          LOGGER.info("load mockerConverter failed, mockerConverters is empty");
          return new MockerConvertSummary();
        }

        for (MockerConverter mockerConverter : mockerConverters) {
          if (mockerConverter != null) {
            LOGGER.info("load mockerConverter success, jar url : {}, className:{}",
                mockHandlerJarUrl, mockerConverter.getClass().getName());
            return new MockerConvertSummary(serviceClassLoader, mockerConverter);
          }
        }

      } catch (Throwable e) {
        LOGGER.warn("load decompress service error, jar url : {}, exception : {}",
            mockHandlerJarUrl,
            e.getMessage());
      }
      return new MockerConvertSummary();
    }

  }

  private Mocker deepMockerCopy(Mocker mocker) {
    return AREXMockerMapper.INSTANCE.dtoFromEntity((AREXMocker) mocker);
  }

}
