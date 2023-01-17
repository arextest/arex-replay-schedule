package com.arextest.schedule.comparer.sqlparse;

public interface Parse<T> {

    boolean support(String simpleName);

    Object parse(T parseObj);
}
