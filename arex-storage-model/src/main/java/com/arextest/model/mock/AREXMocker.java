package com.arextest.model.mock;


import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.annotation.Transient;

@Getter
@Setter
@FieldNameConstants
@NoArgsConstructor
@Document
public class AREXMocker implements Mocker {

  /**
   * 1、Only for editing dependencies,the entry point ignored 2、During query, record the id of the
   * mock, and use the id to associate data during comparison
   */
  private String id;

  /**
   * the value required and empty not allowed
   */
  @Transient
  private MockCategoryType categoryType;
  private String replayId;
  private String recordId;
  private String appId;
  private int recordEnvironment;

  /**
   * millis from utc format without timezone
   */
  @Field(targetType = FieldType.DATE_TIME)
  private long creationTime;
  @Field(targetType = FieldType.DATE_TIME)
  private long updateTime;
  @Field(targetType = FieldType.DATE_TIME)
  private long expirationTime;

  private Target targetRequest;
  private Target targetResponse;

  /**
   * the value required and empty allowed for example: pattern of servlet web api
   */
  private String operationName;
  /**
   * record the version of recorded data
   */
  private String recordVersion;
  private Map<Integer, Long> eigenMap;

  /**
   * add tag to mocker
   */
  private Map<String, String> tags;

  /**
   * index for mergedRecord.
   */
  private Integer index;

  public AREXMocker(MockCategoryType categoryType) {
    this.categoryType = categoryType;
  }
}