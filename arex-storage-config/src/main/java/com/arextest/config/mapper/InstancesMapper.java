package com.arextest.config.mapper;

import com.arextest.config.model.dao.config.InstancesCollection;
import com.arextest.config.model.vo.AgentRemoteConfigurationRequest;
import com.arextest.config.model.vo.AgentStatusRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import com.arextest.config.model.dto.application.InstancesConfiguration;

@Mapper
public interface InstancesMapper {

    InstancesMapper INSTANCE = Mappers.getMapper(InstancesMapper.class);

    @Mappings({@Mapping(target = "modifiedTime",
        expression = "java(dao.getDataChangeUpdateTime() == null ? null : new java.sql.Timestamp(dao.getDataChangeUpdateTime()))")})
    InstancesConfiguration dtoFromDao(InstancesCollection dao);

    InstancesCollection daoFromDto(InstancesConfiguration dto);

    InstancesConfiguration dtoFromContract(AgentRemoteConfigurationRequest contract);

    InstancesConfiguration dtoFromContract(AgentStatusRequest request);
}
