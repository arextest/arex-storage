package com.arextest.storage.model.mocker.impl;

import com.arextest.storage.model.mocker.AbstractMocker;
import com.arextest.storage.model.mocker.ConfigVersion;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

/**
 * build a key for all config files
 *
 * @author jmo
 * @since 2021/11/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ConfigVersionMocker extends AbstractMocker implements ConfigVersion {
    private Integer recordVersion;


    @BsonId
    @Override
    public ObjectId getId() {
        return super.getId();
    }
}
