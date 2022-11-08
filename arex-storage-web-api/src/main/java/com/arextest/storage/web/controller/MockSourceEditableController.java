package com.arextest.storage.web.controller;


import com.arextest.storage.converter.ZstdJacksonMessageConverter;
import com.arextest.storage.model.Response;
import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.header.ResponseStatusType;
import com.arextest.storage.model.mocker.MockItem;
import com.arextest.storage.service.MockSourceEditableService;
import com.arextest.storage.service.PrepareMockResultService;
import com.arextest.storage.repository.ProviderNames;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;


@Controller
@RequestMapping("/api/storage/edit/")
@Slf4j
public class MockSourceEditableController {
    @Resource
    private MockSourceEditableService editableService;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private PrepareMockResultService storageCache;

    @GetMapping(value = "/pinned/{srcRecordId}/{targetRecordId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Response pinned(@PathVariable String srcRecordId, @PathVariable String targetRecordId) {
        return copyTo(ProviderNames.DEFAULT, srcRecordId, ProviderNames.PINNED, targetRecordId);
    }

    @GetMapping(value = "/copy/{srcProviderName}/{srcRecordId}/{targetProviderName}/{targetRecordId}", produces =
            MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Response copyTo(@PathVariable String srcProviderName, @PathVariable String srcRecordId,
                           @PathVariable String targetProviderName, @PathVariable String targetRecordId) {
        if (StringUtils.isEmpty(srcProviderName)) {
            return ResponseUtils.parameterInvalidResponse("The srcProviderName of requested is empty");
        }
        if (StringUtils.isEmpty(srcRecordId)) {
            return ResponseUtils.parameterInvalidResponse("The srcRecordId of requested is empty");
        }
        if (StringUtils.isEmpty(targetProviderName)) {
            return ResponseUtils.parameterInvalidResponse("The targetProviderName of requested is empty");
        }
        if (StringUtils.isEmpty(targetRecordId)) {
            return ResponseUtils.parameterInvalidResponse("The targetRecordId of requested is empty");
        }
        CopyResponseType copyResponseType = new CopyResponseType();
        int count = editableService.copyTo(srcProviderName, srcRecordId, targetProviderName, targetRecordId);
        copyResponseType.setCopied(count);
        return ResponseUtils.successResponse(copyResponseType);
    }

    @GetMapping(value = "/removeAll/{srcProviderName}/{recordId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Response removeAll(@PathVariable String srcProviderName, @PathVariable String recordId) {
        if (StringUtils.isEmpty(recordId)) {
            return ResponseUtils.emptyRecordIdResponse();
        }
        return ResponseUtils.successResponse(editableService.removeAll(srcProviderName, recordId));
    }

    @GetMapping(value = "/remove/{srcProviderName}/{category}/{recordId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Response remove(@PathVariable String srcProviderName, @PathVariable MockCategoryType category,
                           @PathVariable String recordId) {
        if (StringUtils.isEmpty(recordId)) {
            return ResponseUtils.emptyRecordIdResponse();
        }
        return ResponseUtils.successResponse(editableService.remove(srcProviderName, category, recordId));
    }

    /**
     * update special mocker's by whole object
     *
     * @param shortName category shortName
     * @param body      object value
     * @return success true otherwise false
     */
    @PostMapping("/update")
    @ResponseBody
    public Response update(@RequestHeader String srcProviderName,
                           @RequestHeader(name = ZstdJacksonMessageConverter.AREX_MOCKER_CATEGORY_HEADER) String shortName,
                           @RequestBody String body) {
        MockCategoryType category = MockCategoryType.of(shortName);
        if (category == null) {
            LOGGER.warn("update record the category not found {}", shortName);
            return ResponseUtils.parameterInvalidResponse(shortName);
        }
        if (StringUtils.isBlank(body)) {
            LOGGER.warn("update record request body is empty {}", shortName);
            return ResponseUtils.requestBodyEmptyResponse();
        }
        MockItem requestType;
        try {
            requestType = objectMapper.readValue(body, category.getMockImplClassType());
            if (requestType == null) {
                LOGGER.warn("update record deserialize error {}", shortName);
                return ResponseUtils.parameterInvalidResponse(shortName);
            }
            if (StringUtils.isBlank(requestType.getRecordId())) {
                LOGGER.warn("update record the recordId is empty {}", shortName);
                return ResponseUtils.requestBodyEmptyResponse();
            }
            if (StringUtils.isBlank(requestType.getId())) {
                LOGGER.warn("update record the uniqueId is empty {}", shortName);
                return ResponseUtils.requestBodyEmptyResponse();
            }
            boolean updateResult = editableService.update(srcProviderName, category, requestType);
            if (updateResult) {
                storageCache.remove(category, requestType.getRecordId());
            }
            LOGGER.info("update record result:{},category:{},uniqueId:{}", updateResult, shortName, requestType.getId());
            return ResponseUtils.successResponse(updateResult);
        } catch (Throwable throwable) {
            LOGGER.error("update record error:{} from category:{}", throwable.getMessage(), shortName, throwable);
            return ResponseUtils.exceptionResponse(throwable.getMessage());
        }
    }

    @Getter
    @Setter
    private static class CopyResponseType implements Response {
        private ResponseStatusType responseStatusType;
        private int copied;
    }

}