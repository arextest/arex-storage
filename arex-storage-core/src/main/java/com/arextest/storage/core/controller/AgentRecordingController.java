package com.arextest.storage.core.controller;

import com.arextest.storage.core.converter.ZstdJacksonMessageConverter;
import com.arextest.storage.core.serialization.ZstdJacksonSerializer;
import com.arextest.storage.core.service.AgentWorkingService;
import com.arextest.storage.core.trace.MDCTracer;
import com.arextest.storage.model.Response;
import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.enums.RecordEnvType;
import com.arextest.storage.model.mocker.ConfigVersion;
import com.arextest.storage.model.mocker.MainEntry;
import com.arextest.storage.model.mocker.MockItem;
import com.arextest.storage.model.mocker.impl.ConfigVersionMocker;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

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
    @Value("${arex.record.env}")
    private RecordEnvType recordEnvType;
    @Resource
    private AgentWorkingService agentWorkingService;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * from agent query,means to save the request and try find record item as mock result for return.
     *
     * @param requestType the content of request,
     * @param shortName   the category name of mock item
     * @return the bytes compress with zstd
     */
    @PostMapping(value = "/query")
    @ResponseBody
    public byte[] query(@RequestHeader(name = ZstdJacksonMessageConverter.AREX_MOCKER_CATEGORY_HEADER) String shortName,
                        @RequestBody MockItem requestType) {
        try {
            MockCategoryType category = MockCategoryType.of(shortName);
            if (category == null || requestType == null) {
                LOGGER.warn("agent query category not found {}", shortName);
                return ZstdJacksonSerializer.EMPTY_INSTANCE;
            }
            if (category.isConfigVersion() && requestType instanceof ConfigVersion) {
                return agentWorkingService.queryConfigVersion(category, (ConfigVersion) requestType);
            }
            if (StringUtils.isEmpty(requestType.getRecordId())) {
                LOGGER.warn("agent query recordId empty, {}", shortName);
                return ZstdJacksonSerializer.EMPTY_INSTANCE;
            }
            if (StringUtils.isEmpty(requestType.getReplayId())) {
                LOGGER.warn("agent query replayId is empty,{}, recordId:{}", shortName, requestType.getRecordId());
                return ZstdJacksonSerializer.EMPTY_INSTANCE;
            }
            MDCTracer.addTrace(category, requestType);
            return agentWorkingService.queryMockResult(category, requestType);
        } catch (Throwable throwable) {
            LOGGER.error("query error:{} from category:{}", throwable.getMessage(), shortName, throwable);
        } finally {
            MDCTracer.clear();
        }
        return ZstdJacksonSerializer.EMPTY_INSTANCE;
    }

    /**
     * from agent recording, save the content for replay
     *
     * @param shortName   the category name of mock item
     * @param requestType the record content of request
     * @return response for save result
     */
    @PostMapping("/save")
    @ResponseBody
    public Response save(@RequestHeader(name = ZstdJacksonMessageConverter.AREX_MOCKER_CATEGORY_HEADER) String shortName,
                         @RequestBody MockItem requestType) {
        MockCategoryType category = MockCategoryType.of(shortName);
        if (category == null || requestType == null) {
            LOGGER.warn("agent record save the category not found {}", shortName);
            return ResponseUtils.parameterInvalidResponse(shortName);
        }
        if (!category.isConfigVersion() && StringUtils.isEmpty(requestType.getRecordId())) {
            LOGGER.warn("agent record save the recordId is empty,category:{}", shortName);
            return ResponseUtils.emptyRecordIdResponse();
        }
        try {
            MDCTracer.addTrace(category, requestType);
            markRecordEnv(requestType);
            boolean saveResult = agentWorkingService.saveRecord(category, requestType);
            LOGGER.info("agent record save result:{},category:{},recordId:{}", saveResult, shortName,
                    requestType.getRecordId());
            return ResponseUtils.successResponse(saveResult);
        } catch (Throwable throwable) {
            LOGGER.error("save record error:{} from category:{},recordId:{}", throwable.getMessage(), shortName,
                    requestType.getRecordId(), throwable);
            return ResponseUtils.exceptionResponse(throwable.getMessage());
        } finally {
            MDCTracer.clear();
        }
    }

    private void markRecordEnv(MockItem requestType) {
        if (requestType instanceof MainEntry) {
            ((MainEntry) requestType).setEnv(recordEnvType.getCodeValue());
        }
    }

    /**
     * requested to build version key
     *
     * @param application the appId
     * @return ConfigVersionMocker bytes
     */
    @PostMapping(value = "/queryConfigVersionKey")
    @ResponseBody
    public byte[] queryConfigVersionKey(@RequestBody ConfigVersionMocker application) {
        return agentWorkingService.queryConfigVersionKey(application);
    }

    @Resource
    private ZstdJacksonSerializer zstdJacksonSerializer;

    @PostMapping("/saveTest")
    @ResponseBody
    public Response saveTest(@RequestBody String body, @RequestHeader String categoryName) {
        try {
            MockCategoryType categoryType = MockCategoryType.of(categoryName);
            MockItem requestType = objectMapper.readValue(body, categoryType.getMockImplClassType());
            return this.save(categoryName, requestType);
        } catch (Throwable throwable) {
            LOGGER.error("save record error:{} from category:{}", throwable.getMessage(), categoryName, throwable);
        }
        return null;
    }

    @PostMapping("/queryTest")
    public @ResponseBody
    MockItem queryTest(@RequestBody String body, @RequestHeader String categoryName) {
        try {
            MockCategoryType categoryType = MockCategoryType.of(categoryName);
            MockItem requestType = objectMapper.readValue(body, categoryType.getMockImplClassType());
            byte[] bytes = this.query(categoryName, requestType);
            return zstdJacksonSerializer.deserialize(bytes, categoryType.getMockImplClassType());
        } catch (Throwable throwable) {
            LOGGER.error("queryTest error:{} from category: {}", throwable.getMessage(), categoryName, throwable);
        }
        return null;
    }

}