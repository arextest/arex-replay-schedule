package com.arextest.schedule.comparer.sqlparse.select;

import com.arextest.schedule.comparer.sqlparse.constants.Constants;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.sf.jsqlparser.expression.AllValue;
import net.sf.jsqlparser.expression.AnalyticExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.ArrayConstructor;
import net.sf.jsqlparser.expression.ArrayExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.CastExpression;
import net.sf.jsqlparser.expression.CollateExpression;
import net.sf.jsqlparser.expression.ConnectByRootOperator;
import net.sf.jsqlparser.expression.DateTimeLiteralExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.ExtractExpression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.HexValue;
import net.sf.jsqlparser.expression.IntervalExpression;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.JsonAggregateFunction;
import net.sf.jsqlparser.expression.JsonExpression;
import net.sf.jsqlparser.expression.JsonFunction;
import net.sf.jsqlparser.expression.KeepExpression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.MySQLGroupConcat;
import net.sf.jsqlparser.expression.NextValExpression;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.NumericBind;
import net.sf.jsqlparser.expression.OracleHierarchicalExpression;
import net.sf.jsqlparser.expression.OracleHint;
import net.sf.jsqlparser.expression.OracleNamedFunctionParameter;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.RowConstructor;
import net.sf.jsqlparser.expression.RowGetExpression;
import net.sf.jsqlparser.expression.SignedExpression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeKeyExpression;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.TimezoneExpression;
import net.sf.jsqlparser.expression.TryCastExpression;
import net.sf.jsqlparser.expression.UserVariable;
import net.sf.jsqlparser.expression.ValueListExpression;
import net.sf.jsqlparser.expression.VariableAssignment;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.XMLSerializeExpr;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseAnd;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseLeftShift;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseOr;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseRightShift;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseXor;
import net.sf.jsqlparser.expression.operators.arithmetic.Concat;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.IntegerDivision;
import net.sf.jsqlparser.expression.operators.arithmetic.Modulo;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.conditional.XorExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.FullTextSearch;
import net.sf.jsqlparser.expression.operators.relational.GeometryDistance;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsBooleanExpression;
import net.sf.jsqlparser.expression.operators.relational.IsDistinctExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.JsonOperator;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.Matches;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.expression.operators.relational.RegExpMatchOperator;
import net.sf.jsqlparser.expression.operators.relational.RegExpMySQLOperator;
import net.sf.jsqlparser.expression.operators.relational.SimilarToExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.SubSelect;

/**
 * Created by rchen9 on 2023/1/10.
 */
public class ArexExpressionVisitorAdapter implements ExpressionVisitor {

    private ObjectNode sqlObj;
    private ArrayNode andOrObj;
    private ObjectNode columnsObj;

    public ArexExpressionVisitorAdapter(ObjectNode object) {
        sqlObj = object;
        andOrObj = (ArrayNode) object.get(Constants.AND_OR);
        columnsObj = (ObjectNode) object.get(Constants.COLUMNS);
    }

