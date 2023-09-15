package com.arextest.config.model.dto.record;

import lombok.Data;

import javax.annotation.sql.DataSourceDefinition;

@Data
public class SerializeSkipInfoConfiguration {
    private String fullClassName;
    private String fieldName;
}
