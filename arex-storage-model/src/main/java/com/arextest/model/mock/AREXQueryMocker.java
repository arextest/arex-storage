package com.arextest.model.mock;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@FieldNameConstants
@NoArgsConstructor
@Document
public class AREXQueryMocker extends AbstractMocker implements Mocker {

  /**
   * the value required and empty not allowed
   */
  @Transient
  private MockCategoryType categoryType;

  @Field("targetRequest")
  private String request;
  @Field("targetResponse")
  private String response;
}