package com.arextest.storage.beans;

import com.alibaba.ttl.threadpool.TtlExecutors;
import com.arextest.common.config.DefaultApplicationConfig;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ExecutorsConfiguration implements Thread.UncaughtExceptionHandler {

  private static final int CORE_POOL_SIZE = 400;
  private static final long KEEP_ALIVE_TIME = 60L;
  private static final String COVERAGE_HANDLER_EXECUTOR_CORE_POOL_SIZE = "coverage.handler.executor.core.pool.size";
  private static final int DEFAULT_CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
  @Resource
  private DefaultApplicationConfig defaultApplicationConfig;

  @Bean
  public ScheduledExecutorService coverageHandleDelayedPool() {
    int corePoolSize = defaultApplicationConfig.getConfigAsInt(COVERAGE_HANDLER_EXECUTOR_CORE_POOL_SIZE,
        2 * DEFAULT_CORE_POOL_SIZE);
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(corePoolSize,
        createThreadFac("coverage-handler-%d"),
        new DiscardPolicy());
    return TtlExecutors.getTtlScheduledExecutorService(executor);
  }

  @Bean
  public ExecutorService envUpdateHandlerExecutor() {
    ExecutorService executor = new ThreadPoolExecutor(1, 4,
        1000, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(200),
        createThreadFac("envUpdate-handler-%d"),
        new ThreadPoolExecutor.AbortPolicy());
    return TtlExecutors.getTtlExecutorService(executor);
  }

  @Bean(name = "custom-fork-join-executor")
  public ExecutorService customForkJoinExecutor() {
    int parallelism = Runtime.getRuntime().availableProcessors();
    ExecutorService executorService = new ThreadPoolExecutor(parallelism, parallelism,
        60, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1000),
        createThreadFac("forkJoin-handler-%d"),
        new CallerRunsPolicy());
    return TtlExecutors.getTtlExecutorService(executorService);
  }

  /**
   * In the current scenario, the time consumption of a single save interface is 2ms,
   * the qps is around 400, and this interface is an IO-intensive interface,
   * so the number of core threads is set to 400
   * @return
   */
  @Bean
  public ExecutorService batchSaveExecutor() {
    ExecutorService executorService = new ThreadPoolExecutor(CORE_POOL_SIZE, CORE_POOL_SIZE * 2,
        KEEP_ALIVE_TIME, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(100),
        createThreadFac("batchSave-executor-%d"));
    return TtlExecutors.getTtlExecutorService(executorService);
  }

  private ThreadFactory createThreadFac(String namePattern) {
    return new ThreadFactoryBuilder().setNameFormat(namePattern)
        .setDaemon(true)
        .setUncaughtExceptionHandler(this)
        .build();
  }

  @Override
  public void uncaughtException(Thread t, Throwable e) {
    LOGGER.error("uncaught exception in thread:{}", t.getName(), e);
  }
}
