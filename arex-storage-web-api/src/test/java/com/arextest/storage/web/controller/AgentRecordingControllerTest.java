package com.arextest.storage.web.controller;

import com.arextest.model.mock.AREXMocker;
import com.arextest.storage.metric.AgentWorkingMetricService;
import com.arextest.storage.service.AgentWorkingService;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AgentRecordingControllerTest {
  @InjectMocks
  AgentRecordingController service;

  @Mock
  private AgentWorkingService agentWorkingService;
  @Mock
  private AgentWorkingMetricService agentWorkingMetricService;


  @Test
  public void test() {
    service.save(new AREXMocker(), Boolean.TRUE.toString());
  }
}