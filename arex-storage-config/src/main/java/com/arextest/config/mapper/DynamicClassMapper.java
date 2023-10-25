package com.arextest.config.mapper;

import com.arextest.config.model.dao.config.DynamicClassCollection;
import com.arextest.config.model.dto.record.DynamicClassConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DynamicClassMapper {

    DynamicClassMapper INSTANCE = Mappers.getMapper(DynamicClassMapper.class);

    @Mappings({@Mapping(target = "modifiedTime",
            expression = "java(dao.getDataChangeUpdateTime() == null ? null : new java.sql.Timestamp(dao.getDataChangeUpdateTime()))")})
    DynamicClassConfiguration dtoFromDao(DynamicClassCollection dao);

    @Mappings({@Mapping(target = "id", expression = "java(null)"),
            @Mapping(target = "dataChangeCreateTime", expression = "java(System.currentTimeMillis())"),
            @Mapping(target = "dataChangeUpdateTime", expression = "java(System.currentTimeMillis())")})
    DynamicClassCollection daoFromDto(DynamicClassConfiguration dto);
}
