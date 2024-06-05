package com.arextest.config.model.vo;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wildeslam.
 * @create 2024/5/29 17:21
 */
@Data
@NoArgsConstructor
public class ConfigComparisonExclusionsVO {
  private String appId;
  private String operationId;
  private String dependencyId;
  private String operationType;
  private List<String> exclusions;
}
