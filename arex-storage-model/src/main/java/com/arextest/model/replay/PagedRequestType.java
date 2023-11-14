package com.arextest.model.replay;

import com.arextest.model.mock.MockCategoryType;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
public class PagedRequestType {

  private String appId;
  private Long beginTime;
  private Long endTime;
  private Integer env;
  private int pageSize;
  private Integer pageIndex;
  private String operation;
  private MockCategoryType category;
  private String sourceProvider;
  private List<SortingOption> sortingOptions;
  private Map<String, Object> mockTags;
}
