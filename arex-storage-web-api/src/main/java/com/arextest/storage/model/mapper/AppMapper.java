package com.arextest.storage.model.mapper;


import com.arextest.model.dao.config.AppCollection;
import com.arextest.storage.model.dto.config.application.ApplicationConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AppMapper {

    AppMapper INSTANCE = Mappers.getMapper(AppMapper.class);

    @Mappings({
            @Mapping(target = "modifiedTime", expression = "java(dao.getDataChangeUpdateTime() == null ? null : new java.sql.Timestamp(dao.getDataChangeUpdateTime()))")
    })
    ApplicationConfiguration dtoFromDao(AppCollection dao);

    @Mappings({
            @Mapping(target = "dataChangeCreateTime", expression = "java(System.currentTimeMillis())"),
            @Mapping(target = "dataChangeUpdateTime", expression = "java(System.currentTimeMillis())")
    })
    AppCollection daoFromDto(ApplicationConfiguration dto);

}
