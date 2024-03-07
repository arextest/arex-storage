package com.arextest.config.model.dto.system;

import java.util.List;
import lombok.Data;

@Data
public class ComparePluginInfo {

  private String comparePluginUrl;
  private List<String> transMethodList;
}