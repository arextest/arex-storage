package com.arextest.config.model.dao.config;

import com.arextest.config.model.dao.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author wildeslam.
 * @create 2024/7/4 10:57
 */
@Data
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants
@Document(collection = "AppContract")
public class AppContractCollection extends BaseEntity {
  private String appId;
  private Integer contractType;
  private String operationId;
  private String operationName;
  private String operationType;
  private String contract;
}
