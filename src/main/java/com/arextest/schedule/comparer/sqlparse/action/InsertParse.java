package com.arextest.schedule.comparer.sqlparse.action;

import com.arextest.schedule.comparer.sqlparse.Parse;
import com.arextest.schedule.comparer.sqlparse.constants.Constants;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.insert.Insert;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by rchen9 on 2023/1/6.
 */
@Component
public class InsertParse implements Parse<Insert> {
    @Override
    public boolean support(String simpleName) {
        if ("Insert".equals(simpleName)) {
            return true;
        }
        return false;
    }

    @Override
    public Object parse(Insert parseObj) {
        ObjectNode sqlObject = JsonNodeFactory.instance.objectNode();
        sqlObject.put(Constants.ACTION, Constants.INSERT);

        // table parse
        Table table = parseObj.getTable();
        if (table != null) {
            sqlObject.put(Constants.TABLE, table.getFullyQualifiedName());
        }

        // columns parse
        List<Column> columns = parseObj.getColumns();
        if (columns != null && !columns.isEmpty()) {
            ObjectNode columnsObj = JsonNodeFactory.instance.objectNode();
            columns.forEach(item -> {
                columnsObj.put(item.toString(), Constants.EMPTY);
            });
            sqlObject.put(Constants.COLUMNS, columnsObj);
        }

        // setColumns parse
        List<Column> setColumns = parseObj.getSetColumns();
        if (setColumns != null && !setColumns.isEmpty()) {
            ObjectNode setColumnsObj = JsonNodeFactory.instance.objectNode();
            setColumns.forEach(item -> {
                setColumnsObj.put(item.toString(), Constants.EMPTY);
            });
            sqlObject.put(Constants.COLUMNS, setColumnsObj);
        }

        return sqlObject;
    }
}
