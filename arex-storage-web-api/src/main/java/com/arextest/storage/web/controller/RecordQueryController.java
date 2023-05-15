package com.arextest.storage.web.controller;

import com.arextest.model.replay.ListRecordCaseRequestType;
import com.arextest.model.response.Response;
import com.arextest.storage.service.RecordQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@RequestMapping(path = "api/storage/record/query", produces = {MediaType.APPLICATION_JSON_VALUE})
public class RecordQueryController {

    private final RecordQueryService recordQueryService;

    public RecordQueryController(RecordQueryService recordQueryService) {
        this.recordQueryService = recordQueryService;
    }

    @GetMapping(value = "/countRecord/{appId}")
    @ResponseBody
    public Response countRecordByAppId(@PathVariable String appId) {
        try {
            return ResponseUtils.successResponse(recordQueryService.countRecordByAppId(appId));
        } catch (Throwable throwable) {
            LOGGER.error("countRecordByAppId error:{},appId:{}", throwable.getMessage(), appId);
            return ResponseUtils.exceptionResponse(throwable.getMessage());
        }
    }

    @PostMapping(value = "/listRecord")
    @ResponseBody
    public Response listRecord(@RequestBody ListRecordCaseRequestType requestType) {
        if (requestType == null) {
            return ResponseUtils.requestBodyEmptyResponse();
        }
        if (requestType.getPageSize() <= 0 || requestType.getPageSize() >= 1000) {
            return ResponseUtils.parameterInvalidResponse("illegal pageSize!");
        }
        try {
            return ResponseUtils.successResponse(recordQueryService.listRecordCase(requestType));
        } catch (Throwable throwable) {
            LOGGER.error("listRecord error:{},request:{}", throwable.getMessage(), requestType);
            return ResponseUtils.exceptionResponse(throwable.getMessage());
        }
    }
}
