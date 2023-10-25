package com.arextest.config.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * @author jmo
 * @since 2022/1/23
 */
@Getter
@Setter
public abstract class AbstractConfiguration {
    private Integer status;
    private Timestamp modifiedTime;


    public void validParameters() throws Exception {

    }
}
