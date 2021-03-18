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
import org.teasoft.honey.osql.core.Check;
import org.teasoft.honey.osql.core.CheckField;
import org.teasoft.honey.osql.core.FunAndOrderTypeMap;
import org.teasoft.honey.osql.core.K;

/**
 * @author Kingstar
 * @since  1.3
 */
public class SelectImpl extends AbstractSelectToSql implements Select {

	private static final String STAR = "*";
//	private static final String DISTINCT = "distinct";
	private static final String DISTINCT = K.distinct;
	
	//for where condition
	private static final String L_PARENTHESES = "(";
	private static final String R_PARENTHESES = ")";
	private static final String COMMA = ",";
	private static final String SPACE = " ";
	private static final String AND = " "+K.and+" ";
	

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
	
	@Override
	public Select select() {
		if (isStartField) {
//			sql.append("select ");
			sql.append(K.select).append(SPACE);
			sql.append(STAR); //*
			isStartField = false;
		}
		return this;
	}

	@Override
	public Select select(String column) {
		checkField(column);
		if (isStartField) {
			sql.append(K.select).append(SPACE);
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
		checkExpression(table);
//		sql.append(" from ");
		sql.append(SPACE).append(K.from).append(SPACE);
		sql.append(table);
		return this;
	}

	@Override
	public Select join(String anotherTable) {
		checkExpression(anotherTable);
//		sql.append(" join ");
		sql.append(SPACE).append(K.join).append(SPACE);
		sql.append(anotherTable);
		return this;
	}

	@Override
	public Select innerJoin(String anotherTable) {
		checkExpression(anotherTable);
//		sql.append(" inner join ");
		sql.append(SPACE).append(K.innerJoin).append(SPACE);
		sql.append(anotherTable);
		return this;
	}

	@Override
	public Select leftJoin(String anotherTable) {
		checkExpression(anotherTable);
//		sql.append(" left join ");
		sql.append(SPACE).append(K.leftJoin).append(SPACE);
		sql.append(anotherTable);
		return this;
	}

	@Override
	public Select rightJoin(String anotherTable) {
		checkExpression(anotherTable);
//		sql.append(" right join ");
		sql.append(SPACE).append(K.rightJoin).append(SPACE);
		sql.append(anotherTable);
		return this;
	}

	@Override
	public Select on() {
//		sql.append(" on ");
		sql.append(SPACE).append(K.on).append(SPACE);
		isStartOn = false;

		return this;
	}

	@Override
	public Select on(String expression) {
		checkExpression(expression);
		if (isStartOn) {
//			sql.append(" on ");
			sql.append(SPACE).append(K.on).append(SPACE);
			sql.append(expression);
			isStartOn = false;
		} else {
			if (isAddAnd) sql.append(AND);
			sql.append(expression);
			isAddAnd = true;
		}

		return this;
	}



	@Override
	public Select groupBy(String field) {
		checkField(field);
		if (isStartGroupBy) {
//			sql.append(" group by ");
			sql.append(SPACE).append(K.groupBy).append(SPACE);
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
		checkExpression(expression);
		if (isStartHaving) {
//			sql.append(" having ");
			sql.append(SPACE).append(K.having).append(SPACE);
			sql.append(expression);
			isStartHaving = false;
		} else {
			sql.append(AND);
			sql.append(expression);
		}

		return this;
	}

	@Override
	public Select orderBy(String field) {
		checkField(field);
		if (isStartOrderBy) {
//			sql.append(" order by ");
			sql.append(SPACE).append(K.orderBy).append(SPACE);
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
//			sql.append(" order by ");
			sql.append(SPACE).append(K.orderBy).append(SPACE);
			sql.append(field);
			sql.append(SPACE);
			sql.append(FunAndOrderTypeMap.transfer(orderType.getName()));
			isStartOrderBy = false;
		} else {
			sql.append(COMMA);
			sql.append(field);
			sql.append(SPACE);
			sql.append(FunAndOrderTypeMap.transfer(orderType.getName()));
		}
		return this;
	}
	
	private Select useSubSelect(String keyword, String subSelect) { // exists, not exists

		sql.append(keyword);
		sql.append(SPACE);
		sql.append(L_PARENTHESES);
		sql.append(subSelect);
		sql.append(R_PARENTHESES);
		return this;
	}
	
	private Select useSubSelect(String field,String keyword, String subSelect) { // in, not in
		checkField(field);
		
		sql.append(field);
		sql.append(SPACE);
		sql.append(keyword);
		sql.append(SPACE);
		sql.append(L_PARENTHESES);
		sql.append(subSelect);
		sql.append(R_PARENTHESES);
		return this;
	}

	@Override
	public Select exists(Select subSelect) {
		return useSubSelect(K.exists, subSelect.toSQL());
	}

//	public Select exists(String subSelect) {
//		return useSubSelect("exists", subSelect);
//	}

	@Override
	public Select notExists(Select subSelect) {
		return useSubSelect(K.notExists, subSelect.toSQL());
	}

//	@Override
//	public Select notExists(String subSelect) {
//		return useSubSelect("not exists", subSelect);
//	}

	@Override
	public Select in(String field, Select subSelect) {
		return useSubSelect(field, K.in, subSelect.toSQL());
	}

	@Override
	public Select notIn(String field, Select subSelect) {
		return useSubSelect(field, K.notIn, subSelect.toSQL());
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
	
//select , update also need use	
 //Condition<<============= 
	@Override
	public Select lParentheses() {
		if (isAddAnd) sql.append(AND);
		isAddAnd = false;
		sql.append(L_PARENTHESES);
		return this;
	}

	@Override
	public Select rParentheses() {
		sql.append(R_PARENTHESES);
		isAddAnd = true;
		return this;
	}
	
	@Override
	public Select where() {
//		sql.append(" where ");
		sql.append(SPACE).append(K.where).append(SPACE);
		isStartWhere = false;

		return this;
	}

	@Override
	public Select where(String expression) {
		checkExpression(expression);
		if (isStartWhere) {
//			sql.append(" where ");
			sql.append(SPACE).append(K.where).append(SPACE);
			sql.append(expression);
			isStartWhere = false;
			isAddAnd = true; //fix on 2020-01-13
		} else {
			if (isAddAnd) sql.append(AND);
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

		if (isAddAnd) sql.append(AND);

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

		if (isAddAnd) sql.append(AND);
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
		sql.append(AND);
		isAddAnd = false;
		return this;
	}

	@Override
	public Select or() {
//		sql.append(" or ");
		sql.append(SPACE).append(K.or).append(SPACE);
		isAddAnd = false;
		return this;
	}
	
	@Override
	public Select in(String field, Number... valueList) {
		checkField(field);
		return inOrNotIn(field, K.in, valueList);
	}

	public Select notIn(String field, Number... valueList) {
		checkField(field);
		return inOrNotIn(field, K.notIn, valueList);
	}

	private Select inOrNotIn(String field, String op, Number... valueList) {
		checkField(field);
		if (isAddAnd) sql.append(AND);
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
		return inOrNotIn(field, K.in, valueList);
	}

	@Override
	public Select notIn(String field, String valueList) {
		return inOrNotIn(field, K.notIn, valueList);
	}

	private Select inOrNotIn(String field, String op, String valueList) {
		checkField(field);
		if (isAddAnd) sql.append(AND);
		valueList = valueList.replace(",", "','");
		sql.append(field + " " + op + " ('" + valueList + "')"); // in ('client01','bee')
		return this;
	}
	
	@Override
	public Select between(String field, Number low, Number high) {
		checkField(field);
		if (isAddAnd) sql.append(AND);
		sql.append(field);
//		sql.append(" between ");
		sql.append(SPACE).append(K.between).append(SPACE);
		sql.append(low);
		sql.append(AND);
		sql.append(high);
		isAddAnd = true;

		return this;
	}

	@Override
	public Select notBetween(String field, Number low, Number high) {
		checkField(field);
		if (isAddAnd) sql.append(AND);
		sql.append(field);
//		sql.append(" not between ");
		sql.append(SPACE).append(K.notBetween).append(SPACE);
		sql.append(low);
		sql.append(AND);
		sql.append(high);
		isAddAnd = true;

		return this;
	}

	@Override
	public Select isNull(String field) {
		checkField(field);
		if (isAddAnd) sql.append(AND);
//		sql.append(field + " is null ");
		sql.append(field);
		sql.append(SPACE).append(K.isNull).append(SPACE);
		return this;
	}

	@Override
	public Select isNotNull(String field) {
		checkField(field);
		if (isAddAnd) sql.append(AND);
//		sql.append(field + " is not null ");
		sql.append(field);
		sql.append(SPACE).append(K.isNotNull).append(SPACE);
		return this;
	}
	
	private void checkField(String field){
		if(CheckField.isNotValid(field)) {
			throw new BeeErrorFieldException("The field: '"+field+ "' is invalid!");
		}
	}
	
	private void checkExpression(String expression){
		if(Check.isNotValidExpression(expression)) {
			throw new BeeErrorFieldException("The expression: '"+expression+ "' is invalid!");
		}
	}
	
	 //=============>>

}
