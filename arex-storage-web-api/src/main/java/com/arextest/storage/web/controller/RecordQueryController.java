package com.arextest.storage.web.controller;

import com.arextest.model.replay.ListRecordCaseRequestType;
import com.arextest.model.replay.PagedRequestType;
import com.arextest.model.response.Response;
import com.arextest.storage.service.RecordService;
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

    private final RecordService recordService;

    public RecordQueryController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping(value = "/countRecord/{appId}")
    @ResponseBody
    public Response countRecordByAppId(@PathVariable String appId) {
        try {
            return ResponseUtils.successResponse(recordService.countRecordByAppId(appId));
        } catch (Throwable throwable) {
            LOGGER.error("countRecordByAppId error:{},appId:{}", throwable.getMessage(), appId);
            return ResponseUtils.exceptionResponse(throwable.getMessage());
        }
    }

    @PostMapping(value = "/listRecord")
    @ResponseBody
    public Response listRecord(@RequestBody PagedRequestType requestType) {
        if (requestType == null) {
            return ResponseUtils.requestBodyEmptyResponse();
        }
        if (requestType.getPageSize() <= 0 || requestType.getPageSize() >= 1000) {
            return ResponseUtils.parameterInvalidResponse("illegal pageSize!");
        }
        try {
            return ResponseUtils.successResponse(recordService.listRecordCase(requestType));
        } catch (Throwable throwable) {
            LOGGER.error("listRecord error:{},request:{}", throwable.getMessage(), requestType);
            return ResponseUtils.exceptionResponse(throwable.getMessage());
        }
    }
}
