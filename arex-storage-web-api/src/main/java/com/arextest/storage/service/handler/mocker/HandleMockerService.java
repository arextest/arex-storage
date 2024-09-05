package com.arextest.storage.service.handler.mocker;

import com.arextest.common.cache.CacheProvider;
import com.arextest.model.mock.AREXQueryMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.metric.AgentWorkingMetricService;
import com.arextest.storage.model.Constants;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.repository.RepositoryProviderFactory;
import com.arextest.storage.service.InvalidRecordService;
import com.arextest.storage.service.ScheduleReplayingService;
import com.arextest.storage.trace.MDCTracer;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class HandleMockerService extends AbstractAgentWorkingService
    implements AgentWorkingHandler<AREXQueryMocker> {

  @Resource
  private RepositoryProviderFactory repositoryProviderFactory;
  @Resource
  private ScheduleReplayingService scheduleReplayingService;

  private static final String TITLE = "[[title=handleMocker]]";

  public HandleMockerService(AgentWorkingMetricService agentWorkingMetricService,
      CacheProvider redisCacheProvider, InvalidRecordService invalidRecordService) {
    super(agentWorkingMetricService, redisCacheProvider, invalidRecordService);
  }

  @Override
  public boolean batchSave(List<AREXQueryMocker> items) {
    // todo: implement this method
    return true;
  }

  @Override
  public List<AREXQueryMocker> batchQuery(String recordId, String[] fieldNames, String[] categoryList) {
    try {
      if (StringUtils.isEmpty(recordId)) {
        LOGGER.warn("{}record id is empty", TITLE);
        return Collections.emptyList();
      }

      MDCTracer.addRecordId(recordId);
      List<RepositoryProvider<? extends Mocker>> repositoryProviderList =
          repositoryProviderFactory.getRepositoryProviderList(Constants.CLAZZ_NAME_AREX_QUERY_MOCKER);
      Set<MockCategoryType> categoryTypes = repositoryProviderFactory.getCategoryTypesByName(
          categoryList);

      // find data in the order of rolling -> pinned
      for (RepositoryProvider<? extends Mocker> repositoryReader : repositoryProviderList) {
        List<AREXQueryMocker> mockers = scheduleReplayingService.queryRecordsByRepositoryReader(recordId,
            categoryTypes, repositoryReader, fieldNames, AREXQueryMocker.class);
        if (CollectionUtils.isNotEmpty(mockers)) {
          return mockers;
        }
      }

      return Collections.emptyList();
    } catch (Exception e) {
      LOGGER.error("{}batch query mockers error: {} from record id: {}", TITLE, e.getMessage(), recordId, e);
    } finally {
      MDCTracer.clear();
    }
    return Collections.emptyList();
  }
}
