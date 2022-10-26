package com.arextest.storage.web.api.service.controller;

import com.arextest.storage.core.service.FixRecordService;
import com.arextest.storage.model.Response;
import com.arextest.storage.model.replay.FixedRecordRequestType;
import com.arextest.storage.model.replay.FixedRecordResponseType;
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
 * Created by rchen9 on 2022/10/19.
 */
@Slf4j
@Controller
@RequestMapping("/api/report/record")
public class ReportController {

    @Resource
    FixRecordService fixRecordService;

    /**
     * fix record
     *
     * @param requestType recordId
     * @return the record content
     * @see ViewRecordResponseType
     */
    @PostMapping(value = "/fixRecord", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @ResponseBody
    public Response fixRecord(@RequestBody FixedRecordRequestType requestType) {
        if (requestType == null) {
            return requestBodyEmptyResponse();
        }
        String recordId = requestType.getRecordId();
        if (StringUtils.isEmpty(recordId)) {
            return emptyRecordIdResponse();
        }

        try {
            FixedRecordResponseType responseType = new FixedRecordResponseType();
            Map<Integer, List<String>> viewResult = fixRecordService.fixRecord(recordId);
            responseType.setRecordResult(viewResult);
            return successResponse(responseType);
        } catch (Throwable throwable) {
            LOGGER.error("queryRecord error:{}, request:{}", throwable.getMessage(), requestType);
            return exceptionResponse(throwable.getMessage());
        }
    }


}
