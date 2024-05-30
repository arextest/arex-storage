package com.arextest.storage.enums;

/**
 * @author xinyuan_wnag.
 * @create 2024/5/21
 */
public enum InvalidReasonEnum {

  /**
   * FastReject
   */
  FAST_REJECT("FastReject"),
  /**
   * QueueOverFlow
   */
  QUEUE_OVER_FLOW("QueueOverFlow"),
  /**
   * StorageSaveError
   */
  STORAGE_SAVE_ERROR("StorageSaveError");

  final String value;

  private InvalidReasonEnum(String value) {
    this.value = value;
  }

  public String getValue() {
    return this.value;
  }
}
