package com.arextest.storage.model.mapper;


import com.arextest.model.dao.config.InstancesCollection;
import com.arextest.storage.model.dto.config.application.InstancesConfiguration;
import com.arextest.storage.model.vo.config.AgentRemoteConfigurationRequest;
import com.arextest.storage.model.vo.config.AgentStatusRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;


@Mapper
public interface InstancesMapper {

    InstancesMapper INSTANCE = Mappers.getMapper(InstancesMapper.class);

    @Mappings({
            @Mapping(target = "modifiedTime", expression = "java(dao.getDataChangeUpdateTime() == null ? null : new java.sql.Timestamp(dao.getDataChangeUpdateTime()))")
    })
    InstancesConfiguration dtoFromDao(InstancesCollection dao);

    InstancesCollection daoFromDto(InstancesConfiguration dto);

    InstancesConfiguration dtoFromContract(AgentRemoteConfigurationRequest contract);

    InstancesConfiguration dtoFromContract(AgentStatusRequest request);
}
