package com.arextest.storage.model.mocker.impl;

import com.arextest.storage.model.annotations.FieldCompression;
import com.arextest.storage.model.mocker.AbstractMocker;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * @author jmo
 * @since 2021/11/2
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MessageMocker extends AbstractMocker {
    private String subject;
    private Map<String, Object> realAttrs;
    @FieldCompression
    private String msgBody;
    private String messageId;
}
