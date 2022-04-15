package io.arex.storage.model.enums;

import io.arex.storage.model.mocker.ConfigVersion;
import io.arex.storage.model.mocker.MainEntry;
import io.arex.storage.model.mocker.MockItem;
import io.arex.storage.model.mocker.impl.ABtMocker;
import io.arex.storage.model.mocker.impl.DalResultMocker;
import io.arex.storage.model.mocker.impl.DynamicResultMocker;
import io.arex.storage.model.mocker.impl.HttpMocker;
import io.arex.storage.model.mocker.impl.ConfigFileMocker;
import io.arex.storage.model.mocker.impl.ConfigMetaMocker;
import io.arex.storage.model.mocker.impl.ConfigVersionMocker;
import io.arex.storage.model.mocker.impl.QmqConsumerMocker;
import io.arex.storage.model.mocker.impl.QmqProducerMocker;
import io.arex.storage.model.mocker.impl.RedisMocker;
import io.arex.storage.model.mocker.impl.SoaExternalMocker;
import io.arex.storage.model.mocker.impl.SoaMainMocker;
import io.arex.storage.model.mocker.impl.DatabaseMocker;
import io.arex.storage.model.mocker.impl.HttpClientMocker;
import io.arex.storage.model.mocker.impl.ServletMocker;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Mocker 类别
 *
 * @author jmo
 * @since 2021/11/2
 */
@Getter
public enum MockCategoryType {
    /**
     * SOA_MAIN
     */
    SOA_MAIN(0, "soaMain", "SoaMainMocker", SoaMainMocker.class),
    /**
     * SOA_EXTERNAL
     */
    SOA_EXTERNAL(1, "soaExt", "SoaExternalMocker", SoaExternalMocker.class),
    /**
     * QMQ_PRODUCER
     */
    QMQ_PRODUCER(2, "qmqProducer", "QmqMessageMocker", QmqProducerMocker.class),
    /**
     * DAL
     */
    DAL(3, "db", "DalResultMocker", DalResultMocker.class),
    /**
     * REDIS
     */
    REDIS(4, "redis", "RedisMocker", RedisMocker.class),
    /**
     * DYNAMIC
     */
    DYNAMIC(5, "dynamic", "DynamicResultMocker", DynamicResultMocker.class),
    /**
     * HTTP
     */
    HTTP(6, "http", "HttpMocker", HttpMocker.class),
    /**
     * AB_TEST
     */
    AB_TEST(7, "abt", "ABtMocker", ABtMocker.class),

    /**
     * the content of a config file
     */
    CONFIG_FILE(9, "configFile", "QConfigFileMocker", ConfigFileMocker.class),
    /**
     * QMQ_CONSUMER
     */
    QMQ_CONSUMER(10, "qmqConsumer", "QMQMainMocker", QmqConsumerMocker.class),

    /**
     * CONFIG_META
     */
    CONFIG_META(11, "configMeta", "QConfigMetaMocker", ConfigMetaMocker.class),
    /**
     * CONFIG_VERSION for build a version key for all depend config files
     */
    CONFIG_VERSION(12, "configVersion", "", ConfigVersionMocker.class),

    SERVICE_CALL(13, "ServiceCall", "HttpClientMocker", HttpClientMocker.class),

    DATABASE(14, "Database", "DatabaseMocker", DatabaseMocker.class),

    SERVLET_ENTRANCE(15, "ServletEntrance", "ServletMocker", ServletMocker.class);

    private final int codeValue;
    private final String displayName;
    private final String mocker;
    private final Class<? extends MockItem> mockImplClassType;
    private final static Map<Integer, MockCategoryType> CODE_VALUE_MAP = asMap(MockCategoryType::getCodeValue);
    private final static Map<String, MockCategoryType> SHORT_NAME_MAP = asMap(MockCategoryType::getDisplayName);
    private final boolean mainEntry;
    private final boolean configVersion;

    MockCategoryType(int codeValue, String displayName, String mocker,
                     Class<? extends MockItem> mockImplClassType) {
        this.codeValue = codeValue;
        this.displayName = displayName;
        this.mocker = mocker;
        this.mockImplClassType = mockImplClassType;
        this.mainEntry = MainEntry.class.isAssignableFrom(mockImplClassType);
        this.configVersion = ConfigVersion.class.isAssignableFrom(mockImplClassType);
    }

    private static <K> Map<K, MockCategoryType> asMap(Function<MockCategoryType, K> keySelector) {
        MockCategoryType[] values = values();
        Map<K, MockCategoryType> mapResult = new HashMap<>(values.length);
        for (int i = 0; i < values.length; i++) {
            MockCategoryType category = values[i];
            mapResult.put(keySelector.apply(category), category);
        }
        return mapResult;
    }

    public static MockCategoryType of(Integer codeValue) {
        return CODE_VALUE_MAP.get(codeValue);
    }

    public static MockCategoryType of(String displayName) {
        return SHORT_NAME_MAP.get(displayName);
    }

}
