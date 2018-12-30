/*
 * Copyright 2013-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.honey.osql.chain;

import org.bee.osql.Op;
import org.bee.osql.OrderType;
import org.bee.osql.chain.Select;

/**
 * @author Kingstar
 * @since  1.3
 */
public class SelectImpl extends AbstractSelectToSql implements Select {

	private static String STAR = "*";
	private static String DISTINCT = "distinct";
	private static String L_PARENTHESES = "(";
	private static String R_PARENTHESES = ")";
	private static String COMMA = ",";
	private static String ONE_SPACE = " ";

//	private StringBuffer sql = new StringBuffer("select ");

	private boolean isStartField = true;
	private boolean isStartWhere = true;
	private boolean isStartOn = true;

	private boolean isAddAnd = false;
	private boolean isStartGroupBy = true;
	private boolean isStartHaving = true;
	private boolean isStartOrderBy = true;
	
	public SelectImpl(){
		sql.append("select ");
	}
	
//	private int start;
//	private int size;

	public Select select() {
		if (isStartField) {
			sql.append(STAR);
			isStartField = false;
		}
		return this;
	}

	public Select select(String column) {
		if (isStartField) {
			sql.append(column);
			isStartField = false;
		} else {
			sql.append(COMMA);
			sql.append(column);
		}

		return this;
	}

	public Select distinct(String field) {
		return select(DISTINCT + L_PARENTHESES + field + R_PARENTHESES); // DISTINCT(field)
	}

	// public String selectWithFun(String fieldForFun, FunctionType
	// functionType) {
	// return functionType.getName() + L_PARENTHESES + fieldForFun+
	// R_PARENTHESES; // eg. sum(price)
	// }

	public Select from(String table) {
		sql.append(" from ");
		sql.append(table);
		return this;
	}

	public Select join(String anotherTable) {
		sql.append(" join ");
		sql.append(anotherTable);
		return this;
	}

	public Select innerjoin(String anotherTable) {
		sql.append(" inner join ");
		sql.append(anotherTable);
		return this;
	}

	public Select leftjoin(String anotherTable) {
		sql.append(" left join ");
		sql.append(anotherTable);
		return this;
	}

	public Select rightjoin(String anotherTable) {
		sql.append(" right join ");
		sql.append(anotherTable);
		return this;
	}

	public Select on() {
		sql.append(" on ");
		isStartOn = false;

		return this;
	}

	public Select on(String expression) {
		if (isStartOn) {
			sql.append(" on ");
			sql.append(expression);
			isStartOn = false;
		} else {
			if (isAddAnd) sql.append(" and ");
			sql.append(expression);
			isAddAnd = true;
		}

		return this;
	}


	public Select between(String field, Number low, Number high) {

		if (isAddAnd) sql.append(" and ");
		sql.append(field);
		sql.append(" between ");
		sql.append(low);
		sql.append(" and ");
		sql.append(high);
		isAddAnd = true;

		return this;
	}

	public Select notBetween(String field, Number low, Number high) {

		if (isAddAnd) sql.append(" and ");
		sql.append(field);
		sql.append(" not between ");
		sql.append(low);
		sql.append(" and ");
		sql.append(high);
		isAddAnd = true;

		return this;
	}

	public Select isNull(String field) {
		if (isAddAnd) sql.append(" and ");
		sql.append(field + " is null ");
		return this;
	}

	public Select isNotNull(String field) {
		if (isAddAnd) sql.append(" and ");
		sql.append(field + " is not null ");
		return this;
	}

	public Select in(String field, Number... valueList) {
		return inOrNotIn(field, "in", valueList);
	}

	public Select notIn(String field, Number... valueList) {
		return inOrNotIn(field, "not in", valueList);
	}

	private Select inOrNotIn(String field, String op, Number... valueList) {
		if (isAddAnd) sql.append(" and ");
		String value = "";
		for (int i = 0; i < valueList.length; i++) {
			if (i == 0)
				value += valueList[i];
			else
				value += "," + valueList[i];
		}
		sql.append(field + " " + op + " (" + value + ")"); // in (99,18)
		return this;
	}

	public Select in(String field, String valueList) {
		return inOrNotIn(field, "in", valueList);
	}

	public Select notIn(String field, String valueList) {
		return inOrNotIn(field, "not in", valueList);
	}

