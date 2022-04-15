package io.arex.storage.model.mocker.impl;

import io.arex.storage.model.annotations.FieldCompression;
import io.arex.storage.model.mocker.AbstractMocker;
import io.arex.storage.model.mocker.ConfigVersion;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author jmo
 * @since 2021/11/2
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ConfigMetaMocker extends AbstractMocker implements ConfigVersion {
    private Integer recordVersion;
    @FieldCompression
    private String data;
}