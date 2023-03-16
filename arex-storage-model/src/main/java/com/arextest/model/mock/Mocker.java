package com.arextest.model.mock;


import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public interface Mocker {
    String getAppId();

    String getReplayId();

    void setReplayId(String replayId);

    String getRecordId();

    void setRecordId(String recordId);

    void setRecordEnvironment(int environment);

    int getRecordEnvironment();

    /**
     * millis from utc format without timezone
     */
    void setCreationTime(long creationTime);

    long getCreationTime();

    void setId(String id);

    String getId();

    MockCategoryType getCategoryType();

    String getOperationName();

    Target getTargetRequest();

    Target getTargetResponse();

    String getRecordVersion();

    void setRecordVersion(String recordVersion);


    @Getter
    @Setter
    class Target {
        /**
         * The value used base64 encoding from AREX's Agent for bytes requested.
         */
        private String body;
        private Map<String, Object> attributes;
        /**
         * It used by AREX's agent deserialization which class type should be applying
         */
        private String type;

        public Object getAttribute(String name) {
            return attributes == null ? null : attributes.get(name);
        }

        public void setAttribute(String name, Object value) {
            if (this.attributes == null) {
                this.attributes = new HashMap<>();
            }
            if (value == null) {
                this.attributes.remove(name);
                return;
            }
            this.attributes.put(name, value);
        }

        public String attributeAsString(String name) {
            Object result = getAttribute(name);
            return result instanceof String ? (String) result : null;
        }
    }

}