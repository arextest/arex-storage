package com.arextest.storage.web.controller;

import com.arextest.common.model.response.ResponseCode;
import com.arextest.config.model.dto.application.ApplicationConfiguration;
import com.arextest.config.model.dto.application.InstancesConfiguration;
import com.arextest.config.model.dto.record.DynamicClassConfiguration;
import com.arextest.config.model.dto.record.ServiceCollectConfiguration;
import com.arextest.config.model.vo.AgentRemoteConfigurationRequest;
import com.arextest.config.repository.impl.ComparisonExclusionsConfigurationRepositoryImpl;
import com.arextest.storage.service.config.ConfigurableHandler;
import com.arextest.storage.service.config.impl.ApplicationConfigurableHandler;
import com.arextest.storage.service.config.impl.ApplicationInstancesConfigurableHandler;
import com.arextest.storage.service.config.impl.ApplicationServiceConfigurableHandler;
import com.arextest.storage.service.config.impl.ServiceCollectConfigurableHandler;
import com.arextest.storage.web.controller.config.AgentRemoteConfigurationController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author: QizhengMo
 * @date: 2024/3/6 13:48
 */
@ExtendWith(MockitoExtension.class)
public class ConfigLoadTest {
  @InjectMocks
  AgentRemoteConfigurationController controller;

  @Mock
  private ConfigurableHandler<DynamicClassConfiguration> dynamicClassHandler;
  @Mock
  private ConfigurableHandler<ApplicationConfiguration> applicationHandler;
  @Mock
  private ServiceCollectConfigurableHandler serviceCollectHandler;
  @Mock
  private ApplicationInstancesConfigurableHandler instanceHandler;
  @Mock
  private ApplicationServiceConfigurableHandler applicationServiceHandler;
  @Mock
  private ApplicationConfigurableHandler applicationConfigurableHandler;
  @Mock
  private ThreadPoolExecutor envUpdateHandlerExecutor;
  @Mock
  private ComparisonExclusionsConfigurationRepositoryImpl comparisonExclusionsConfigurationRepository;

  @Test
  public void testInvalidReq() {
    AgentRemoteConfigurationRequest req = new AgentRemoteConfigurationRequest();
    req.setHost("1.1.1.1");
    controller.load(req);

    req.setAppId("TEST");
    controller.load(req);
  }

  @Test
  public void baseTest() {
    AgentRemoteConfigurationRequest req = baseReq();
    mockAppQuery();
    controller.load(req);
  }

  @Test
  public void testNormal() {
    AgentRemoteConfigurationRequest req = baseReq();
    mockAppQuery();
    mockInstanceQuery();
    mockExclusionConfig();
    ServiceCollectConfiguration collectConfig = new ServiceCollectConfiguration();
    collectConfig.setRecordMachineCountLimit(1);

    // returned instance list only contains self
    Mockito.when(serviceCollectHandler.allocateServiceCollectConfig(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Pair.of(collectConfig, selfInstance()));
    Assertions.assertEquals(ResponseCode.SUCCESS.getCodeValue(),
        (int) controller.load(req).getResponseStatusType().getResponseCode());

    // returned instance list contains self
    Mockito.when(serviceCollectHandler.allocateServiceCollectConfig(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Pair.of(collectConfig, hasSelfInstances()));
    Assertions.assertEquals(ResponseCode.SUCCESS.getCodeValue(),
        (int) controller.load(req).getResponseStatusType().getResponseCode());

    // returned instance list contains no self
    Mockito.when(serviceCollectHandler.allocateServiceCollectConfig(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Pair.of(collectConfig, noSelfInstances()));
    Assertions.assertEquals(ResponseCode.SUCCESS.getCodeValue(),
        (int) controller.load(req).getResponseStatusType().getResponseCode());
  }

  private void mockAppQuery() {
    Mockito.when(applicationHandler.useResult(Mockito.any()))
        .thenReturn(new ApplicationConfiguration());
  }

  private void mockInstanceQuery() {
    Mockito.when(instanceHandler.listByAppOrdered(Mockito.any()))
        .thenReturn(selfInstance());
  }

  private void mockExclusionConfig() {
    Mockito.when(comparisonExclusionsConfigurationRepository.listBy(Mockito.anyString(), Mockito.anyInt()))
        .thenReturn(Collections.emptyList());
  }

  private static List<InstancesConfiguration> selfInstance() {
    return Collections.singletonList(new InstancesConfiguration() {{
      setAppId("TEST");
      setHost("1.1.1.1");
    }});
  }

  private static List<InstancesConfiguration> hasSelfInstances() {
    List<InstancesConfiguration> res = new ArrayList<>();
    res.add(new InstancesConfiguration() {{
      setAppId("TEST");
      setHost("1.1.1.1");
    }});
    res.add(new InstancesConfiguration() {{
      setAppId("TEST");
      setHost("1.1.1.2");
    }});
    return res;
  }

  private static List<InstancesConfiguration> noSelfInstances() {
    List<InstancesConfiguration> res = new ArrayList<>();
    res.add(new InstancesConfiguration() {{
      setAppId("TEST");
      setHost("1.1.1.3");
    }});
    res.add(new InstancesConfiguration() {{
      setAppId("TEST");
      setHost("1.1.1.4");
    }});
    return res;
  }

  private static AgentRemoteConfigurationRequest baseReq() {
    AgentRemoteConfigurationRequest req = new AgentRemoteConfigurationRequest();
    req.setAppId("TEST");
    req.setHost("1.1.1.1");
    return req;
  }
}
