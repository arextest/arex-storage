package com.arextest.config.model.vo;

import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * created by xinyuan_wang on 2023/12/4
 */
@Data
public class QueryConfigOfCategory {
  private String operationName;

  private Set<List<String>> exclusionList;

  private Set<String> ignoreNodeSet;
}