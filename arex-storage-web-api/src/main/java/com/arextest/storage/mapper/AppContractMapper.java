package com.arextest.storage.mapper;

import com.arextest.config.model.dto.application.AppContract;
import com.arextest.storage.model.AppContractCollection;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author wildeslam.
 * @create 2024/7/4 10:59
 */
@Mapper
public interface AppContractMapper {
  AppContractMapper INSTANCE = Mappers.getMapper(AppContractMapper.class);

  AppContractCollection daoFromDto(AppContract dto);

  AppContract dtoFromDao(AppContractCollection dao);
}
