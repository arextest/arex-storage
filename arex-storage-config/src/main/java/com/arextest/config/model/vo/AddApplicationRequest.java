package com.arextest.config.model.vo;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Set;

/**
 * @author wildeslam.
 * @create 2023/9/15 14:21
 */
@Data
public class AddApplicationRequest {
    @NotNull
    private String appName;
    @NotEmpty
    private Set<String> owners;
}
