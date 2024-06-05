package com.arextest.config.mapper;

import com.arextest.config.model.dao.config.ConfigComparisonExclusionsCollection;
import com.arextest.config.model.dto.ComparisonExclusionsConfiguration;
import com.arextest.config.model.vo.ConfigComparisonExclusionsVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * Created by rchen9 on 2022/9/16.
 */
@Mapper
public interface ConfigComparisonExclusionsMapper {

  ConfigComparisonExclusionsMapper INSTANCE = Mappers.getMapper(
      ConfigComparisonExclusionsMapper.class);

  @Mappings({@Mapping(target = "modifiedTime",
      expression = "java(dao.getDataChangeUpdateTime() == null ? null : new java.sql.Timestamp(dao.getDataChangeUpdateTime()))")})
  ComparisonExclusionsConfiguration dtoFromDao(ConfigComparisonExclusionsCollection dao);

  @Mappings({@Mapping(target = "id", expression = "java(null)"),
      @Mapping(target = "dataChangeCreateTime", expression = "java(System.currentTimeMillis())"),
      @Mapping(target = "dataChangeUpdateTime", expression = "java(System.currentTimeMillis())")})
  ConfigComparisonExclusionsCollection daoFromDto(ComparisonExclusionsConfiguration dto);

  ConfigComparisonExclusionsVO voFromDto(ComparisonExclusionsConfiguration dto);
}