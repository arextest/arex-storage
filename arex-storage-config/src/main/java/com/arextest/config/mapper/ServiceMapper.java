package com.arextest.config.mapper;

import com.arextest.config.model.dao.config.ServiceCollection;
import com.arextest.config.model.dto.application.ApplicationServiceConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;


@Mapper
public interface ServiceMapper {

  ServiceMapper INSTANCE = Mappers.getMapper(ServiceMapper.class);

  @Mappings({
      @Mapping(target = "modifiedTime", expression = "java(dao.getDataChangeUpdateTime() == null ? null : new java.sql.Timestamp(dao.getDataChangeUpdateTime()))")
  })
  ApplicationServiceConfiguration dtoFromDao(ServiceCollection dao);

  @Mappings({
      @Mapping(target = "id", expression = "java(null)"),
      @Mapping(target = "dataChangeCreateTime", expression = "java(System.currentTimeMillis())"),
      @Mapping(target = "dataChangeUpdateTime", expression = "java(System.currentTimeMillis())")
  })
  ServiceCollection daoFromDto(ApplicationServiceConfiguration dto);
}
