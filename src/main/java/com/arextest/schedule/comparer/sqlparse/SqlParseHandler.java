package com.arextest.schedule.comparer.sqlparse;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by rchen9 on 2023/1/12.
 */
@Component
public class SqlParseHandler {

    @Autowired
    List<Parse> sqlParseList;

    private static final String ORIGINAL_SQL = "originalBody";

    public ObjectNode sqlParse(String sql) {
        ObjectNode sqlObj = JsonNodeFactory.instance.objectNode();
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            Parse parse = selectPare(statement.getClass().getSimpleName());
            sqlObj = (ObjectNode) parse.parse(statement);
        } catch (Throwable throwable) {
            sqlObj.put(ORIGINAL_SQL, sql);
        }
        return sqlObj;
    }

    private Parse selectPare(String simpleName) {
        for (Parse parse : sqlParseList) {
            if (parse.support(simpleName)) {
                return parse;
            }
        }
        throw new UnsupportedOperationException("not support");
    }
}
