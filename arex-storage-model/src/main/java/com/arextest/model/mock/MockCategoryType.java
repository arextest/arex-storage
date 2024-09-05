package com.arextest.model.mock;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "name")
public class MockCategoryType {

  public static final Set<MockCategoryType> DEFAULTS;
  public static final MockCategoryType Q_MESSAGE_CONSUMER = MockCategoryType.createSkipComparisonEntrance(
      "QMessageConsumer");
  public static final MockCategoryType Q_MESSAGE_PRODUCER = MockCategoryType.createDependency(
      "QMessageProducer");
  public static final MockCategoryType SERVLET = MockCategoryType.createEntryPoint("Servlet");
  public static final MockCategoryType NETTY_PROVIDER = MockCategoryType.createEntryPoint("NettyProvider");
  public static final MockCategoryType DATABASE = MockCategoryType.createDependency("Database");
  public static final MockCategoryType HTTP_CLIENT = MockCategoryType.createDependency(
      "HttpClient");
  public static final MockCategoryType CONFIG_FILE = MockCategoryType.createSkipComparison(
      "ConfigFile");
  public static final MockCategoryType DYNAMIC_CLASS = MockCategoryType.createSkipComparison(
      "DynamicClass");
  public static final MockCategoryType REDIS = MockCategoryType.createDependency("Redis");
  public static final MockCategoryType DUBBO_PROVIDER = MockCategoryType.createEntryPoint(
      "DubboProvider");
  public static final MockCategoryType DUBBO_CONSUMER = MockCategoryType.createDependency(
      "DubboConsumer");
  public static final MockCategoryType COVERAGE = MockCategoryType.createCoverage("Coverage");
  public static final MockCategoryType RECORDING_SCENE = MockCategoryType.createCoverage("RecordingScene");
  public static final MockCategoryType REPLAY_SCENE = MockCategoryType.createCoverage("ReplayScene");

  static {
    Set<MockCategoryType> internalSet = new LinkedHashSet<>();
    internalSet.add(SERVLET);
    internalSet.add(NETTY_PROVIDER);
    internalSet.add(DUBBO_PROVIDER);
    internalSet.add(Q_MESSAGE_CONSUMER);

    internalSet.add(Q_MESSAGE_PRODUCER);
    internalSet.add(DATABASE);
    internalSet.add(HTTP_CLIENT);
    internalSet.add(CONFIG_FILE);
    internalSet.add(DYNAMIC_CLASS);
    internalSet.add(REDIS);
    internalSet.add(DUBBO_CONSUMER);
    DEFAULTS = Collections.unmodifiableSet(internalSet);
  }

  private String name;
  private boolean entryPoint;
  private boolean skipComparison;

  public static MockCategoryType createEntryPoint(String name) {
    return new MockCategoryType(name, true, false);
  }

  public static MockCategoryType createSkipComparison(String name) {
    return new MockCategoryType(name, false, true);
  }

  public static MockCategoryType createSkipComparisonEntrance(String name) {
    return new MockCategoryType(name, true, true);
  }

  public static MockCategoryType createDependency(String name) {
    return new MockCategoryType(name, false, false);
  }

  public static MockCategoryType createCoverage(String name) {
    return new MockCategoryType(name, false, true);
  }

  public static MockCategoryType create(String name) {
    for (MockCategoryType categoryType : DEFAULTS) {
      if (Objects.equals(categoryType.name, name)) {
        return categoryType;
      }
    }
    return createDependency(name);
  }

  @Override
  public String toString() {
    return this.name;
  }
}