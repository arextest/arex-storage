package com.arextest.model.replay;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum CaseStatusEnum {
  /**
   * normal case.
   */
  NORMAL(0),
  /**
   * deduplicated case.
   */
  DEDUPLICATED(1);

  private final int code;

  public int getCode() {
    return code;
  }
}
