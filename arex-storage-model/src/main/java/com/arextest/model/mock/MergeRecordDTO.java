package com.arextest.model.mock;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * @author wildeslam.
 * @create 2023/12/18 20:03
 */
@Data
@Builder
public class MergeRecordDTO {
    private Object arexOriginalResult;
    private String arexResultClazz;
    private String category;
    private int methodSignatureHash;
    private String operationName;
    private String request;
    private String serializeType;
    private String recordId;
    private Map<String, Object> requestAttributes;
    private Map<String, Object> responseAttributes;
}
