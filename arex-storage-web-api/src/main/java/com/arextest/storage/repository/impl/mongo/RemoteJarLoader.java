package com.arextest.storage.repository.impl.mongo;

import com.arextest.desensitization.extension.DataDesensitization;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class RemoteJarLoader {
    public static URLClassLoader loadJar(String jarUrl) throws MalformedURLException {
        URL resource;
        if (jarUrl.startsWith("http")) {
            resource = new URL(jarUrl);
        } else {
            resource = RemoteJarLoader.class.getClassLoader().getResource(jarUrl);
        }
        if (resource == null) {
            resource = new File(jarUrl).toURI().toURL();
        }

        return new URLClassLoader(new URL[]{resource});
    }

    public static <T> List<T> loadService(Class<T> clazz, SecureClassLoader classLoader) {
        ServiceLoader<T> serviceLoader = ServiceLoader.load(clazz, classLoader);
        List<T> res = new ArrayList<>();
        for (T service : serviceLoader) {
            res.add(service);
        }
        return res;
    }

    public static void main(String[] args) throws Exception {
        URLClassLoader urlClassLoader = loadJar("http://maven.release.ctripcorp.com/nexus/content/repositories/flightsnapshot/com/arextest/arex-desensitization-core/0.0.0.0-SNAPSHOT/arex-desensitization-core-0.0.0.0-20230817.022218-1-jar-with-dependencies.jar");
        List<DataDesensitization> dataDesensitizations = loadService(DataDesensitization.class, urlClassLoader);
        DataDesensitization dataDesensitization = dataDesensitizations.get(0);
        dataDesensitization.encrypt("123");
    }
}