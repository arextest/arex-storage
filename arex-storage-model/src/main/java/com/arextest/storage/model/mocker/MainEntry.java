package com.arextest.storage.model.mocker;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.enums.RecordEnvType;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

/**
 * The entrance of the recording
 *
 * @author jmo
 * @since 2021/11/18
 */
public interface MainEntry extends MockItem {

    /**
     * default value PRO
     *
     * @param env the recorder running environment
     * @see RecordEnvType
     */
    @JsonIgnore
    void setEnv(int env);

    /**
     * @return utc format without timezone
     */
    long getCreateTime();

    String getRequest();

    /**
     * @return the mock category type value from MockCategoryType
     * @see MockCategoryType
     */
    @JsonIgnore
    int getCategoryType();

    /**
     * How to serialize the request's body to target ,default using application/json
     *
     * @return application/json or others
     */
    default String getFormat() {
        return null;
    }

    default String getConsumerGroupName() {
        return null;
    }

    default Integer getConfigVersion() {
        return null;
    }

    default String getAgentVersion() {
        return null;
    }

    /**
     * @return default http post
     */
    default String getMethod() {
        return "POST";
    }

    default Map<String, String> getRequestHeaders() {
        return null;
    }

    default String getPath() {
        return null;
    }
}
