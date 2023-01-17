package com.arextest.schedule.comparer.sqlparse;

import com.arextest.schedule.comparer.sqlparse.action.ActionFactory;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;

/**
 * Created by rchen9 on 2023/1/12.
 */
public class SqlParseUtil {

    private static final String ORIGINAL_SQL = "originalBody";


    public static ObjectNode sqlParse(String sql) {
        ObjectNode sqlObj = JsonNodeFactory.instance.objectNode();
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            Parse parse = ActionFactory.selectParse(statement);
            sqlObj = (ObjectNode) parse.parse(statement);
        } catch (Throwable throwable) {
            sqlObj.put(ORIGINAL_SQL, sql);
        }
        return sqlObj;
    }
}
