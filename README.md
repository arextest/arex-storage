# <img src="https://avatars.githubusercontent.com/u/103105168?s=200&v=4" alt="Arex Icon" width="27" height=""> AREX's Storage Service

## Introduction

It is an http web APIs used to manage or access the recording and replaying resources from remote repository,
which it structured by `MockItem` for each category enums:`MockCategoryType`.

The primary functions as following:

1. **for the agent recording**
    - provide a version number for all config files
    - save all the entry point & dependent instances which implemented `MockItem`
1. **for the agent replaying**
    - read the config file by version number before replaying
    - receive the request to diff and read a mock result as response
1. **for the schedule replaying**
    - provide entry points as replay trigger source structured by `MainEntry` extends `MockItem`
    - provide diff response source which saved from agent's replay

Currently,
we use mongodb as default persistence for `AREX's agent`'s recording,
because the item of collection will dropped by the TTL index expired.

we use Redis as default cache provider for `AREX`'s replaying.

The `MockItem` is a base interface defined in the package `arex.storage.model.mocker` as following:

  ```java
    public interface MockItem {
        String getReplayId();
    
        void setReplayId(String replayId);
    
        String getRecordId();
    
        void setRecordId(String recordId);
    
        /**
         * millis from utc format without timezone
         */
        void setCreateTime(long createTime);
    }
  ```

Note: The value of `replayId` and `recordId` from beginning to end created by [**AREX's Agent
**](arextest/arex-agent-java).

The `MainEntry` is a base interface defined in the package `arex.storage.model.mocker` as following:

   ```java
     public interface MainEntry extends MockItem {
    
         /**
          * @return utc format without timezone
          */
         long getCreateTime();
         /**
           * The request's content encoded by base64
           *
           */
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
   ```

## Getting Started

1. **Modify default value `localhost`**

   If you use default providers as remote storage implementation,
   you should be change the connection string in the file of path 'resources/META-INF/application.properties'.

   example for `Redis` & `mongodb` connection:
   ```
   arex.storage.cache.redis.host=redis://10.3.2.42:6379/
   arex.mongo.uri=mongodb://arex:iLoveArex@10.3.2.42:27017/arex_storage_db
   ```
1. **Extends your providers**

   We use java's `SPI` mechanism to support your implementation.

   The storage cache interface `arex.common.cache.CacheProvider`  defined as following:
   ```java
   public interface CacheProvider {
       /**
        * put the value with expired seconds
        * @param key the key for value
        * @param expiredSeconds  the expired seconds
        * @param value bytes of the object
        * @return true if success,others false
        */
       boolean put(byte[] key, long expiredSeconds, byte[] value);    
           
       /**
        * Get the value of a key
        *
        * @param key the bytes of key
        * @return null when key does not exist.
        */
       byte[] get(byte[] key);
   
       /**
        * Increment integer value of the key by on
        *
        * @param key the key
        * @return The value of the key after increment
        */
       long incrValue(byte[] key);
   
       /**
        * Decrement the integer value of a key by one.
        *
        * @param key the key
        * @return the value of key after decrement
        */
       long decrValue(byte[] key);
   
       /**
        * Delete value specified by the key
        *
        * @param key the bytes of key
        * @return True if key is removed.
        */
       boolean remove(byte[] key);    
   }
   ```

## License

- Code: [Apache-2.0](https://github.com/arextest/arex-agent-java/blob/LICENSE)