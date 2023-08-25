package com.arextest.storage.beans;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class GetBeanFromIOC implements ApplicationContextAware {
    public static ApplicationContext applicationContext;

    // Spring 容器会在创建该 Bean 时，自动调用该 Bean 的setApplicationContext方法，并把 ApplicationContext 传过来
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        if (GetBeanFromIOC.applicationContext == null) {
            GetBeanFromIOC.applicationContext = applicationContext;
        }
    }

    public static Object getBean(String name) {
        return applicationContext.getBean(name);
    }

    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    public static <T> T getBean(String name, Class<T> clazz) {
        return applicationContext.getBean(name, clazz);
    }

}
