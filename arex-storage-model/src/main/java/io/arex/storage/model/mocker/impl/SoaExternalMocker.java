package io.arex.storage.model.mocker.impl;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author jmo
 * @since 2021/11/2
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SoaExternalMocker extends ServiceMocker {
    /**
     * 结果类型字符串
     */
    private String responseType;
}
