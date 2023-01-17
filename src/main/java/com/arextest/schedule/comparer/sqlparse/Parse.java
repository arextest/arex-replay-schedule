package com.arextest.schedule.comparer.sqlparse;

import net.sf.jsqlparser.statement.Statement;

public interface Parse<T> {

    boolean support(Statement statement);

    Object parse(T parseObj);
}
