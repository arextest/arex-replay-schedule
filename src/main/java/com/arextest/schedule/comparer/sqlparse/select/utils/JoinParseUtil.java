package com.arextest.schedule.comparer.sqlparse.select.utils;

import com.arextest.schedule.comparer.sqlparse.constants.Constants;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;

import java.util.Collection;
import java.util.List;

/**
 * Created by rchen9 on 2023/1/10.
 */
public class JoinParseUtil {
    public static ObjectNode parse(Join parseObj) {
        // JSONObject res = new JSONObject();
        ObjectNode res = JsonNodeFactory.instance.objectNode();

        // join type parse
        res.put(Constants.TYPE, getJOINType(parseObj));

        // rightItem parse
        FromItem rightItem = parseObj.getRightItem();
        if (rightItem != null) {
            res.put(Constants.TABLE, rightItem.toString());
        }

        // onExpressions parse
        Collection<Expression> onExpressions = parseObj.getOnExpressions();
        if (onExpressions != null && !onExpressions.isEmpty()) {
            // JSONObject onObj = new JSONObject();
            ObjectNode onObj = JsonNodeFactory.instance.objectNode();
            onExpressions.forEach(item -> {
                onObj.put(item.toString(), Constants.EMPTY);
            });
            res.put(Constants.ON, onObj);
        }

        // usingColumns parse
        List<Column> usingColumns = parseObj.getUsingColumns();
        if (usingColumns != null && !usingColumns.isEmpty()) {
            // JSONObject usingObj = new JSONObject();
            ObjectNode usingObj = JsonNodeFactory.instance.objectNode();
            usingColumns.forEach(item -> {
                usingObj.put(item.toString(), Constants.EMPTY);
            });
            res.put(Constants.USING, usingObj);
        }
        return res;
    }

    private static String getJOINType(Join parseObj) {
        StringBuilder builder = new StringBuilder();
        if (parseObj.isSimple() && parseObj.isOuter()) {
            builder.append("OUTER JOIN");
        } else if (parseObj.isSimple()) {
            builder.append("");
        } else {
            if (parseObj.isNatural()) {
                builder.append("NATURAL ");
            }

            if (parseObj.isRight()) {
                builder.append("RIGHT ");
            } else if (parseObj.isFull()) {
                builder.append("FULL ");
            } else if (parseObj.isLeft()) {
                builder.append("LEFT ");
            } else if (parseObj.isCross()) {
                builder.append("CROSS ");
            }

            if (parseObj.isOuter()) {
                builder.append("OUTER ");
            } else if (parseObj.isInner()) {
                builder.append("INNER ");
            } else if (parseObj.isSemi()) {
                builder.append("SEMI ");
            }

            if (parseObj.isStraight()) {
                builder.append("STRAIGHT_JOIN ");
            } else if (parseObj.isApply()) {
                builder.append("APPLY ");
            } else {
                builder.append("JOIN");
            }
        }
        return builder.toString();
    }
}
