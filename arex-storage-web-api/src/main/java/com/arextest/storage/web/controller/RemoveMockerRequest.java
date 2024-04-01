package com.arextest.storage.web.controller;

import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author wildeslam.
 * @create 2024/4/1 19:46
 */
@Data
public class RemoveMockerRequest {
  private String category;
  @NotNull
  private String mockerId;
  private Integer index;

}
