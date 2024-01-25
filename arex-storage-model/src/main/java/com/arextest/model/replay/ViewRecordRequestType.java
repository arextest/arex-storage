package com.arextest.model.replay;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author jmo
 * @since 2021/11/3
 */
@Getter
@Setter
@ToString
public class ViewRecordRequestType {

  private String recordId;
  private String sourceProvider;
  private String categoryType;
  /**
   * If categoryType is passed, the type of categoryTypes will not be paid attention to.
   */
  private List<String> categoryTypes;
  // default: false
  private Boolean splitMergeRecord;
}