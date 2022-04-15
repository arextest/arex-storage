package io.arex.storage.web.api.service.controller;

import io.arex.storage.core.service.PrepareMockResultService;
import io.arex.storage.core.service.RecordReplayMappingBuilder;
import io.arex.storage.core.service.ScheduleReplayingService;
import io.arex.storage.core.trace.MDCTracer;
import io.arex.storage.model.Response;
import io.arex.storage.model.enums.MockCategoryType;
import io.arex.storage.model.mocker.MainEntry;
import io.arex.storage.model.replay.PagingQueryCaseRequestType;
import io.arex.storage.model.replay.PagingQueryCaseResponseType;
import io.arex.storage.model.replay.QueryCaseCountRequestType;
import io.arex.storage.model.replay.QueryCaseCountResponseType;
import io.arex.storage.model.replay.QueryMockCacheRequestType;
import io.arex.storage.model.replay.QueryMockCacheResponseType;
import io.arex.storage.model.replay.QueryReplayResultRequestType;
import io.arex.storage.model.replay.QueryReplayResultResponseType;
import io.arex.storage.model.replay.ReplayCaseRangeRequestType;
import io.arex.storage.model.replay.ViewRecordRequestType;
import io.arex.storage.model.replay.ViewRecordResponseType;
import io.arex.storage.model.replay.holder.ListResultHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

import static io.arex.storage.web.api.service.controller.ResponseUtils.*;

/**
 * this class defined all api list for scheduler replaying
 *
 * @author jmo
 * @since 2021/11/3
 */
@Slf4j
@Controller
@RequestMapping("/api/storage/replay/query")
public final class ScheduleReplayQueryController {
    @Resource
    private ScheduleReplayingService scheduleReplayingService;
    @Resource
    private PrepareMockResultService prepareMockResultService;
    @Resource
    private RecordReplayMappingBuilder recordReplayMappingBuilder;
    private static final long MAIN_CATEGORY_TYPES = mainCategoryMasks();

