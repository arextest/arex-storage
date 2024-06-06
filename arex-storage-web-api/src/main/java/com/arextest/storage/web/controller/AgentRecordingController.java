package com.arextest.storage.web.controller;

import static com.arextest.model.constants.HeaderNames.AREX_AGENT_VERSION;
import static com.arextest.model.constants.HeaderNames.AREX_MOCK_STRATEGY_CODE;
import static com.arextest.storage.model.Constants.TEN_MINUTES;
import com.arextest.common.cache.CacheProvider;
import com.arextest.model.constants.MockAttributeNames;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.model.mock.Mocker.Target;
import com.arextest.model.replay.CompareReplayResult;
import com.arextest.model.replay.QueryMockRequestType;
import com.arextest.model.response.Response;
import com.arextest.storage.cache.CacheKeyUtils;
import com.arextest.storage.metric.AgentWorkingMetricService;
import com.arextest.storage.mock.MockResultContext;
import com.arextest.storage.mock.MockResultMatchStrategy;
import com.arextest.storage.model.InvalidIncompleteRecordRequest;
import com.arextest.storage.serialization.ZstdJacksonSerializer;
import com.arextest.storage.service.AgentWorkingService;
import com.arextest.storage.service.handler.AgentWorkingHandler;
import com.arextest.storage.trace.MDCTracer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * This class defined all api list for agent recording
 *
 * @author jmo
 * @since 2021/11/3
 */
@Controller
@RequestMapping("/api/storage/record/")
@Slf4j
public class AgentRecordingController {

  @Resource
  private AgentWorkingService agentWorkingService;
  @Resource
  private AgentWorkingMetricService agentWorkingMetricService;
  @Resource
  private ZstdJacksonSerializer zstdJacksonSerializer;
  @Resource
  private CacheProvider redisCacheProvider;
  @Resource(name = "custom-fork-join-executor")
  private Executor customForkJoinExecutor;
  @Resource
  private Executor batchSaveMockersExecutor;
  @Resource
  private Executor batchSaveReplayResultsExecutor;
  @Resource
  private AgentWorkingHandler<AREXMocker> handleMockerService;
  @Resource
  private AgentWorkingHandler<CompareReplayResult> handleReplayResultService;

  /**
   * from agent query,means to save the request and try to find a record item as mock result for
   * return.
   *
   * @param requestType the content of request,
   * @return the bytes compress with zstd
   */
  @PostMapping(value = "/query")
  @ResponseBody
  public byte[] query(@RequestBody AREXMocker requestType,
      @RequestHeader(name = AREX_MOCK_STRATEGY_CODE,
          defaultValue = "0") int strategyCode) {
    try {
      MockCategoryType category = requestType.getCategoryType();
      if (category == null) {
        LOGGER.warn("agent query category not found");
        return ZstdJacksonSerializer.EMPTY_INSTANCE;
      }
      if (category.equals(MockCategoryType.CONFIG_FILE)) {
        return agentWorkingService.queryConfigFile(requestType);
      }
      if (StringUtils.isEmpty(requestType.getRecordId())) {
        LOGGER.warn("agent query recordId empty, {}", category);
        return ZstdJacksonSerializer.EMPTY_INSTANCE;
      }
      if (StringUtils.isEmpty(requestType.getReplayId())) {
        LOGGER.warn("agent query replayId is empty,{}, recordId:{}", category,
            requestType.getRecordId());
        return ZstdJacksonSerializer.EMPTY_INSTANCE;
      }
      MDCTracer.addTrace(requestType);
      MockResultContext context = new MockResultContext(MockResultMatchStrategy.of(strategyCode));
      return agentWorkingMetricService.queryMockResult(requestType, context);
    } catch (Throwable throwable) {
      LOGGER.error("query error:{} from category:{}", throwable.getMessage(), requestType,
          throwable);
    } finally {
      MDCTracer.clear();
    }
    return ZstdJacksonSerializer.EMPTY_INSTANCE;
  }

  @PostMapping(value = "/queryMockers")
  @ResponseBody
  public byte[] queryMockers(@RequestBody QueryMockRequestType requestType) {
    try {
      if (StringUtils.isEmpty(requestType.getRecordId())) {
        LOGGER.warn("agent query recordId empty");
        return ZstdJacksonSerializer.EMPTY_INSTANCE;
      }

      MDCTracer.addRecordId(requestType.getRecordId());
      return agentWorkingService.queryMockers(requestType.getRecordId(), requestType.getFieldNames(), requestType.getCategoryTypes());
    } catch (Exception e) {
      LOGGER.error("query error:{} from category:{}", e.getMessage(), requestType, e);
    } finally {
      MDCTracer.clear();
    }
    return ZstdJacksonSerializer.EMPTY_INSTANCE;
  }

  /**
   * from agent recording, save the content for replay
   *
   * @param requestType the record content of request
   * @return response for save result
   */
  @PostMapping(value = "/save")
  @ResponseBody
  public Response save(@RequestBody AREXMocker requestType) {
    MockCategoryType category = requestType.getCategoryType();
    if (category == null || StringUtils.isEmpty(category.getName())) {
      LOGGER.warn("The name of category is empty from agent record save not allowed ,request:{}",
          requestType);
      return ResponseUtils.parameterInvalidResponse("empty category");
    }
    try {
      MDCTracer.addTrace(requestType);
      boolean saveResult = agentWorkingMetricService.saveRecord(requestType);
      LOGGER.info("agent record save result:{},category:{},recordId:{}", saveResult, category,
          requestType.getRecordId());
      return ResponseUtils.successResponse(saveResult);
    } catch (Exception e) {
      LOGGER.error("save record error: {} from category: {}, recordId: {}",
          e.getMessage(), requestType.getCategoryType(), requestType.getRecordId(), e);
      return ResponseUtils.exceptionResponse(e.getMessage());
    } finally {
      MDCTracer.clear();
    }
  }

