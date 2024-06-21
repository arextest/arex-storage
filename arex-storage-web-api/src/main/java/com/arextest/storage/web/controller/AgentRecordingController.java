package com.arextest.storage.web.controller;

import static com.arextest.model.constants.HeaderNames.AREX_MOCK_STRATEGY_CODE;

import com.arextest.common.cache.CacheProvider;
import com.arextest.model.constants.MockAttributeNames;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.model.mock.Mocker.Target;
import com.arextest.model.replay.QueryMockRequestType;
import com.arextest.model.response.Response;
import com.arextest.storage.enums.InvalidReasonEnum;
import com.arextest.storage.metric.AgentWorkingMetricService;
import com.arextest.storage.mock.MockResultContext;
import com.arextest.storage.mock.MockResultMatchStrategy;
import com.arextest.storage.model.InvalidIncompleteRecordRequest;
import com.arextest.storage.serialization.ZstdJacksonSerializer;
import com.arextest.storage.service.AgentWorkingService;
import com.arextest.storage.trace.MDCTracer;
import com.fasterxml.jackson.core.type.TypeReference;
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
  private Executor batchSaveExecutor;
  private static final String INVALID_RECORD_REDIS_KEY = "invalidRecord_";
  private static final long FIVE_MINUTES_EXPIRE = 5 * 60L;
  private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

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
      MDCTracer.addTrace(category, requestType);
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
      return agentWorkingService.queryMockers(requestType.getRecordId(),
          requestType.getFieldNames(), requestType.getCategoryTypes());
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
      MDCTracer.addTrace(category, requestType);
      if (isInvalidRecord(requestType.getRecordId())) {
        LOGGER.warn("recordId: {} is invalid", requestType.getRecordId());
        return ResponseUtils.parameterInvalidResponse("invalid mocker");
      }

      boolean saveResult = agentWorkingMetricService.saveRecord(requestType);
      LOGGER.info("agent record save result:{},category:{},recordId:{}", saveResult, category,
          requestType.getRecordId());
      return ResponseUtils.successResponse(saveResult);
    } catch (Exception e) {
      LOGGER.error("save record error: {} from category: {}, recordId: {}",
          e.getMessage(), requestType.getCategoryType(), requestType.getRecordId(), e);
      handleSaveMockerError(requestType);
      return ResponseUtils.exceptionResponse(e.getMessage());
    } finally {
      MDCTracer.clear();
    }
  }

  @PostMapping(value = "/batchSave")
  @ResponseBody
  public Response batchSave(@RequestBody List<AREXMocker> mockers) {
    if (CollectionUtils.isEmpty(mockers)) {
      return ResponseUtils.parameterInvalidResponse("request body is empty");
    }

    try {
      if (isInvalidRecord(mockers.get(0).getRecordId())) {
        LOGGER.warn("recordId: {} is invalid", mockers.get(0).getRecordId());
        return ResponseUtils.parameterInvalidResponse("invalid mocker");
      }

      // Return the results directly to the agent, asynchronous processing process
      CompletableFuture.runAsync(() -> {
        for (AREXMocker mocker : mockers) {
          this.save(mocker);
        }
      }, batchSaveExecutor);
    } catch (Exception e) {
      LOGGER.error("batch save record error: {}", e.getMessage(), e);
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

  @SuppressWarnings("java:S1452, java:S1168")
  @PostMapping(value = "/queryMockersTest", produces = {MediaType.APPLICATION_JSON_VALUE})
  public @ResponseBody
  List<? extends Mocker> queryMockersTest(@RequestBody QueryMockRequestType requestType) {
    try {
      byte[] bytes = this.queryMockers(requestType);
      return zstdJacksonSerializer.deserialize(bytes,
          new TypeReference<List<AREXMocker>>() {
          });
    } catch (RuntimeException runtimeException) {
      LOGGER.error("queryMockersTest error:{} ", runtimeException.getMessage(), runtimeException);
    }
    return null;
  }

  private boolean isInvalidRecord(String recordId) {
    return redisCacheProvider.get(buildInvalidRecordKey(recordId)) != null;
  }

  private void putInvalidRecordInRedis(String recordId) {
    redisCacheProvider.put(buildInvalidRecordKey(recordId), FIVE_MINUTES_EXPIRE, EMPTY_BYTE_ARRAY);
  }

  private byte[] buildInvalidRecordKey(String recordId) {
    return (INVALID_RECORD_REDIS_KEY + recordId).getBytes(StandardCharsets.UTF_8);
  }

  private void handleSaveMockerError(AREXMocker requestType) {
    InvalidIncompleteRecordRequest request = new InvalidIncompleteRecordRequest();
    request.setRecordId(request.getRecordId());
    request.setAppId(request.getAppId());
    request.setReason(InvalidReasonEnum.STORAGE_SAVE_ERROR.getValue());
    this.invalidIncompleteRecord(request);
    putInvalidRecordInRedis(requestType.getRecordId());
  }

}
