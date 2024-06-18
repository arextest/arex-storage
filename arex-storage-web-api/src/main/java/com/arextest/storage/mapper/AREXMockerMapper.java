package com.arextest.storage.mapper;

import com.arextest.model.mock.AREXMocker;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AREXMockerMapper {

  AREXMockerMapper INSTANCE = Mappers.getMapper(AREXMockerMapper.class);

  AREXMocker dtoFromEntity(AREXMocker entity);

}
