package com.arextest.storage.web.controller;

import com.arextest.model.replay.ListRecordCaseRequestType;
import com.arextest.model.response.Response;
import com.arextest.storage.service.RecordQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

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

        try {
            return ResponseUtils.successResponse(recordQueryService.listRecordCase(requestType));
        } catch (Throwable throwable) {
            LOGGER.error("listRecord error:{},request:{}", throwable.getMessage(), requestType);
            return ResponseUtils.exceptionResponse(throwable.getMessage());
        }
    }
}
