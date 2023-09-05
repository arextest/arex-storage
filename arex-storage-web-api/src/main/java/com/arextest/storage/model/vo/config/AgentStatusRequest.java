package com.arextest.storage.model.vo.config;

import lombok.Data;

/**
 * @author Owen_gan
 * @since 2023/7/17
 */
@Data
public class AgentStatusRequest {
    private String appId;
    private String host;
    private String agentStatus;
}
