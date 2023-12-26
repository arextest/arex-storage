package com.arextest.model.replay;

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
  // default: false
  private Boolean splitMergeRecord;
}