package com.arextest.storage.model.mocker;


import com.arextest.storage.model.annotations.FieldCompression;
import com.arextest.storage.model.serialization.ObjectIdDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import org.bson.types.ObjectId;

/**
 * @author jmo
 * @since 2021/11/2
 */
@Data
public abstract class AbstractMocker implements MockItem {

    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    private ObjectId id;

    private String replayId;
    private String recordId;
    private String appId;
    private long createTime;
    private String ip;
    @FieldCompression
    private String response;
    private String responseType;
}