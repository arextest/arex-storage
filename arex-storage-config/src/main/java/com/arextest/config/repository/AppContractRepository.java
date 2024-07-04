package com.arextest.config.repository;

import com.arextest.config.model.dto.application.AppContract;
import java.util.List;

/**
 * @author wildeslam.
 * @create 2024/7/3 17:07
 */
public interface AppContractRepository {
  List<AppContract> queryAppContracts(String appId);
}
