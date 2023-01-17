package com.arextest.schedule.comparer.sqlparse.select;

import com.arextest.schedule.comparer.sqlparse.constants.Constants;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItemVisitor;
import net.sf.jsqlparser.statement.select.LateralSubSelect;
import net.sf.jsqlparser.statement.select.ParenthesisFromItem;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.TableFunction;
import net.sf.jsqlparser.statement.select.ValuesList;

/**
 * Created by rchen9 on 2023/1/9.
 */
public class ArexFromItemVisitorAdapter implements FromItemVisitor {

    private ObjectNode sqlObj;

    public ArexFromItemVisitorAdapter(ObjectNode object) {
        sqlObj = object;
    }


    @Override
    public void visit(Table table) {
        // partItems parse
        sqlObj.put(Constants.TABLE, table.getFullyQualifiedName());

        // alias parse
        Alias alias = table.getAlias();
        if (alias != null) {
            sqlObj.put(Constants.ALIAS, alias.toString());
        }
    }

    @Override
    public void visit(SubSelect subSelect) {
        // SelectBody parse
        SelectBody selectBody = subSelect.getSelectBody();
        if (selectBody != null) {
            ObjectNode tempSelectBodyObj = JsonNodeFactory.instance.objectNode();
            ArexSelectVisitorAdapter arexSelectVisitorAdapter = new ArexSelectVisitorAdapter(tempSelectBodyObj);
            selectBody.accept(arexSelectVisitorAdapter);
            sqlObj.put(Constants.TABLE, tempSelectBodyObj);
        }

        // alias parse
        Alias alias = subSelect.getAlias();
        if (alias != null) {
            sqlObj.put(Constants.ALIAS, alias.toString());
        }
    }

    @Override
    public void visit(SubJoin subJoin) {
        sqlObj.put(Constants.TABLE, subJoin.toString());
    }

    @Override
    public void visit(LateralSubSelect lateralSubSelect) {
        sqlObj.put(Constants.TABLE, lateralSubSelect.toString());
    }

    @Override
    public void visit(ValuesList valuesList) {
        sqlObj.put(Constants.TABLE, valuesList.toString());
    }

    @Override
    public void visit(TableFunction tableFunction) {
        sqlObj.put(Constants.TABLE, tableFunction.toString());
    }

    @Override
    public void visit(ParenthesisFromItem parenthesisFromItem) {
        sqlObj.put(Constants.TABLE, parenthesisFromItem.toString());
    }
}
