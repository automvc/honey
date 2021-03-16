/*
 * Copyright 2013-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.chain;

import org.teasoft.bee.osql.Op;
import org.teasoft.bee.osql.OrderType;
import org.teasoft.bee.osql.chain.Select;
import org.teasoft.bee.osql.exception.BeeErrorFieldException;
import org.teasoft.honey.osql.core.CheckField;

/**
 * @author Kingstar
 * @since  1.3
 */
public class SelectImpl extends AbstractSelectToSql implements Select {

	private static final String STAR = "*";
	private static final String DISTINCT = "distinct";
	private static final String L_PARENTHESES = "(";
	private static final String R_PARENTHESES = ")";
	private static final String COMMA = ",";
	private static final String ONE_SPACE = " ";

//	private StringBuffer sql = new StringBuffer("select ");

	private boolean isStartField = true;
	private boolean isStartWhere = true;
	private boolean isStartOn = true;

	private boolean isAddAnd = false;
	private boolean isStartGroupBy = true;
	private boolean isStartHaving = true;
	private boolean isStartOrderBy = true;
	
	public SelectImpl(){
// some one maybe just need where part
//		sql.append("select ");
	}
	
//	private int start;
//	private int size;

	@Override
	public Select select() {
		if (isStartField) {
			sql.append("select ");
			sql.append(STAR); //*
			isStartField = false;
		}
		return this;
	}

	@Override
	public Select select(String column) {
		checkField(column);
		if (isStartField) {
			sql.append("select ");
			sql.append(column);
			isStartField = false;
		} else {
			sql.append(COMMA);
			sql.append(column);
		}

		return this;
	}

	@Override
	public Select distinct(String field) {
		checkField(field);
		return select(DISTINCT + L_PARENTHESES + field + R_PARENTHESES); // DISTINCT(field)
	}

	// public String selectWithFun(String fieldForFun, FunctionType
	// functionType) {
	// return functionType.getName() + L_PARENTHESES + fieldForFun+
	// R_PARENTHESES; // eg. sum(price)
	// }

	@Override
	public Select from(String table) {
//		checkField(table);
		sql.append(" from ");
		sql.append(table);
		return this;
	}

	@Override
	public Select join(String anotherTable) {
//		checkField(anotherTable);
		sql.append(" join ");
		sql.append(anotherTable);
		return this;
	}

	@Override
	public Select innerjoin(String anotherTable) {
		checkField(anotherTable);
		sql.append(" inner join ");
		sql.append(anotherTable);
		return this;
	}

	@Override
	public Select leftjoin(String anotherTable) {
		checkField(anotherTable);
		sql.append(" left join ");
		sql.append(anotherTable);
		return this;
	}

	@Override
	public Select rightjoin(String anotherTable) {
		checkField(anotherTable);
		sql.append(" right join ");
		sql.append(anotherTable);
		return this;
	}

	@Override
	public Select on() {
		sql.append(" on ");
		isStartOn = false;

		return this;
	}

	@Override
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

	@Override
	public Select between(String field, Number low, Number high) {
		checkField(field);

		if (isAddAnd) sql.append(" and ");
		sql.append(field);
		sql.append(" between ");
		sql.append(low);
		sql.append(" and ");
		sql.append(high);
		isAddAnd = true;

		return this;
	}

	@Override
	public Select notBetween(String field, Number low, Number high) {
		checkField(field);
		if (isAddAnd) sql.append(" and ");
		sql.append(field);
		sql.append(" not between ");
		sql.append(low);
		sql.append(" and ");
		sql.append(high);
		isAddAnd = true;

		return this;
	}

	@Override
	public Select isNull(String field) {
		checkField(field);
		if (isAddAnd) sql.append(" and ");
		sql.append(field + " is null ");
		return this;
	}

	@Override
	public Select isNotNull(String field) {
		checkField(field);
		if (isAddAnd) sql.append(" and ");
		sql.append(field + " is not null ");
		return this;
	}

	@Override
	public Select in(String field, Number... valueList) {
		checkField(field);
		return inOrNotIn(field, "in", valueList);
	}

