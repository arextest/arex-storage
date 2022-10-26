package com.arextest.storage.core.controller;

import com.arextest.storage.core.service.FrontEndRecordService;
import com.arextest.storage.core.trace.MDCTracer;
import com.arextest.storage.model.Response;
import com.arextest.storage.model.replay.QueryRecordRequestType;
import com.arextest.storage.model.replay.QueryRecordResponseType;
import com.arextest.storage.model.replay.ViewRecordResponseType;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Created by rchen9 on 2022/10/11.
 */
@Slf4j
@Controller
@RequestMapping("/api/frontEnd/record")
public class FrontEndController {

    @Resource
    FrontEndRecordService frontEndRecordService;

    /**
     * query the fixed record
     *
     * @param requestType recordId
     * @return the record content
     * @see ViewRecordResponseType
     */
    @PostMapping(value = "/queryFixedRecord", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @ResponseBody
    public Response queryFixedRecord(@RequestBody QueryRecordRequestType requestType) {
        if (requestType == null) {
            return ResponseUtils.requestBodyEmptyResponse();
        }
        String recordId = requestType.getRecordId();
        if (StringUtils.isEmpty(recordId)) {
            return ResponseUtils.emptyRecordIdResponse();
        }
        MDCTracer.addRecordId(recordId);
        try {
            QueryRecordResponseType responseType = new QueryRecordResponseType();
            Map<Integer, List<String>> viewResult = frontEndRecordService.queryFixedRecord(recordId,
                    requestType.getCategoryTypes());
            responseType.setRecordResult(viewResult);
            return ResponseUtils.successResponse(responseType);
        } catch (Throwable throwable) {
            LOGGER.error("queryRecord error:{},request:{}", throwable.getMessage(), requestType);
            return ResponseUtils.exceptionResponse(throwable.getMessage());
        } finally {
            MDCTracer.clear();
        }
    }

}