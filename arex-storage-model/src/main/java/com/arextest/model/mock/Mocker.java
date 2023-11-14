package com.arextest.model.mock;


import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

public interface Mocker {

  String getAppId();

  String getReplayId();

  void setReplayId(String replayId);

  String getRecordId();

  void setRecordId(String recordId);

  int getRecordEnvironment();

  void setRecordEnvironment(int environment);

  long getCreationTime();

  /**
   * millis from utc format without timezone
   */
  void setCreationTime(long creationTime);

  long getUpdateTime();

  void setUpdateTime(long updateTime);

  /**
   * MongoDB TTL Index
   */
  long getExpirationTime();

  void setExpirationTime(long expirationTime);

  String getId();

  void setId(String id);

  MockCategoryType getCategoryType();

  String getOperationName();

  Target getTargetRequest();

  Target getTargetResponse();

  String getRecordVersion();

  void setRecordVersion(String recordVersion);

  Map<Integer, Long> getEigenMap();

  /**
   * Eigenvalues of mock data
   * key: Hashcode for the key value of the JSON node
   * value: The hash code of the value value value of the JSON node,
   * The use of long type is to prevent memory overflow
   */
  void setEigenMap(Map<Integer, Long> eigenMap);

  void setTags(Map<String, Object> tags);

  Map<String, Object> getTags();


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