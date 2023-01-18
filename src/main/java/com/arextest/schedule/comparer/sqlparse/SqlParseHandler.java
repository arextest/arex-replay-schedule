package com.arextest.schedule.comparer.sqlparse;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by rchen9 on 2023/1/12.
 */
@Slf4j
@Component
public class SqlParseHandler {

    @Autowired
    List<Parse> sqlParseList;

    private static final String ORIGINAL_SQL = "originalBody";

    public ObjectNode sqlParse(String sql) {
        ObjectNode sqlObj = JsonNodeFactory.instance.objectNode();
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            Parse parse = selectPare(statement);
            sqlObj = parse.parse(statement);
        } catch (Throwable throwable) {
            sqlObj.put(ORIGINAL_SQL, sql);
            LOGGER.warn("sqlParse has exception, sql:{}", sql);
        }
        return sqlObj;
    }

    private Parse selectPare(Statement statement) {
        for (Parse parse : sqlParseList) {
            if (parse.support(statement)) {
                return parse;
            }
        }
        throw new UnsupportedOperationException("not support");
    }
}
