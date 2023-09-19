package com.arextest.config.model.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author wildeslam.
 * @create 2023/9/19 11:21
 */
@Data
public class UpdateApplicationRequest {
    @NotNull
    private String appId;
    private String appName;
    private List<String> appOwners;
}
