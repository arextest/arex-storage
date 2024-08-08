package com.arextest.storage.service.handler.mocker.coverage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.arextest.common.cache.CacheProvider;
import com.arextest.common.config.DefaultApplicationConfig;
import com.arextest.model.constants.MockAttributeNames;
import com.arextest.model.mock.Mocker;
import com.arextest.model.mock.Mocker.Target;
import com.arextest.model.replay.CaseSendScene;
import com.arextest.storage.metric.MetricListener;
import com.arextest.storage.repository.scenepool.ScenePoolFactory;
import com.arextest.storage.service.InvalidRecordService;
import com.arextest.storage.service.MockSourceEditionService;
import com.arextest.storage.service.UpdateCaseStatusService;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class CoverageMockerHandlerTest {
    @Mock
    private MockSourceEditionService mockSourceEditionService;

    @Mock
    private ScheduledExecutorService coverageHandleDelayedPool;

    @Mock
    private ScenePoolFactory scenePoolFactory;

    @Mock
    private UpdateCaseStatusService updateCaseStatusService;

    @Mock
    private CoverageHandlerSwitch handlerSwitch;

    @Mock
    private InvalidRecordService invalidRecordService;

    @Mock
    private DefaultApplicationConfig defaultApplicationConfig;

    @Mock
    private CacheProvider cacheProvider;

    @Mock
    private List<MetricListener> metricListeners;

    @Mock
    private Mocker coverageMocker;

    @InjectMocks
    private CoverageMockerHandler coverageMockerHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testHandleOnRecordSaving() {
        // Arrange
        when(coverageMocker.getReplayId()).thenReturn("testReplayId");
        when(coverageMocker.getAppId()).thenReturn("testAPP");
        when(coverageMocker.getOperationName()).thenReturn("testOP");
        Target target = new Target();
        target.setAttribute(MockAttributeNames.SCHEDULE_PARAM, CaseSendScene.MIXED_NORMAL.name());
        when(coverageMocker.getTargetRequest()).thenReturn(target);
        when(handlerSwitch.allowReplayTask(anyString())).thenReturn(true);

        // Act
        coverageMockerHandler.handleOnRecordSaving(coverageMocker);

        // Assert
        verify(coverageHandleDelayedPool, times(1)).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }
}