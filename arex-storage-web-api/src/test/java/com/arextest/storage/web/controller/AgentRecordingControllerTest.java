package com.arextest.storage.web.controller;

import com.arextest.model.mock.AREXMocker;
import com.arextest.storage.metric.AgentWorkingMetricService;
import com.arextest.storage.service.AgentWorkingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AgentRecordingControllerTest {
  @InjectMocks
  AgentRecordingController service;

  @Mock
  private AgentWorkingService agentWorkingService;
  @Mock
  private AgentWorkingMetricService agentWorkingMetricService;


  @Test
  public void test() {
    service.save(new AREXMocker());
  }
}