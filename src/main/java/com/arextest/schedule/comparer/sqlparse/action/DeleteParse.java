package com.arextest.schedule.comparer.sqlparse.action;

import com.arextest.schedule.comparer.sqlparse.Parse;
import com.arextest.schedule.comparer.sqlparse.constants.Constants;
import com.arextest.schedule.comparer.sqlparse.select.ArexExpressionVisitorAdapter;
import com.arextest.schedule.comparer.sqlparse.select.ArexOrderByVisitorAdapter;
import com.arextest.schedule.comparer.sqlparse.select.utils.JoinParseUtil;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.OrderByElement;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by rchen9 on 2023/1/6.
 */

@Component
public class DeleteParse implements Parse<Delete> {
    @Override
    public boolean support(Statement statement) {
        if (statement instanceof Delete) {
            return true;
        }
        return false;
    }

    @Override
    public Object parse(Delete parseObj) {
        ObjectNode sqlObject = JsonNodeFactory.instance.objectNode();
        sqlObject.put(Constants.ACTION, Constants.DELETE);

        // tables parse
        List<Table> tables = parseObj.getTables();
        if (tables != null && !tables.isEmpty()) {
            ObjectNode delTableObj = JsonNodeFactory.instance.objectNode();
            tables.forEach(item -> {
                delTableObj.put(item.getFullyQualifiedName(), Constants.EMPTY);
            });
            sqlObject.put(Constants.DEL_TABLES, delTableObj);
        }

        // table parse
        Table table = parseObj.getTable();
        if (table != null) {
            sqlObject.put(Constants.TABLE, table.getFullyQualifiedName());
        }

        // join parse
        List<Join> joins = parseObj.getJoins();
        if (joins != null && !joins.isEmpty()) {
            ArrayNode joinArr = JsonNodeFactory.instance.arrayNode();
            joins.forEach(item -> {
                joinArr.add(JoinParseUtil.parse(item));
            });
            sqlObject.put(Constants.JOIN, joinArr);
        }

        // where parse
        Expression where = parseObj.getWhere();
        if (where != null) {
            ObjectNode whereObj = JsonNodeFactory.instance.objectNode();
            whereObj.put(Constants.AND_OR, JsonNodeFactory.instance.arrayNode());
            whereObj.put(Constants.COLUMNS, JsonNodeFactory.instance.objectNode());
            where.accept(new ArexExpressionVisitorAdapter(whereObj));
            sqlObject.put(Constants.WHERE, whereObj);
        }

        // orderby parse
        List<OrderByElement> orderByElements = parseObj.getOrderByElements();
        if (orderByElements != null && !orderByElements.isEmpty()) {
            ObjectNode orderByObj = JsonNodeFactory.instance.objectNode();
            ArexOrderByVisitorAdapter arexOrderByVisitorAdapter = new ArexOrderByVisitorAdapter(orderByObj);
            orderByElements.forEach(item -> {
                item.accept(arexOrderByVisitorAdapter);
            });
            sqlObject.put(Constants.ORDER_BY, orderByObj);
        }

        // limit parse
        Limit limit = parseObj.getLimit();
        if (limit != null) {
            sqlObject.put(Constants.LIMIT, limit.toString());
        }
        return sqlObject;
    }
}
