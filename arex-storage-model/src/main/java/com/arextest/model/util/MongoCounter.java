package com.arextest.model.util;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author: QizhengMo
 * @date: 2024/4/11
 */
@Document
@Setter
@Getter
@FieldNameConstants
public class MongoCounter {
  private String id;
  private Long count;
}