  @PostMapping(value = "/batchSaveMockers")
  @ResponseBody
  public Response batchSaveMockers(@RequestBody List<AREXMocker> mockers) {
    if (CollectionUtils.isEmpty(mockers)) {
        return ResponseUtils.parameterInvalidResponse("request body is empty");
    }

    try {
      // Return the results directly to the agent, asynchronous processing process
      CompletableFuture.runAsync(() -> handleMockerService.batchSave(mockers), batchSaveMockersExecutor);
    } catch (Exception e) {
      LOGGER.error("batch save record error: {}", e.getMessage(), e);
      return ResponseUtils.exceptionResponse(e.getMessage());
    }
    return ResponseUtils.successResponse(true);
  }

  @PostMapping(value = "/batchSaveReplayResult")
  @ResponseBody
  public Response batchSaveReplayResult(@RequestBody List<CompareReplayResult> replayResults,
      @RequestHeader(name = AREX_AGENT_VERSION) String agentVersion) {
    if (CollectionUtils.isEmpty(replayResults)) {
        return ResponseUtils.parameterInvalidResponse("request body is empty");
    }

    try {
      // Return the results directly to the agent, asynchronous processing process
      CompletableFuture.runAsync(() -> {
        handleReplayResultService.batchSave(replayResults);
        putAgentVersionInRedis(replayResults.get(0).getReplayId(), agentVersion);
      }, batchSaveReplayResultsExecutor);
    } catch (Exception e) {
      LOGGER.error("batch save replay result error: {}", e.getMessage(), e);
      return ResponseUtils.exceptionResponse(e.getMessage());
    }
    return ResponseUtils.successResponse(true);
  }

  @GetMapping(value = "/saveTest/", produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseBody
  public Response saveTest(
      @RequestParam(required = false, defaultValue = "Servlet") String category) {
    return save(arexMocker(MockCategoryType.create(category)));
  }

  @PostMapping(value = {"/invalidCase", "/invalidIncompleteRecord"}, produces = {
      MediaType.APPLICATION_JSON_VALUE})
  @ResponseBody
  public Response invalidIncompleteRecord(@RequestBody InvalidIncompleteRecordRequest requestType) {
    try {
      if (StringUtils.isEmpty(requestType.getRecordId())) {
        LOGGER.warn("[[title=invalidIncompleteRecord]]agent invalid case recordId empty, {}",
            requestType);
        return ResponseUtils.emptyRecordIdResponse();
      }

      MDCTracer.addRecordId(requestType.getRecordId());
      MDCTracer.addReplayId(requestType.getReplayId());
      LOGGER.info("[[title=invalidIncompleteRecord]]agent invalid case, request:{}", requestType);

      CompletableFuture.runAsync(
          () -> agentWorkingMetricService.invalidIncompleteRecord(requestType),
          customForkJoinExecutor
      );
      return ResponseUtils.successResponse(true);
    } catch (Throwable throwable) {
      LOGGER.error("[[title=invalidIncompleteRecord]] invalidCase error:{}",
          throwable.getMessage());
      return ResponseUtils.exceptionResponse(throwable.getMessage());
    } finally {
      MDCTracer.clear();
    }
  }

  private AREXMocker arexMocker(MockCategoryType categoryType) {
    AREXMocker mocker = new AREXMocker(categoryType);
    mocker.setOperationName("hello");
    mocker.setRecordId("demo-recordId-" + System.currentTimeMillis());
    mocker.setAppId("demoAppID");
    mocker.setCreationTime(System.currentTimeMillis());
    Target targetRequest = new Target();
    targetRequest.setBody("request body");
    mocker.setTargetRequest(targetRequest);
    Map<String, String> headers = new HashMap<>();
    headers.put(".name", "value");
    targetRequest.setAttribute(MockAttributeNames.HTTP_METHOD, "POST");
    targetRequest.setAttribute(MockAttributeNames.HEADERS, headers);
    return mocker;
  }

  @PostMapping(value = "/queryTest", produces = {MediaType.APPLICATION_JSON_VALUE})
  public @ResponseBody
  Mocker queryTest(@RequestBody AREXMocker body, @RequestHeader(name = AREX_MOCK_STRATEGY_CODE,
      defaultValue = "0") int strategyCode) {
    try {
      byte[] bytes = this.query(body, strategyCode);
      return zstdJacksonSerializer.deserialize(bytes, AREXMocker.class);
    } catch (Throwable throwable) {
      LOGGER.error("queryTest error:{} ", throwable.getMessage(), throwable);
    }
    return null;
  }

  private void putAgentVersionInRedis(String replayId, String agentVersion) {
    if (StringUtils.isEmpty(agentVersion) || StringUtils.isEmpty(replayId)) {
      return;
    }

    byte[] value = agentVersion.getBytes(StandardCharsets.UTF_8);
    redisCacheProvider.put(CacheKeyUtils.buildAgentVersionKey(replayId), TEN_MINUTES, value);
  }

}
