package com.arextest.config.mapper;

import com.arextest.config.model.dao.config.AppContractCollection;
import com.arextest.config.model.dto.application.AppContract;
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
