package com.arextest.storage.sqlparse;

/**
 * Created by rchen9 on 2023/1/6.
 */
public interface Parse<T> {
    Object parse(T parseObj);
}
