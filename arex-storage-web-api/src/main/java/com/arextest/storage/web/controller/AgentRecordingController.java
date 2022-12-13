package com.arextest.storage.web.controller;

import com.arextest.model.constants.MockAttributeNames;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.model.mock.Mocker.Target;
import com.arextest.model.response.Response;
import com.arextest.storage.mock.MockResultContext;
import com.arextest.storage.mock.MockResultMatchStrategy;
import com.arextest.storage.serialization.ZstdJacksonSerializer;
import com.arextest.storage.service.AgentWorkingService;
import com.arextest.storage.trace.MDCTracer;
import lombok.extern.slf4j.Slf4j;
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

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

import static com.arextest.model.constants.HeaderNames.AREX_MOCK_STRATEGY_CODE;

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


    /**
     * from agent query,means to save the request and try to find a record item as mock result for return.
     *
     * @param requestType the content of request,
     * @return the bytes compress with zstd
     */
    @PostMapping(value = "/query")
    @ResponseBody
    public byte[] query(@RequestBody AREXMocker requestType, @RequestHeader(name = AREX_MOCK_STRATEGY_CODE,
            defaultValue = "0") int strategyCode) {
        try {
            MockCategoryType category = requestType.getCategoryType();
            if (category == null) {
                LOGGER.warn("agent query category not found");
                return ZstdJacksonSerializer.EMPTY_INSTANCE;
            }
            if (category.equals(MockCategoryType.CONFIG_FILE)) {
                return agentWorkingService.queryConfigFile(category, requestType);
            }
            if (StringUtils.isEmpty(requestType.getRecordId())) {
                LOGGER.warn("agent query recordId empty, {}", category);
                return ZstdJacksonSerializer.EMPTY_INSTANCE;
            }
            if (StringUtils.isEmpty(requestType.getReplayId())) {
                LOGGER.warn("agent query replayId is empty,{}, recordId:{}", category, requestType.getRecordId());
                return ZstdJacksonSerializer.EMPTY_INSTANCE;
            }
            MDCTracer.addTrace(category, requestType);
            MockResultContext context = new MockResultContext(MockResultMatchStrategy.of(strategyCode));
            return agentWorkingService.queryMockResult(requestType, context);
        } catch (Throwable throwable) {
            LOGGER.error("query error:{} from category:{}", throwable.getMessage(), requestType, throwable);
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
    @PostMapping("/save")
    @ResponseBody
    public Response save(@RequestBody AREXMocker requestType) {
        MockCategoryType category = requestType.getCategoryType();
        if (category == null || StringUtils.isEmpty(category.getName())) {
            LOGGER.warn("The name of category is empty from agent record save not allowed ,request:{}", requestType);
            return ResponseUtils.parameterInvalidResponse("empty category");
        }
        try {
            MDCTracer.addTrace(category, requestType);

            boolean saveResult = agentWorkingService.saveRecord(requestType);
            LOGGER.info("agent record save result:{},category:{},recordId:{}", saveResult, category,
                    requestType.getRecordId());
            return ResponseUtils.successResponse(saveResult);
        } catch (Throwable throwable) {
            LOGGER.error("save record error:{} from category:{},recordId:{}", throwable.getMessage(), category,
                    requestType.getRecordId(), throwable);
            return ResponseUtils.exceptionResponse(throwable.getMessage());
        } finally {
            MDCTracer.clear();
        }
    }

    @Resource
    private ZstdJacksonSerializer zstdJacksonSerializer;

    @PostMapping(value = "/saveTest", produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    public Response saveTest(@RequestBody AREXMocker body) {
        try {
            return this.save(body);
        } catch (Throwable throwable) {
            LOGGER.error("save record error:{}", throwable.getMessage(), throwable);
        }
        return null;
    }

    @GetMapping(value = "/saveTest/", produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    public Response saveTest(@RequestParam(required = false, defaultValue = "Servlet") String category) {
        return saveTest(arexMocker(MockCategoryType.create(category)));
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

}