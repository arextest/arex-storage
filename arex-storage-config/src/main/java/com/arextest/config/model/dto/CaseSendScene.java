package com.arextest.config.model.dto;

/**
 * Sent from schedule -> Target Service Agent -> Storage
 * Used to identified the use case of test case sent by schedule
 */
public enum CaseSendScene {
  /**
   * normal cases
   */
  NORMAL,
  /**
   * extra cases like de-noise and config recover
   */
  EXTRA,

  /**
   * normal cases in mixed replay
   */
  MIXED_NORMAL;
}