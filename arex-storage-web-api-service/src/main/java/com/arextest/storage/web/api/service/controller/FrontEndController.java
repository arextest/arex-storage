package com.arextest.storage.web.api.service.controller;

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

import static com.arextest.storage.web.api.service.controller.ResponseUtils.emptyRecordIdResponse;
import static com.arextest.storage.web.api.service.controller.ResponseUtils.exceptionResponse;
import static com.arextest.storage.web.api.service.controller.ResponseUtils.requestBodyEmptyResponse;
import static com.arextest.storage.web.api.service.controller.ResponseUtils.successResponse;

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
     * show the decoded message of main entry by special recordId
     *
     * @param requestType recordId
     * @return the record content
     * @see ViewRecordResponseType
     */
    @PostMapping(value = "/queryRecord", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @ResponseBody
    public Response queryRecord(@RequestBody QueryRecordRequestType requestType) {
        if (requestType == null) {
            return requestBodyEmptyResponse();
        }
        String recordId = requestType.getRecordId();
        if (StringUtils.isEmpty(recordId)) {
            return emptyRecordIdResponse();
        }
        MDCTracer.addRecordId(recordId);
        try {
            QueryRecordResponseType responseType = new QueryRecordResponseType();
            Map<Integer, List<String>> viewResult = frontEndRecordService.queryRecord(recordId, requestType.getCategoryTypes());
            responseType.setRecordResult(viewResult);
            return successResponse(responseType);
        } catch (Throwable throwable) {
            LOGGER.error("queryRecord error:{},request:{}", throwable.getMessage(), requestType);
            return exceptionResponse(throwable.getMessage());
        } finally {
            MDCTracer.clear();
        }

    }

}
