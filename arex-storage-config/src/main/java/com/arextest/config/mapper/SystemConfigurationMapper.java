package com.arextest.config.mapper;

import com.arextest.config.model.dao.config.SystemConfigurationCollection;
import com.arextest.config.model.dto.SystemConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * @author wildeslam.
 * @create 2024/2/21 19:53
 */
@Mapper
public interface SystemConfigurationMapper {

  SystemConfigurationMapper INSTANCE = Mappers.getMapper(SystemConfigurationMapper.class);

  SystemConfiguration dtoFromDao(SystemConfigurationCollection dao);

  @Mappings({@Mapping(target = "id", expression = "java(null)"),
      @Mapping(target = "dataChangeCreateTime", expression = "java(System.currentTimeMillis())"),
      @Mapping(target = "dataChangeUpdateTime", expression = "java(System.currentTimeMillis())")})
  SystemConfigurationCollection daoFromDto(SystemConfiguration dto);
}

