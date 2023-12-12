package com.arextest.config.mapper;

import com.arextest.config.model.dao.config.InstancesCollection;
import com.arextest.config.model.dto.application.InstancesConfiguration;
import com.arextest.config.model.vo.AgentRemoteConfigurationRequest;
import com.arextest.config.model.vo.AgentStatusRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface InstancesMapper {

  String TAG_PREFIX = "arex.tags.";

  InstancesMapper INSTANCE = Mappers.getMapper(InstancesMapper.class);

  @Mappings({@Mapping(target = "modifiedTime",
      expression = "java(dao.getDataChangeUpdateTime() == null ? null : new java.sql.Timestamp(dao.getDataChangeUpdateTime()))")})
  InstancesConfiguration dtoFromDao(InstancesCollection dao);

  InstancesCollection daoFromDto(InstancesConfiguration dto);

  @Mappings({
      @Mapping(target = "tags", source = "systemProperties", qualifiedByName = "extractTags"),
  })
  InstancesConfiguration dtoFromContract(AgentRemoteConfigurationRequest contract);

  InstancesConfiguration dtoFromContract(AgentStatusRequest request);

  @Named("extractTags")
  default Map<String, String> extractTags(Map<String, String> systemProperties) {
    if (systemProperties == null || systemProperties.isEmpty()) {
      return null;
    }
    HashMap<String, String> tags = new HashMap<>();
    for (Map.Entry<String, String> entry : systemProperties.entrySet()) {
      String key = entry.getKey();
      Optional.ofNullable(key).ifPresent(k -> {
        if (k.startsWith(TAG_PREFIX)) {
          String substring = key.substring(TAG_PREFIX.length());
          if (substring.isEmpty()) {
            return;
          }
          tags.put(substring, entry.getValue());
        }
      });
    }
    return tags;
  }
}
