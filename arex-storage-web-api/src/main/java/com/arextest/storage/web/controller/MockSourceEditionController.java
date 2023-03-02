package com.arextest.storage.web.controller;


import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.response.Response;
import com.arextest.model.response.ResponseStatusType;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.service.MockSourceEditionService;
import com.arextest.storage.service.PrepareMockResultService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;


@RequestMapping(path = "/api/storage/edit/", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class MockSourceEditionController {

    private final MockSourceEditionService editableService;

    private final PrepareMockResultService storageCache;

    public MockSourceEditionController(MockSourceEditionService editableService, PrepareMockResultService storageCache) {
        this.editableService = editableService;
        this.storageCache = storageCache;
    }

    @GetMapping(value = "/pinned/{srcRecordId}/{targetRecordId}/")
    @ResponseBody
    public Response pinned(@PathVariable String srcRecordId, @PathVariable String targetRecordId) {
        return copyTo(ProviderNames.DEFAULT, srcRecordId, ProviderNames.PINNED, targetRecordId);
    }

    @GetMapping(value = "/copy/")
    @ResponseBody
    public Response copyTo(String srcProviderName, String srcRecordId, String targetProviderName, String targetRecordId) {
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

    @GetMapping(value = "/removeAll/")
    @ResponseBody
    public Response removeAll(@RequestParam(required = false, defaultValue = ProviderNames.DEFAULT) String srcProviderName, String recordId) {
        if (StringUtils.isEmpty(recordId)) {
            return ResponseUtils.emptyRecordIdResponse();
        }
        return ResponseUtils.successResponse(editableService.removeAll(srcProviderName, recordId));
    }

    @GetMapping(value = "/remove/")
    @ResponseBody
    public Response remove(@RequestParam(required = false, defaultValue = ProviderNames.DEFAULT) String srcProviderName,
                           String category,
                           String recordId) {
        if (StringUtils.isEmpty(recordId)) {
            return ResponseUtils.emptyRecordIdResponse();
        }
        if (StringUtils.isEmpty(category)) {
            return ResponseUtils.parameterInvalidResponse("The category of requested is empty");
        }
        return ResponseUtils.successResponse(editableService.remove(srcProviderName, category, recordId));
    }

    @PostMapping("/pinned/add/")
    @ResponseBody
    public Response pinnedAdd(@RequestBody AREXMocker body) {
        return this.add(ProviderNames.PINNED, body);
    }

    /**
     * add special mocker's by whole object
     *
     * @param body object value
     * @return success true otherwise false
     */
    @PostMapping("/add/{srcProviderName}/")
    @ResponseBody
    public Response add(@PathVariable String srcProviderName, @RequestBody AREXMocker body) {
        Response response = checkRequiredParameters(srcProviderName, body);
        if (response != null) {
            return response;
        }
        MockCategoryType category = body.getCategoryType();
        try {
            boolean updateResult = editableService.add(srcProviderName, body);
            LOGGER.info("add record result:{},category:{},id:{}", updateResult, category, body.getId());
            return ResponseUtils.successResponse(updateResult);
        } catch (Throwable throwable) {
            LOGGER.error("add record error:{} from category:{}", throwable.getMessage(), category, throwable);
            return ResponseUtils.exceptionResponse(throwable.getMessage());
        }
    }

    /**
     * update special pinned mocker's by whole object
     *
     * @param body object value
     * @return success true otherwise false
     */
    @PostMapping("/pinned/update/")
    @ResponseBody
    public Response pinnedUpdate(@RequestBody AREXMocker body) {
        return this.update(ProviderNames.PINNED, body);
    }

    /**
     * update special mocker's by whole object
     *
     * @param body object value
     * @return success true otherwise false
     */
    @PostMapping("/update/")
    @ResponseBody
    public Response update(@RequestHeader String srcProviderName, @RequestBody AREXMocker body) {
        Response response = checkRequiredParameters(srcProviderName, body);
        if (response != null) {
            return response;
        }
        MockCategoryType category = body.getCategoryType();

        try {
            boolean updateResult = editableService.update(srcProviderName, body);
            if (updateResult) {
                storageCache.remove(category, body.getRecordId());
            }
            LOGGER.info("update record result:{},category:{},uniqueId:{}", updateResult, category, body.getId());
            return ResponseUtils.successResponse(updateResult);
        } catch (Throwable throwable) {
            LOGGER.error("update record error:{} from category:{}", throwable.getMessage(), category, throwable);
            return ResponseUtils.exceptionResponse(throwable.getMessage());
        }
    }

    private Response checkRequiredParameters(String srcProviderName, AREXMocker body) {
        MockCategoryType category = body.getCategoryType();
        if (category == null) {
            LOGGER.warn("update record the category not found {}", srcProviderName);
            return ResponseUtils.parameterInvalidResponse(srcProviderName);
        }
        if (StringUtils.isBlank(body.getAppId())) {
            LOGGER.warn("update record request appId is empty,{}", body);
            return ResponseUtils.parameterInvalidResponse("request appId is empty");
        }
        if (StringUtils.isBlank(body.getRecordId())) {
            LOGGER.warn("update record the recordId is empty {}", body);
            return ResponseUtils.emptyRecordIdResponse();
        }
        return null;
    }

    @Getter
    @Setter
    protected static class CopyResponseType implements Response {
        private ResponseStatusType responseStatusType;
        private int copied;
    }

}