    /**
     * fetch the replay result for compare
     *
     * @param requestType which record id & replay id should be fetch.
     * @return response
     * @see QueryReplayResultResponseType
     */
    @PostMapping(value = "/replayResult", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @ResponseBody
    public Response replayResult(@RequestBody QueryReplayResultRequestType requestType) {
        if (requestType == null) {
            return requestBodyEmptyResponse();
        }
        final String recordId = requestType.getRecordId();
        if (StringUtils.isEmpty(recordId)) {
            return emptyRecordIdResponse();
        }
        String replayResultId = requestType.getReplayResultId();
        try {
            MDCTracer.addRecordId(recordId);
            if (StringUtils.isEmpty(replayResultId)) {
                replayResultId = recordReplayMappingBuilder.lastReplayResultId(MockCategoryType.QMQ_CONSUMER, recordId);
            }
            if (StringUtils.isEmpty(replayResultId)) {
                return emptyReplayResultIdResponse();
            }
            MDCTracer.addReplayId(replayResultId);

            List<ListResultHolder<String>> resultHolderList = scheduleReplayingService.queryReplayResult(recordId,
                    replayResultId);
            QueryReplayResultResponseType responseType = new QueryReplayResultResponseType();
            responseType.setResultHolderList(resultHolderList);
            return successResponse(responseType);
        } catch (Throwable throwable) {
            LOGGER.error("replayResult error:{} ,recordId:{} ,replayResultId:{}", throwable.getMessage(), recordId,
                    replayResultId);
            return exceptionResponse(throwable.getMessage());
        } finally {
            MDCTracer.clear();
        }
    }

    /**
     * a record list by special query to replay
     *
     * @param requestType range query
     * @return response
     * @see PagingQueryCaseResponseType
     */
    @SuppressWarnings("unchecked")
    @PostMapping(value = "/replayCase")
    @ResponseBody
    public Response replayCase(@RequestBody PagingQueryCaseRequestType requestType) {
        Response validateResult = rangeParameterValidate(requestType);
        if (validateResult != null) {
            return validateResult;
        }
        if (requestType.getMaxCaseCount() <= 0) {
            return parameterInvalidResponse("The max case size <= 0 from requested");
        }
        MockCategoryType categoryType = MockCategoryType.of(requestType.getCategoryType());
        if (categoryType == null) {
            return parameterInvalidResponse("request category type not found");
        }
        if (!categoryType.isMainEntry()) {
            return parameterInvalidResponse("request category type not main entry:" + categoryType.getDisplayName());
        }
        try {
            PagingQueryCaseResponseType responseType = new PagingQueryCaseResponseType();
            Iterable<?> iterable = scheduleReplayingService.pagingQueryReplayCaseList(categoryType,
                    requestType);
            List<?> mainList = new IterableListWrapper<>(iterable);
            responseType.setMainEntryList((List<? extends MainEntry>) mainList);
            return successResponse(responseType);
        } catch (Throwable throwable) {
            LOGGER.error("error:{},request:{}", throwable.getMessage(), requestType);
            return exceptionResponse(throwable.getMessage());
        }
    }

    private Response rangeParameterValidate(ReplayCaseRangeRequestType requestType) {
        if (requestType == null) {
            return requestBodyEmptyResponse();
        }
        if (StringUtils.isEmpty(requestType.getAppId())) {
            return parameterInvalidResponse("The appId of requested is empty");
        }
        if (requestType.getBeginTime() == null) {
            return parameterInvalidResponse("The beginTime of requested is null");
        }
        if (requestType.getEndTime() == null) {
            return parameterInvalidResponse("The endTime of requested is null");
        }
        if (requestType.getBeginTime() >= requestType.getEndTime()) {
            return parameterInvalidResponse("The beginTime >= endTime from requested");
        }
        return null;
    }

    /**
     * count for query how many records should be preload to replay
     *
     * @param requestType range query
     * @return a size value
     * @see QueryCaseCountResponseType
     */
    @PostMapping(value = "/countByRange", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @ResponseBody
    public Response countByRange(@RequestBody QueryCaseCountRequestType requestType) {
        Response validateResult = rangeParameterValidate(requestType);
        if (validateResult != null) {
            return validateResult;
        }
        MockCategoryType categoryType = MockCategoryType.of(requestType.getCategoryType());
        if (categoryType == null) {
            return parameterInvalidResponse("request category type not found");
        }
        try {
            QueryCaseCountResponseType responseType = new QueryCaseCountResponseType();
            int countResult = scheduleReplayingService.countByRange(categoryType, requestType);
            responseType.setCount(countResult);
            return successResponse(responseType);
        } catch (Throwable throwable) {
            LOGGER.error("replayCaseCount error:{},request:{}", throwable.getMessage(), requestType, throwable);
            return exceptionResponse(throwable.getMessage());
        } finally {
            MDCTracer.clear();
        }
    }

    /**
     * show the main entry by special recordId
     *
     * @param requestType recordId
     * @return the record content
     * @see ViewRecordResponseType
     */
    @PostMapping("/viewRecord")
    @ResponseBody
    public Response viewRecord(@RequestBody ViewRecordRequestType requestType) {
        if (requestType == null) {
            return requestBodyEmptyResponse();
        }
        String recordId = requestType.getRecordId();
        if (StringUtils.isEmpty(recordId)) {
            return emptyRecordIdResponse();
        }
        MDCTracer.addRecordId(recordId);
        try {
            prepareMockResultService.preloadAll(recordId);
            ViewRecordResponseType responseType = new ViewRecordResponseType();
            long categoryTypes = MAIN_CATEGORY_TYPES;
            if (requestType.getCategoryTypes() != null) {
                categoryTypes = requestType.getCategoryTypes();
            }
            Map<Integer, List<String>> viewResult = scheduleReplayingService.queryRecordResult(recordId, categoryTypes);
            if (MapUtils.isEmpty(viewResult)) {
                LOGGER.info("viewRecord not found any resource recordId: {} ,request: {}", recordId, requestType);
            }
            responseType.setRecordResult(viewResult);
            return successResponse(responseType);
        } catch (Throwable throwable) {
            LOGGER.error("viewRecord error:{},request:{}", throwable.getMessage(), requestType);
            return exceptionResponse(throwable.getMessage());
        } finally {
            MDCTracer.clear();
        }

    }

    /**
     * preload record content to cache from repository provider
     *
     * @param requestType recordId
     * @return true loaded success
     */
    @PostMapping(value = "/cacheLoad", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @ResponseBody
    public Response cacheLoad(@RequestBody QueryMockCacheRequestType requestType) {
        if (requestType == null) {
            return requestBodyEmptyResponse();
        }
        String recordId = requestType.getRecordId();
        if (StringUtils.isEmpty(recordId)) {
            return emptyRecordIdResponse();
        }
        MDCTracer.addRecordId(recordId);
        long beginTime = System.currentTimeMillis();
        try {
            return toResponse(prepareMockResultService.preloadAll(recordId));
        } catch (Throwable throwable) {
            LOGGER.error("QueryMockCache error:{},request:{}", throwable.getMessage(), requestType);
            return exceptionResponse(throwable.getMessage());
        } finally {
            long timeUsed = System.currentTimeMillis() - beginTime;
            LOGGER.info("cacheLoad timeUsed:{} ms,record id:{}", timeUsed, recordId);
            MDCTracer.clear();
        }
    }

    /**
     * initiative clean the cache
     *
     * @param requestType the recordId
     * @return true remove success
     */
    @PostMapping(value = "/cacheRemove", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @ResponseBody
    public Response cacheRemove(@RequestBody QueryMockCacheRequestType requestType) {
        if (requestType == null) {
            return requestBodyEmptyResponse();
        }
        String recordId = requestType.getRecordId();
        if (StringUtils.isEmpty(recordId)) {
            return emptyRecordIdResponse();
        }
        MDCTracer.addRecordId(recordId);
        try {
            return toResponse(prepareMockResultService.removeAll(recordId));
        } catch (Throwable throwable) {
            LOGGER.error("QueryMockCache error:{},request:{}", throwable.getMessage(), requestType);
            return exceptionResponse(throwable.getMessage());
        } finally {
            MDCTracer.clear();
        }
    }

    private Response toResponse(boolean actionResult) {
        return actionResult ? successResponse(new QueryMockCacheResponseType()) : resourceNotFoundResponse();
    }

    private static long mainCategoryMasks() {
        long mainCategoryValue = 0;
        for (MockCategoryType categoryType : MockCategoryType.values()) {
            if (categoryType.isMainEntry()) {
                mainCategoryValue |= 1 << categoryType.getCodeValue();
            }
        }
        return mainCategoryValue;
    }
}
