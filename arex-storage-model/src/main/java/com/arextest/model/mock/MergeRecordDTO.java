package com.arextest.model.mock;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wildeslam.
 * @create 2023/12/18 20:03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MergeRecordDTO {
    private String response;
    private String responseType;
    private String category;
    private int methodSignatureHash;
    private int methodRequestTypeHash;
    private String operationName;
    private String request;
    private String serializeType;
    private String recordId;
    private Map<String, Object> requestAttributes;
    private Map<String, Object> responseAttributes;
}
