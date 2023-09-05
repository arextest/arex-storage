package com.arextest.storage.model.vo.config;

import java.util.List;

import com.arextest.storage.model.dto.config.StatusType;
import com.arextest.storage.model.dto.config.record.DynamicClassConfiguration;
import com.arextest.storage.model.dto.config.record.ServiceCollectConfiguration;

import lombok.Data;

/**
 * @author b_yu
 * @since 2023/6/14
 */
@Data
public class AgentRemoteConfigurationResponse {
    private ServiceCollectConfiguration serviceCollectConfiguration;
    private List<DynamicClassConfiguration> dynamicClassConfigurationList;

    /**
     * Bit flag composed of bits that record/replay are enabled. see {@link StatusType}
     */
    private Integer status;

    private String targetAddress;
}