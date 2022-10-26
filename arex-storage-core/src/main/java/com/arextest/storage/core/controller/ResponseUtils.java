package com.arextest.storage.core.controller;

import com.arextest.storage.model.Response;
import com.arextest.storage.model.ResponseCode;
import com.arextest.storage.model.header.ResponseStatusType;

import javax.validation.constraints.NotNull;

/**
 * @author jmo
 * @since 2021/11/10
 */
final class ResponseUtils {
    private static final String REQUESTED_BODY_EMPTY = "requested body empty";
    private static final String REQUESTED_RECORD_ID_EMPTY = "The recordId of requested is empty";
    private static final String REQUESTED_REPLAY_RESULT_ID_EMPTY = "The replayResultId of requested is empty";
    private static final String SUCCESS = "success";

    private ResponseUtils() {

    }

    static Response emptyRecordIdResponse() {
        return parameterInvalidResponse(REQUESTED_RECORD_ID_EMPTY);
    }

    static Response emptyReplayResultIdResponse() {
        return parameterInvalidResponse(REQUESTED_REPLAY_RESULT_ID_EMPTY);
    }

    static Response exceptionResponse(String remark) {
        return errorResponse(remark, ResponseCode.REQUESTED_HANDLE_EXCEPTION);
    }

    static Response errorResponse(String remark, ResponseCode responseCode) {
        return errorResponse(responseStatus(remark, responseCode));
    }

    static Response errorResponse(ResponseStatusType responseStatusType) {
        return new DefaultResponseImpl(responseStatusType);
    }

    private final static class DefaultResponseImpl implements Response {
        private final ResponseStatusType responseStatusType;

        private DefaultResponseImpl(ResponseStatusType responseStatusType) {
            this.responseStatusType = responseStatusType;
        }

        @Override
        public ResponseStatusType getResponseStatusType() {
            return responseStatusType;
        }

        @Override
        public void setResponseStatusType(ResponseStatusType responseStatusType1) {

        }
    }

    static Response successResponse(boolean result) {
        if (result) {
            return new DefaultResponseImpl(successStatus());
        }
        return resourceNotFoundResponse();
    }

    static <T extends Response> T successResponse(@NotNull T target) {
        ResponseStatusType responseStatusType = successStatus();
        target.setResponseStatusType(responseStatusType);
        return target;
    }

    static Response resourceNotFoundResponse() {
        return errorResponse(ResponseCode.REQUESTED_RESOURCE_NOT_FOUND.name(),
                ResponseCode.REQUESTED_RESOURCE_NOT_FOUND);
    }

    static Response parameterInvalidResponse(String remark) {
        return errorResponse(remark, ResponseCode.REQUESTED_PARAMETER_INVALID);
    }

    static ResponseStatusType successStatus() {
        return responseStatus(SUCCESS, ResponseCode.SUCCESS);
    }

    static Response requestBodyEmptyResponse() {
        return parameterInvalidResponse(REQUESTED_BODY_EMPTY);
    }

    private static ResponseStatusType responseStatus(String remark, ResponseCode responseCode) {
        ResponseStatusType responseStatusType = new ResponseStatusType();
        responseStatusType.setResponseDesc(remark);
        responseStatusType.setResponseCode(responseCode.getCodeValue());
        responseStatusType.setTimestamp(System.currentTimeMillis());
        return responseStatusType;
    }
}