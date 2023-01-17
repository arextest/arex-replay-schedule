package com.arextest.schedule.comparer.sqlparse.action;

import com.arextest.schedule.comparer.sqlparse.Parse;
import com.arextest.schedule.comparer.sqlparse.constants.Constants;
import com.arextest.schedule.comparer.sqlparse.select.ArexSelectVisitorAdapter;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;

/**
 * Created by rchen9 on 2023/1/6.
 */
public class SelectParse implements Parse<Select> {
    @Override
    public Object parse(Select parseObj) {
        ObjectNode sqlObject = JsonNodeFactory.instance.objectNode();
        sqlObject.put(Constants.ACTION, Constants.SELECT);
        SelectBody selectBody = parseObj.getSelectBody();
        ArexSelectVisitorAdapter arexSelectVisitorAdapter = new ArexSelectVisitorAdapter(sqlObject);
        selectBody.accept(arexSelectVisitorAdapter);
        return sqlObject;
    }
}
