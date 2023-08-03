package com.arextest.model.mock;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "name")
public class MockCategoryType {
    private static final long ONE_DAY = 24 * 60 * 60 * 1000;

    private String name;
    private boolean entryPoint;
    private boolean skipComparison;
    // milliseconds.
    private long expirationTime;

    @Override
    public String toString() {
        return this.name;
    }

    public static MockCategoryType createEntryPoint(String name) {
        return new MockCategoryType(name, true, false);
    }

    public static MockCategoryType createSkipComparison(String name) {
        return new MockCategoryType(name, false, true);
    }

    public static MockCategoryType createSkipComparison(String name, long expirationTime) {
        return new MockCategoryType(name, false, true, expirationTime);
    }

    public MockCategoryType(String name, boolean entryPoint, boolean skipComparison) {
        this.name = name;
        this.entryPoint = entryPoint;
        this.skipComparison = skipComparison;
        // default value.
        this.expirationTime = 4 * ONE_DAY;
    }

    public static MockCategoryType createDependency(String name) {
        return new MockCategoryType(name, false, false);
    }

    public static MockCategoryType create(String name) {
        for (MockCategoryType categoryType : DEFAULTS) {
            if (Objects.equals(categoryType.name, name)) {
                return categoryType;
            }
        }
        return createDependency(name);
    }

    public static final Set<MockCategoryType> DEFAULTS;
    public static final MockCategoryType Q_MESSAGE_CONSUMER = MockCategoryType.createEntryPoint("QMessageConsumer");
    public static final MockCategoryType Q_MESSAGE_PRODUCER = MockCategoryType.createDependency("QMessageProducer");
    public static final MockCategoryType SERVLET = MockCategoryType.createEntryPoint("Servlet");
    public static final MockCategoryType DATABASE = MockCategoryType.createDependency("Database");
    public static final MockCategoryType HTTP_CLIENT = MockCategoryType.createDependency("HttpClient");
    public static final MockCategoryType CONFIG_FILE = MockCategoryType.createSkipComparison("ConfigFile", 40 * ONE_DAY);
    public static final MockCategoryType DYNAMIC_CLASS = MockCategoryType.createSkipComparison("DynamicClass");
    public static final MockCategoryType REDIS = MockCategoryType.createDependency("Redis");
    public static final MockCategoryType DUBBO_PROVIDER = MockCategoryType.createEntryPoint("DubboProvider");
    public static final MockCategoryType DUBBO_CONSUMER = MockCategoryType.createDependency("DubboConsumer");

    static {
        HashSet<MockCategoryType> internalSet = new HashSet<>();
        internalSet.add(Q_MESSAGE_CONSUMER);
        internalSet.add(Q_MESSAGE_PRODUCER);
        internalSet.add(SERVLET);
        internalSet.add(DATABASE);
        internalSet.add(HTTP_CLIENT);
        internalSet.add(CONFIG_FILE);
        internalSet.add(DYNAMIC_CLASS);
        internalSet.add(REDIS);
        internalSet.add(DUBBO_PROVIDER);
        internalSet.add(DUBBO_CONSUMER);
        DEFAULTS = Collections.unmodifiableSet(internalSet);
    }
}