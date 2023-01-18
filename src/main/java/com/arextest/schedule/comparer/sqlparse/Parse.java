package com.arextest.schedule.comparer.sqlparse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.sf.jsqlparser.statement.Statement;

public interface Parse<T> {

    boolean support(Statement statement);

    ObjectNode parse(T parseObj);
}
