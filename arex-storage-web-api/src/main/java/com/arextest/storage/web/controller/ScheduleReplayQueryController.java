package com.arextest.storage.web.controller;

import com.arextest.common.annotation.AppAuth;
import com.arextest.common.context.ArexContext;
import com.arextest.common.enums.AuthRejectStrategy;
import com.arextest.common.utils.JsonTraverseUtils;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.Mocker;
import com.arextest.model.replay.*;
import com.arextest.model.replay.holder.ListResultHolder;
import com.arextest.model.response.Response;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.service.PrepareMockResultService;
import com.arextest.storage.service.ScheduleReplayingService;
import com.arextest.storage.trace.MDCTracer;
import com.arextest.storage.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * this class defined all api list for scheduler replaying
 *
 * @author jmo
 * @since 2021/11/3
 */
@Slf4j
@RequestMapping(path = "/api/storage/replay/query", produces = {MediaType.APPLICATION_JSON_VALUE})
public class ScheduleReplayQueryController {

    private final ScheduleReplayingService scheduleReplayingService;

    private final PrepareMockResultService prepareMockResultService;

    public ScheduleReplayQueryController(ScheduleReplayingService scheduleReplayingService,
                                         PrepareMockResultService prepareMockResultService) {
        this.scheduleReplayingService = scheduleReplayingService;
        this.prepareMockResultService = prepareMockResultService;
    }

    /**
     * fetch the replay result for compare
     *
     * @param requestType which record id &amp; replay id should be fetch.
     * @return response
     * @see QueryReplayResultResponseType
     */
    @PostMapping(value = "/replayResult")
    @ResponseBody
    public Response replayResult(@RequestBody QueryReplayResultRequestType requestType) {
        if (requestType == null) {
            return ResponseUtils.requestBodyEmptyResponse();
        }
        final String recordId = requestType.getRecordId();
        if (StringUtils.isEmpty(recordId)) {
            return ResponseUtils.emptyRecordIdResponse();
        }
        String replayResultId = requestType.getReplayResultId();
        if (StringUtils.isEmpty(replayResultId)) {
            return ResponseUtils.emptyReplayResultIdResponse();
        }
        try {
            MDCTracer.addRecordId(recordId);
            MDCTracer.addReplayId(replayResultId);
            List<ListResultHolder> resultHolderList =
                    scheduleReplayingService.queryReplayResult(recordId, replayResultId);
            QueryReplayResultResponseType responseType = new QueryReplayResultResponseType();
            responseType.setResultHolderList(resultHolderList);
            return ResponseUtils.successResponse(responseType);
        } catch (Throwable throwable) {
            LOGGER.error("replayResult error:{} ,recordId:{} ,replayResultId:{}",
                    throwable.getMessage(),
                    recordId,
                    replayResultId);
            return ResponseUtils.exceptionResponse(throwable.getMessage());
        } finally {
            MDCTracer.clear();
        }
    }

    /**
     * a record list by special query to replay
     *
     * @param requestType range query
     * @return response
     * @see PagedResponseType
     */
    @PostMapping(value = "/replayCase")
    @ResponseBody
    public Response replayCase(@RequestBody PagedRequestType requestType) {
        Response validateResult = rangeParameterValidate(requestType);
        if (validateResult != null) {
            return validateResult;
        }

        validateResult = pageParameterValidate(requestType);
        if (validateResult != null) {
            return validateResult;
        }

        try {
            PagedResponseType responseType = new PagedResponseType();
            responseType.setRecords(scheduleReplayingService.queryEntryPointByRange(requestType));
            return ResponseUtils.successResponse(responseType);
        } catch (Throwable throwable) {
            LOGGER.error("error:{},request:{}", throwable.getMessage(), requestType);
            return ResponseUtils.exceptionResponse(throwable.getMessage());
        }
    }

