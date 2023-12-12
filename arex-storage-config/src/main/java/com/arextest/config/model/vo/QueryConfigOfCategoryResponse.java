package com.arextest.config.model.vo;

import java.util.List;
import java.util.Set;
import lombok.Data;

/**
 * created by xinyuan_wang on 2023/12/4
 */
@Data
public class QueryConfigOfCategoryResponse {
  private QueryConfigOfCategory body;

  @Data
  public static class QueryConfigOfCategory {
    private String operationName;

    private Set<List<String>> exclusionList;

    private Set<String> ignoreNodeSet;
  }
}
