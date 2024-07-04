package com.arextest.config.model.vo;

import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wildeslam.
 * @create 2024/5/29 17:21
 */
@Data
@NoArgsConstructor
public class ConfigComparisonExclusions {
  private String operationName;
  private String categoryType;
  private Set<List<String>> exclusionList;
}
