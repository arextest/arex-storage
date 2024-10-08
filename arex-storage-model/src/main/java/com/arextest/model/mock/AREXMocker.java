package com.arextest.model.mock;


import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@FieldNameConstants
@NoArgsConstructor
@Document
public class AREXMocker extends AbstractMocker implements Mocker {

  /**
   * the value required and empty not allowed
   */
  @Transient
  private MockCategoryType categoryType;

  private Target targetRequest;
  private Target targetResponse;
  private Map<Integer, Long> eigenMap;

  public AREXMocker(MockCategoryType categoryType) {
    this.categoryType = categoryType;
  }
}