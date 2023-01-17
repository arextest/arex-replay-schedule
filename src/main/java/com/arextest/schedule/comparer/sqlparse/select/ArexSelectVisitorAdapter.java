package com.arextest.schedule.comparer.sqlparse.select;

import com.arextest.schedule.comparer.sqlparse.constants.Constants;
import com.arextest.schedule.comparer.sqlparse.select.utils.JoinParseUtil;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Distinct;
import net.sf.jsqlparser.statement.select.Fetch;
import net.sf.jsqlparser.statement.select.First;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.Offset;
import net.sf.jsqlparser.statement.select.OptimizeFor;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectVisitor;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.Skip;
import net.sf.jsqlparser.statement.select.Top;
import net.sf.jsqlparser.statement.select.Wait;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.statement.values.ValuesStatement;

import java.util.List;

/**
 * Created by rchen9 on 2023/1/9.
 */
public class ArexSelectVisitorAdapter implements SelectVisitor {

    private ObjectNode sqlObj;

    public ArexSelectVisitorAdapter(ObjectNode object) {
        sqlObj = object;
    }

    @Override
    public void visit(PlainSelect plainSelect) {
        // distinct parse
        Distinct distinct = plainSelect.getDistinct();
        if (distinct != null) {
            sqlObj.put(Constants.DISTINCT, distinct.toString());
        }

        // skip parse
        Skip skip = plainSelect.getSkip();
        if (skip != null) {
            sqlObj.put(Constants.SKIP, skip.toString());
        }

        // top parse
        Top top = plainSelect.getTop();
        if (top != null) {
            sqlObj.put(Constants.TOP, top.toString());
        }

        // first parse
        First first = plainSelect.getFirst();
        if (first != null) {
            sqlObj.put(Constants.FIRST, first.toString());
        }

        // selectItems parse
        List<SelectItem> selectItems = plainSelect.getSelectItems();
        if (selectItems != null && !selectItems.isEmpty()) {
            ObjectNode columnsObj = JsonNodeFactory.instance.objectNode();
            ArexSelectItemVisitorAdapter arexSelectItemVisitorAdapter = new ArexSelectItemVisitorAdapter(columnsObj);
            selectItems.forEach(selectItem -> {
                selectItem.accept(arexSelectItemVisitorAdapter);
            });
            sqlObj.put(Constants.COLUMNS, columnsObj);
        }

        // into parse
        List<Table> intoTables = plainSelect.getIntoTables();
        if (intoTables != null && !intoTables.isEmpty()) {
            sqlObj.put(Constants.INTO, intoTables.toString());
        }

        // fromItem parse
        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem != null) {
            ObjectNode fromObj = JsonNodeFactory.instance.objectNode();
            ArexFromItemVisitorAdapter arexFromItemVisitorAdapter = new ArexFromItemVisitorAdapter(fromObj);
            fromItem.accept(arexFromItemVisitorAdapter);
            sqlObj.put(Constants.FROM, fromObj);
        }

        // jonis parse
        List<Join> joins = plainSelect.getJoins();
        if (joins != null && !joins.isEmpty()) {
            ArrayNode joinArr = JsonNodeFactory.instance.arrayNode();
            joins.forEach(item -> {
                joinArr.add(JoinParseUtil.parse(item));
            });
            sqlObj.put(Constants.JOIN, joinArr);
        }

        // where parse
        Expression where = plainSelect.getWhere();
        if (where != null) {
            ObjectNode whereObj = JsonNodeFactory.instance.objectNode();
            whereObj.put(Constants.AND_OR, JsonNodeFactory.instance.arrayNode());
            whereObj.put(Constants.COLUMNS, JsonNodeFactory.instance.objectNode());
            ArexExpressionVisitorAdapter arexExpressionVisitorAdapter = new ArexExpressionVisitorAdapter(whereObj);
            where.accept(arexExpressionVisitorAdapter);
            sqlObj.put(Constants.WHERE, whereObj);
        }

        // group by parse
        GroupByElement groupBy = plainSelect.getGroupBy();
        if (groupBy != null) {
            sqlObj.put(Constants.GROUP_BY, groupBy.toString());
        }

        // having parse
        Expression having = plainSelect.getHaving();
        if (having != null) {
            ObjectNode havingObj = JsonNodeFactory.instance.objectNode();
            havingObj.put(Constants.AND_OR, JsonNodeFactory.instance.arrayNode());
            havingObj.put(Constants.COLUMNS, JsonNodeFactory.instance.objectNode());
            ArexExpressionVisitorAdapter arexExpressionVisitorAdapter = new ArexExpressionVisitorAdapter(havingObj);
            having.accept(arexExpressionVisitorAdapter);
            sqlObj.put(Constants.HAVING, havingObj);
        }

        // order by parse
        List<OrderByElement> orderByElements = plainSelect.getOrderByElements();
        if (orderByElements != null && !orderByElements.isEmpty()) {
            ObjectNode orderByObj = JsonNodeFactory.instance.objectNode();
            ArexOrderByVisitorAdapter arexOrderByVisitorAdapter = new ArexOrderByVisitorAdapter(orderByObj);
            orderByElements.forEach(item -> {
                item.accept(arexOrderByVisitorAdapter);
            });
            sqlObj.put(Constants.ORDER_BY, orderByObj);
        }

        // fetch parse
        Fetch fetch = plainSelect.getFetch();
        if (fetch != null) {
            sqlObj.put(Constants.FETCH, fetch.toString());
        }
        // optimizeFor parse
        OptimizeFor optimizeFor = plainSelect.getOptimizeFor();
        if (optimizeFor != null) {
            sqlObj.put(Constants.OPTIMIZE_FOR, optimizeFor.toString());
        }

        // limit parse
        Limit limit = plainSelect.getLimit();
        if (limit != null) {
            sqlObj.put(Constants.LIMIT, limit.toString());
        }

        // offset parse
        Offset offset = plainSelect.getOffset();
        if (offset != null) {
            sqlObj.put(Constants.OFFSET, offset.toString());
        }

        // forUpdate parse
        boolean forUpdate = plainSelect.isForUpdate();
        if (forUpdate) {
            sqlObj.put(Constants.FOR_UPDATE, true);
        }

        // forUpdateTable parse
        Table forUpdateTable = plainSelect.getForUpdateTable();
        if (forUpdateTable != null) {
            sqlObj.put(Constants.FOR_UPDATE_TABLE, forUpdateTable.toString());
        }

        // noWait parse
        boolean noWait = plainSelect.isNoWait();
        if (noWait) {
            sqlObj.put(Constants.NO_WAIT, true);
        }

        // wait parse
        Wait wait = plainSelect.getWait();
        if (wait != null) {
            sqlObj.put(Constants.WAIT, wait.toString());
        }

    }

    @Override
    public void visit(SetOperationList setOperationList) {
        sqlObj.put("setOperationList", setOperationList.toString());
    }

    @Override
    public void visit(WithItem withItem) {
        sqlObj.put("withItem", withItem.toString());
    }

    @Override
    public void visit(ValuesStatement valuesStatement) {
        sqlObj.put("valuesStatement", valuesStatement.toString());
    }
}