    @Override
    public void visit(BitwiseRightShift aThis) {
        columnsObj.put(aThis.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(BitwiseLeftShift aThis) {
        columnsObj.put(aThis.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(NullValue nullValue) {
        columnsObj.put(nullValue.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(Function function) {
        columnsObj.put(function.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(SignedExpression signedExpression) {
        columnsObj.put(signedExpression.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(JdbcParameter jdbcParameter) {
        columnsObj.put(jdbcParameter.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(JdbcNamedParameter jdbcNamedParameter) {
        columnsObj.put(jdbcNamedParameter.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(DoubleValue doubleValue) {
        columnsObj.put(doubleValue.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(LongValue longValue) {
        columnsObj.put(longValue.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(HexValue hexValue) {
        columnsObj.put(hexValue.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(DateValue dateValue) {
        columnsObj.put(dateValue.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(TimeValue timeValue) {
        columnsObj.put(timeValue.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(TimestampValue timestampValue) {
        columnsObj.put(timestampValue.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(Parenthesis parenthesis) {
        columnsObj.put(parenthesis.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(StringValue stringValue) {
        columnsObj.put(stringValue.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(Addition addition) {
        columnsObj.put(addition.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(Division division) {
        columnsObj.put(division.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(IntegerDivision division) {
        columnsObj.put(division.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(Multiplication multiplication) {
        columnsObj.put(multiplication.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(Subtraction subtraction) {
        columnsObj.put(subtraction.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(AndExpression andExpression) {
        andOrObj.add(Constants.AND);
        ArexExpressionVisitorAdapter arexExpressionVisitorAdapter = new ArexExpressionVisitorAdapter(sqlObj);
        Expression leftExpression = andExpression.getLeftExpression();
        Expression rightExpression = andExpression.getRightExpression();
        leftExpression.accept(arexExpressionVisitorAdapter);
        rightExpression.accept(arexExpressionVisitorAdapter);
    }

    @Override
    public void visit(OrExpression orExpression) {
        andOrObj.add(Constants.OR);
        ArexExpressionVisitorAdapter arexExpressionVisitorAdapter = new ArexExpressionVisitorAdapter(sqlObj);
        Expression leftExpression = orExpression.getLeftExpression();
        Expression rightExpression = orExpression.getRightExpression();
        leftExpression.accept(arexExpressionVisitorAdapter);
        rightExpression.accept(arexExpressionVisitorAdapter);
    }

    @Override
    public void visit(XorExpression orExpression) {
        andOrObj.add(Constants.XOR);
        ArexExpressionVisitorAdapter arexExpressionVisitorAdapter = new ArexExpressionVisitorAdapter(sqlObj);
        Expression leftExpression = orExpression.getLeftExpression();
        Expression rightExpression = orExpression.getRightExpression();
        leftExpression.accept(arexExpressionVisitorAdapter);
        rightExpression.accept(arexExpressionVisitorAdapter);
    }

    @Override
    public void visit(Between between) {
        columnsObj.put(between.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(EqualsTo equalsTo) {
        columnsObj.put(equalsTo.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(GreaterThan greaterThan) {
        columnsObj.put(greaterThan.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(GreaterThanEquals greaterThanEquals) {
        columnsObj.put(greaterThanEquals.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(InExpression inExpression) {
        columnsObj.put(inExpression.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(FullTextSearch fullTextSearch) {
        columnsObj.put(fullTextSearch.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(IsNullExpression isNullExpression) {
        columnsObj.put(isNullExpression.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(IsBooleanExpression isBooleanExpression) {
        columnsObj.put(isBooleanExpression.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(LikeExpression likeExpression) {
        columnsObj.put(likeExpression.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(MinorThan minorThan) {
        columnsObj.put(minorThan.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(MinorThanEquals minorThanEquals) {
        columnsObj.put(minorThanEquals.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(NotEqualsTo notEqualsTo) {
        columnsObj.put(notEqualsTo.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(Column tableColumn) {
        columnsObj.put(tableColumn.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(SubSelect subSelect) {
        columnsObj.put(subSelect.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(CaseExpression caseExpression) {
        columnsObj.put(caseExpression.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(WhenClause whenClause) {
        columnsObj.put(whenClause.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(ExistsExpression existsExpression) {
        columnsObj.put(existsExpression.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(AnyComparisonExpression anyComparisonExpression) {
        columnsObj.put(anyComparisonExpression.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(Concat concat) {
        columnsObj.put(concat.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(Matches matches) {
        columnsObj.put(matches.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(BitwiseAnd bitwiseAnd) {
        columnsObj.put(bitwiseAnd.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(BitwiseOr bitwiseOr) {
        columnsObj.put(bitwiseOr.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(BitwiseXor bitwiseXor) {
        columnsObj.put(bitwiseXor.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(CastExpression cast) {
        columnsObj.put(cast.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(TryCastExpression cast) {
        columnsObj.put(cast.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(Modulo modulo) {
        columnsObj.put(modulo.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(AnalyticExpression aexpr) {
        columnsObj.put(aexpr.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(ExtractExpression eexpr) {
        columnsObj.put(eexpr.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(IntervalExpression iexpr) {
        columnsObj.put(iexpr.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(OracleHierarchicalExpression oexpr) {
        columnsObj.put(oexpr.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(RegExpMatchOperator rexpr) {
        columnsObj.put(rexpr.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(JsonExpression jsonExpr) {
        columnsObj.put(jsonExpr.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(JsonOperator jsonExpr) {
        columnsObj.put(jsonExpr.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(RegExpMySQLOperator regExpMySQLOperator) {
        columnsObj.put(regExpMySQLOperator.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(UserVariable var) {
        columnsObj.put(var.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(NumericBind bind) {
        columnsObj.put(bind.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(KeepExpression aexpr) {
        columnsObj.put(aexpr.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(MySQLGroupConcat groupConcat) {
        columnsObj.put(groupConcat.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(ValueListExpression valueList) {
        columnsObj.put(valueList.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(RowConstructor rowConstructor) {
        columnsObj.put(rowConstructor.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(RowGetExpression rowGetExpression) {
        columnsObj.put(rowGetExpression.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(OracleHint hint) {
        columnsObj.put(hint.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(TimeKeyExpression timeKeyExpression) {
        columnsObj.put(timeKeyExpression.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(DateTimeLiteralExpression literal) {
        columnsObj.put(literal.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(NotExpression aThis) {
        columnsObj.put(aThis.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(NextValExpression aThis) {
        columnsObj.put(aThis.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(CollateExpression aThis) {
        columnsObj.put(aThis.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(SimilarToExpression aThis) {
        columnsObj.put(aThis.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(ArrayExpression aThis) {
        columnsObj.put(aThis.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(ArrayConstructor aThis) {
        columnsObj.put(aThis.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(VariableAssignment aThis) {
        columnsObj.put(aThis.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(XMLSerializeExpr aThis) {
        columnsObj.put(aThis.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(TimezoneExpression aThis) {
        columnsObj.put(aThis.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(JsonAggregateFunction aThis) {
        columnsObj.put(aThis.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(JsonFunction aThis) {
        columnsObj.put(aThis.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(ConnectByRootOperator aThis) {
        columnsObj.put(aThis.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(OracleNamedFunctionParameter aThis) {
        columnsObj.put(aThis.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(AllColumns allColumns) {
        columnsObj.put(allColumns.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(AllTableColumns allTableColumns) {
        columnsObj.put(allTableColumns.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(AllValue allValue) {
        columnsObj.put(allValue.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(IsDistinctExpression isDistinctExpression) {
        columnsObj.put(isDistinctExpression.toString(), Constants.EMPTY);
    }

    @Override
    public void visit(GeometryDistance geometryDistance) {
        columnsObj.put(geometryDistance.toString(), Constants.EMPTY);
    }
}
