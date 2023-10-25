package com.arextest.storage.beans;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class ExecutorsConfiguration implements Thread.UncaughtExceptionHandler {
    @Bean
    public ThreadPoolExecutor coverageHandlerExecutor() {
        return new ThreadPoolExecutor(4, 4,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                createThreadFac("coverage-handler-%d"),
                new ThreadPoolExecutor.CallerRunsPolicy());
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
