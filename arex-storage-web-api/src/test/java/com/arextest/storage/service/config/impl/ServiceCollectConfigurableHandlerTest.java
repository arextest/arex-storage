package com.arextest.storage.service.config.impl;

import com.arextest.config.model.dto.application.InstancesConfiguration;
import com.arextest.config.model.dto.record.ServiceCollectConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.google.common.collect.Lists;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class ServiceCollectConfigurableHandlerTest {
  @InjectMocks
  ServiceCollectConfigurableHandler service;

  @Mock
  ConfigRepositoryProvider<ServiceCollectConfiguration> repositoryProvider;

  @Mock
  ServiceCollectConfiguration globalDefaultConfiguration;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IllegalAccessException {
    Class<? extends ServiceCollectConfigurableHandler> clazz = service.getClass();
    Field field = clazz.getDeclaredField("globalDefaultConfiguration");
    field.setAccessible(true);
    field.set(service, globalDefaultConfiguration);
  }

  @Test
  void testNoEnv() {
    List<InstancesConfiguration> instances = onlySelfInstances();
    Pair<ServiceCollectConfiguration, List<InstancesConfiguration>> result =
        service.allocateServiceCollectConfig("appId", instances, selfInstance());

    // no env configs, should return all submitted instances
    Assertions.assertEquals(result.getValue(), instances);

    instances = multipleRandomInstances(10);
    result = service.allocateServiceCollectConfig("appId", instances, selfInstance());
    Assertions.assertEquals(result.getValue(), instances);
  }

  @Test
  void testSimpleEnv() {
    Mockito.when(repositoryProvider.listBy(Mockito.anyString())).thenReturn(Collections.singletonList(generateTwoEnvConfig()));
    Pair<ServiceCollectConfiguration, List<InstancesConfiguration>> result =
        service.allocateServiceCollectConfig("appId", onlySelfInstances(), selfInstance());
    // instance with no tag, should use root config
    Assertions.assertEquals(0, result.getKey().getSampleRate());

    Mockito.when(repositoryProvider.listBy(Mockito.anyString())).thenReturn(Collections.singletonList(generateTwoEnvConfig()));
    result = service.allocateServiceCollectConfig("appId", selfAndOtherInstances(), selfInstance());
    // instance with no tag, should use root config, instance list should be size of 2
    Assertions.assertEquals(0, result.getKey().getSampleRate());
    Assertions.assertEquals(2, result.getValue().size());

    Mockito.when(repositoryProvider.listBy(Mockito.anyString())).thenReturn(Collections.singletonList(generateTwoEnvConfig()));
    result = service.allocateServiceCollectConfig("appId", multipleRandomInstances(10), selfInstance());
    // instance with no tag, should use root config, instance list should be size of 2
    Assertions.assertEquals(0, result.getKey().getSampleRate());
    Assertions.assertEquals(10, result.getValue().size());

    Mockito.when(repositoryProvider.listBy(Mockito.anyString())).thenReturn(Collections.singletonList(generateTwoEnvConfig()));
    InstancesConfiguration self = selfInstance();
    self.setTags(Collections.singletonMap("env", "uat"));
    result = service.allocateServiceCollectConfig("appId", Collections.singletonList(self), self);
    // instance with uat tag, should use uat config
    Assertions.assertEquals(1, result.getKey().getSampleRate());

    Mockito.when(repositoryProvider.listBy(Mockito.anyString())).thenReturn(Collections.singletonList(generateTwoEnvConfig()));
    self = selfInstance();
    self.setTags(Collections.singletonMap("env", "pro"));
    result = service.allocateServiceCollectConfig("appId", Collections.singletonList(self), self);
    // instance with uat tag, should use pro config
    Assertions.assertEquals(10, result.getKey().getSampleRate());
  }

  private static ServiceCollectConfiguration generateTwoEnvConfig() {
    ServiceCollectConfiguration rootConfig = new ServiceCollectConfiguration();
    rootConfig.setSampleRate(0);
    ServiceCollectConfiguration uat = new ServiceCollectConfiguration();
    ServiceCollectConfiguration pro = new ServiceCollectConfiguration();

    uat.setAppId("appId");
    uat.setEnvTags(Collections.singletonMap("env", Collections.singletonList("uat")));
    uat.setSampleRate(1);

    pro.setAppId("appId");
    pro.setEnvTags(Collections.singletonMap("env", Collections.singletonList("pro")));
    pro.setSampleRate(10);
    rootConfig.setMultiEnvConfigs(Lists.newArrayList(uat, pro));
    return rootConfig;
  }

  private static InstancesConfiguration selfInstance() {
    InstancesConfiguration res = new InstancesConfiguration();
    res.setAppId("appId");
    res.setHost("1.1.1.1");
    return res;
  }

  private static List<InstancesConfiguration> multipleRandomInstances(int size) {
    List<InstancesConfiguration> res = Lists.newArrayList();
    for (int i = 0; i < size; i++) {
      InstancesConfiguration item = new InstancesConfiguration();
      item.setAppId("appId");
      item.setHost("1.1.1." + i);
      res.add(item);
    }
    return res;
  }

  private static List<InstancesConfiguration> onlySelfInstances() {
    return Collections.singletonList(selfInstance());
  }

  private static List<InstancesConfiguration> selfAndOtherInstances() {
    InstancesConfiguration that = selfInstance();
    that.setHost("2.2.2.2");
    return Lists.newArrayList(selfInstance(), that);
  }
}