	public Select notIn(String field, Number... valueList) {
		checkField(field);
		return inOrNotIn(field, "not in", valueList);
	}

	private Select inOrNotIn(String field, String op, Number... valueList) {
		checkField(field);
		if (isAddAnd) sql.append(" and ");
		String value = "";
		for (int i = 0; i < valueList.length; i++) {
			if (i == 0)
				value += valueList[i];
			else
				value += "," + valueList[i];
		}
		sql.append(field + " " + op + " (" + value + ")"); // eg: in (99,18)
		return this;
	}

	@Override
	public Select in(String field, String valueList) {
		checkField(field);
		return inOrNotIn(field, "in", valueList);
	}

	@Override
	public Select notIn(String field, String valueList) {
		checkField(field);
		return inOrNotIn(field, "not in", valueList);
	}

	private Select inOrNotIn(String field, String op, String valueList) {
		checkField(field);
		if (isAddAnd) sql.append(" and ");
		valueList = valueList.replace(",", "','");
		sql.append(field + " " + op + " ('" + valueList + "')"); // in ('client01','bee')
		return this;
	}

	@Override
	public Select groupBy(String field) {
		checkField(field);
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

	@Override
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

	@Override
	public Select orderBy(String field) {
		checkField(field);

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

	@Override
	public Select orderBy(String field, OrderType orderType) {
		checkField(field);
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

	@Override
	public Select exists(Select subSelect) {
		return exists(subSelect.toSQL(true));
	}

	public Select exists(String subSelect) {
		return useSubSelect("exists", subSelect);
	}

	@Override
	public Select notExists(Select subSelect) {
		return notExists(subSelect.toSQL(true));
	}

	@Override
	public Select notExists(String subSelect) {
		return useSubSelect("not exists", subSelect);
	}

	@Override
	public Select in(Select subSelect) {
		return in(subSelect.toSQL(true));
	}

	@Override
	public Select in(String subSelect) {
		return useSubSelect("in", subSelect);
	}

	@Override
	public Select notIn(Select subSelect) {
		return notIn(subSelect.toSQL(true));
	}

	@Override
	public Select notIn(String subSelect) {
		return useSubSelect("not in", subSelect);
	}

	@Override
	public Select start(int start) {
		this.start=start;
		return this;
	}

	@Override
	public Select size(int size) {
		this.size=size;
		return this;
	}
	
	@Override
	public Select lParentheses() {
		if (isAddAnd) sql.append(" and ");
		isAddAnd = false;
		sql.append(L_PARENTHESES);
		return this;
	}

	@Override
	public Select rParentheses() {
		sql.append(R_PARENTHESES);
		return this;
	}
	
	
 //Condition<<=============
	@Override
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
			isAddAnd = true; //fix on 2020-01-13
		} else {
			if (isAddAnd) sql.append(" and ");
			sql.append(expression);
			isAddAnd = true;
		}

		return this;
	}

	@Override
	public Select op(String field, Op opType, String value) {
		checkField(field);
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

	@Override
	public Select op(String field, Op opType, Number value) {
		checkField(field);
		if (opType == Op.in) return in(field, value);
		if (opType == Op.notIn) return notIn(field, value);

		if (isAddAnd) sql.append(" and ");
		sql.append(field);
		sql.append(opType.getOperator());
		sql.append(value);
		isAddAnd = true;
		return this;
	}
	
	@Override
	public Select op(String field, String value) {
		checkField(field);
		return op(field, Op.eq, value);
	}
	
	@Override
	public Select op(String field, Number value) {
		checkField(field);
		return op(field, Op.eq, value);
	}

	/**
	 * 默认自动加and.default will automatically add and.
	 * 
	 * @return a reference to this object.
	 */
	@Override
	public Select and() {
		sql.append(" and ");
		isAddAnd = false;
		return this;
	}

	@Override
	public Select or() {
		sql.append(" or ");
		isAddAnd = false;
		return this;
	}
	 //=============>>
	
	private void checkField(String field){
		if(CheckField.isNotValid(field)) {
			throw new BeeErrorFieldException("The field: '"+field+ "' is invalid!");
		}
	}
}