    private Response rangeParameterValidate(PagedRequestType requestType) {
        if (requestType == null) {
            return ResponseUtils.requestBodyEmptyResponse();
        }
        if (StringUtils.isEmpty(requestType.getAppId())) {
            return ResponseUtils.parameterInvalidResponse("The appId of requested is empty");
        }
        if (requestType.getBeginTime() == null) {
            return ResponseUtils.parameterInvalidResponse("The beginTime of requested is null");
        }
        if (requestType.getEndTime() == null) {
            return ResponseUtils.parameterInvalidResponse("The endTime of requested is null");
        }
        if (requestType.getBeginTime() >= requestType.getEndTime()) {
            return ResponseUtils.parameterInvalidResponse("The beginTime >= endTime from requested");
        }
        return null;
    }

    private Response pageParameterValidate(PagedRequestType requestType) {
        if (requestType.getPageSize() <= 0) {
            return ResponseUtils.parameterInvalidResponse("The max case size <= 0 from requested");
        }
        if (requestType.getCategory() == null) {
            return ResponseUtils.parameterInvalidResponse("The category of requested is empty");
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
    @PostMapping(value = "/countByRange")
    @ResponseBody
    public Response countByRange(@RequestBody QueryCaseCountRequestType requestType) {
        Response validateResult = rangeParameterValidate(requestType);
        if (validateResult != null) {
            return validateResult;
        }
        try {
            QueryCaseCountResponseType responseType = new QueryCaseCountResponseType();
            long countResult = scheduleReplayingService.countByRange(requestType);
            responseType.setCount(countResult);
            return ResponseUtils.successResponse(responseType);
        } catch (Throwable throwable) {
            LOGGER.error("replayCaseCount error:{},request:{}", throwable.getMessage(), requestType, throwable);
            return ResponseUtils.exceptionResponse(throwable.getMessage());
        } finally {
            MDCTracer.clear();
        }
    }

    /**
     * count records cases for each operationName.
     */
    @PostMapping(value = "/countByOperationName")
    @ResponseBody
    public Response countByOperationName (@RequestBody CountOperationCaseRequestType requestType) {
        Response validateResult = rangeParameterValidate(requestType);
        if (validateResult != null) {
            return validateResult;
        }
        try {
            CountOperationCaseResponseType responseType = new CountOperationCaseResponseType();
            Map<String, Long> countResult = scheduleReplayingService.countByOperationName(requestType);
            responseType.setCountMap(countResult);
            return ResponseUtils.successResponse(responseType);
        } catch (Throwable throwable) {
            LOGGER.error("countByOperationName  error:{},request:{}", throwable.getMessage(), requestType, throwable);
            return ResponseUtils.exceptionResponse(throwable.getMessage());
        } finally {
            MDCTracer.clear();
        }
    }

    @ResponseBody
    @GetMapping(value = "/viewRecord/")
    @AppAuth(rejectStrategy = AuthRejectStrategy.DOWNGRADE)
    public Response viewRecord(String recordId,
                               @RequestParam(required = false) String category,
                               @RequestParam(required = false, defaultValue = ProviderNames.DEFAULT) String srcProvider) {
        ViewRecordRequestType recordRequestType = new ViewRecordRequestType();
        recordRequestType.setRecordId(recordId);
        recordRequestType.setSourceProvider(srcProvider);
        recordRequestType.setCategoryType(category);
        return viewRecord(recordRequestType);
    }

    /**
     * show the all records (includes entryPoint &amp; dependencies) by special recordId
     *
     * @param requestType recordId
     * @return the record content
     * @see ViewRecordResponseType
     */
    @PostMapping("/viewRecord")
    @ResponseBody
    @AppAuth(rejectStrategy = AuthRejectStrategy.DOWNGRADE)
    public Response viewRecord(@RequestBody ViewRecordRequestType requestType) {
        if (requestType == null) {
            return ResponseUtils.requestBodyEmptyResponse();
        }
        String recordId = requestType.getRecordId();
        if (StringUtils.isEmpty(recordId)) {
            return ResponseUtils.emptyRecordIdResponse();
        }
        MDCTracer.addRecordId(recordId);
        try {
            ViewRecordResponseType responseType = new ViewRecordResponseType();
            List<AREXMocker> allReadableResult = scheduleReplayingService.queryRecordList(requestType);
            if (CollectionUtils.isEmpty(allReadableResult)) {
                LOGGER.info("could not found any resources for request: {}", requestType);
            }
            responseType.setRecordResult(allReadableResult);
            responseDesensitization(responseType, allReadableResult);
            return ResponseUtils.successResponse(responseType);
        } catch (Throwable throwable) {
            LOGGER.error("viewRecord error:{}, request:{}", throwable.getMessage(), requestType);
            return ResponseUtils.exceptionResponse(throwable.getMessage());
        } finally {
            MDCTracer.clear();
        }
    }

    private static void responseDesensitization(ViewRecordResponseType responseType, List<AREXMocker> allReadableResult) {
        if (!Boolean.TRUE.equals(ArexContext.getContext().getPassAuth())) {
            try {
                responseType.setDesensitized(true);
                for (AREXMocker arexMocker : allReadableResult) {
                    Mocker.Target request = arexMocker.getTargetRequest();
                    Mocker.Target response = arexMocker.getTargetResponse();

                    if (JsonUtil.isJsonStr(request.getBody())) {
                        request.setBody(JsonTraverseUtils.trimAllLeaves(request.getBody()));
                    }
                    if (JsonUtil.isJsonStr(response.getBody())) {
                        response.setBody(JsonTraverseUtils.trimAllLeaves(response.getBody()));
                    }
                    Optional.ofNullable(response.getAttributes())
                            .ifPresent(attributes -> attributes.entrySet().forEach(entry -> entry.setValue(null)));
                    Optional.ofNullable(request.getAttributes())
                            .ifPresent(attributes -> attributes.entrySet().forEach(entry -> entry.setValue(null)));
                }
            } catch (Exception e) {
                responseType.setDesensitized(false);
                LOGGER.error("trimAllLeaves error", e);
            }
        }
    }

    /**
     * preload record content to cache from repository provider
     *
     * @param requestType recordId
     * @return true loaded success
     */
    @PostMapping(value = "/cacheLoad")
    @ResponseBody
    public Response cacheLoad(@RequestBody QueryMockCacheRequestType requestType) {
        if (requestType == null) {
            return ResponseUtils.requestBodyEmptyResponse();
        }
        String recordId = requestType.getRecordId();
        if (StringUtils.isEmpty(recordId)) {
            return ResponseUtils.emptyRecordIdResponse();
        }
        MDCTracer.addRecordId(recordId);
        long beginTime = System.currentTimeMillis();
        try {
            return toResponse(prepareMockResultService.preloadAll(requestType.getSourceProvider(), recordId));
        } catch (Throwable throwable) {
            LOGGER.error("QueryMockCache error:{},request:{}", throwable.getMessage(), requestType);
            return ResponseUtils.exceptionResponse(throwable.getMessage());
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
    @PostMapping(value = "/cacheRemove")
    @ResponseBody
    public Response cacheRemove(@RequestBody QueryMockCacheRequestType requestType) {
        if (requestType == null) {
            return ResponseUtils.requestBodyEmptyResponse();
        }
        String recordId = requestType.getRecordId();
        if (StringUtils.isEmpty(recordId)) {
            return ResponseUtils.emptyRecordIdResponse();
        }
        MDCTracer.addRecordId(recordId);
        try {
            return toResponse(prepareMockResultService.removeAll(recordId));
        } catch (Throwable throwable) {
            LOGGER.error("QueryMockCache error:{},request:{}", throwable.getMessage(), requestType);
            return ResponseUtils.exceptionResponse(throwable.getMessage());
        } finally {
            MDCTracer.clear();
        }
    }

    private Response toResponse(boolean actionResult) {
        return actionResult ?
                ResponseUtils.successResponse(new QueryMockCacheResponseType()) :
                ResponseUtils.resourceNotFoundResponse();
    }
}