	private Select inOrNotIn(String field, String op, String valueList) {
		if (isAddAnd) sql.append(" and ");
		valueList = valueList.replace(",", "','");
		sql.append(field + " " + op + " ('" + valueList + "')"); // in ('client01','bee')
		return this;
	}

	public Select groupBy(String field) {
		if (isStartGroupBy) {
			sql.append(" group by ");
			sql.append(field);
			isStartGroupBy = false;

		} else {
			sql.append(COMMA);
			sql.append(field);
		}
		return this;
	}

	public Select having(String expression) {
		// if(isStartGroupBy) //throw Exception;
		// throw new Exception();
		// throw new StringIndexOutOfBoundsException(1);
		if (isStartHaving) {
			sql.append(" having ");
			sql.append(expression);
			isStartHaving = false;
		} else {
			sql.append(" and ");
			sql.append(expression);
		}

		return this;
	}

	public Select orderBy(String field) {

		if (isStartOrderBy) {
			sql.append(" order by ");
			sql.append(field);
			isStartOrderBy = false;

		} else {
			sql.append(COMMA);
			sql.append(field);
		}
		return this;
	}

	public Select orderBy(String field, OrderType orderType) {
		if (isStartOrderBy) {
			sql.append(" order by ");
			sql.append(field);
			sql.append(ONE_SPACE);
			sql.append(orderType.getName());
			isStartOrderBy = false;
		} else {
			sql.append(COMMA);
			sql.append(field);
			sql.append(ONE_SPACE);
			sql.append(orderType.getName());
		}
		return this;
	}
	
	private Select useSubSelect(String keyword, String subSelect) { // in ,exists

		sql.append(ONE_SPACE);
		sql.append(keyword);
		sql.append(ONE_SPACE);
		sql.append(L_PARENTHESES);
		sql.append(subSelect);
		sql.append(R_PARENTHESES);
		return this;
	}

	public Select exists(Select subSelect) {
		return exists(subSelect.toSQL(true));
	}

	public Select exists(String subSelect) {
		return useSubSelect("exists", subSelect);
	}

	public Select notExists(Select subSelect) {
		return notExists(subSelect.toSQL(true));
	}

	public Select notExists(String subSelect) {
		return useSubSelect("not exists", subSelect);
	}

	public Select in(Select subSelect) {
		return in(subSelect.toSQL(true));
	}

	public Select in(String subSelect) {
		return useSubSelect("in", subSelect);
	}

	public Select notIn(Select subSelect) {
		return notIn(subSelect.toSQL(true));
	}

	public Select notIn(String subSelect) {
		return useSubSelect("not in", subSelect);
	}

	public Select start(int start) {
		this.start=start;
		return this;
	}

	public Select size(int size) {
		this.size=size;
		return this;
	}
	
	public Select lParentheses() {
		sql.append(L_PARENTHESES);
		return this;
	}

	public Select rParentheses() {
		sql.append(R_PARENTHESES);
		return this;
	}
	
	
 //Condition<<=============
	public Select where() {
		sql.append(" where ");
		isStartWhere = false;

		return this;
	}

	@Override
	public Select where(String expression) {
		if (isStartWhere) {
			sql.append(" where ");
			sql.append(expression);
			isStartWhere = false;
		} else {
			if (isAddAnd) sql.append(" and ");
			sql.append(expression);
			isAddAnd = true;
		}

		return this;
	}

	public Select op(String field, Op opType, String value) {

		if (opType == Op.in) return in(field, value);
		if (opType == Op.notIn) return notIn(field, value);

		if (isAddAnd) sql.append(" and ");

		sql.append(field);
		sql.append(opType.getOperator());
		sql.append("'");
		sql.append(value);
		sql.append("'");
		isAddAnd = true;
		return this;
	}

	public Select op(String field, Op opType, Number value) {
		if (opType == Op.in) return in(field, value);
		if (opType == Op.notIn) return notIn(field, value);

		if (isAddAnd) sql.append(" and ");
		sql.append(field);
		sql.append(opType.getOperator());
		sql.append(value);
		isAddAnd = true;
		return this;
	}

	/**
	 * 默认自动加 and default will automatically add and
	 * 
	 * @return
	 */
	public Select and() {
		sql.append(" and ");
		isAddAnd = false;
		return this;
	}

	public Select or() {
		sql.append(" or ");
		isAddAnd = false;
		return this;
	}
	 //=============>>
}
