/*
 * Copyright 2013-2024 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.chain;

import java.util.List;

import org.teasoft.bee.osql.OrderType;
import org.teasoft.bee.osql.chain.Select;
import org.teasoft.bee.osql.exception.BeeIllegalParameterException;
import org.teasoft.honey.osql.core.FunAndOrderTypeMap;
import org.teasoft.honey.osql.core.K;
import org.teasoft.honey.osql.core.StringConst;

/**
 * @author Kingstar
 * @since  1.3
 * @since  2.4.0
 */
public class SelectImpl extends AbstractSelectToSql implements Select {

	private static final String STAR = "*";
	private static final String DISTINCT = K.distinct;

	// for where condition
	private static final String L_PARENTHESES = "(";
	private static final String R_PARENTHESES = ")";
	private static final String COMMA = ",";
	private static final String SPACE = " ";
	private static final String AND = " " + K.and + " ";

	private boolean isStartField = true;
//	private boolean isStartWhere = true;
	private boolean isStartOn = true;

//	private boolean isAddAnd = false;
	private boolean isStartGroupBy = true;
	private boolean isStartHaving = true;
	private boolean isStartOrderBy = true;

	public SelectImpl() {
// some one maybe just need where part
//		sql.append("select ");
	}

	@Override
	public Select select() {
		if (isStartField) {
			sql.append(K.select).append(SPACE);
			sql.append(STAR); // *
			isStartField = false;
		}
		return this;
	}

	@Override
	public Select select(String column) {
//		checkField(column);
		checkExpression(column);
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
	public Select distinct(String fieldName, String alias) {
		return select(K.distinct + L_PARENTHESES + fieldName + R_PARENTHESES + K.space + K.as + alias);
	}

	@Override
	public Select distinct(String field) {
		checkFieldOrExpression(field);
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
		_appendTable(table);

		if (isStartField) { // 2.4.0 //select *
			sql.append(K.select).append(SPACE);
			sql.append(STAR); // *
			isStartField = false;
		}

		sql.append(SPACE).append(K.from).append(SPACE);
		sql.append(table);
		return this;
	}

	@Override
	public Select join(String anotherTable) {
		checkExpression(anotherTable);
		_appendTable(anotherTable);
		sql.append(SPACE).append(K.join).append(SPACE);
		sql.append(anotherTable);
		return this;
	}

	@Override
	public Select innerJoin(String anotherTable) {
		checkExpression(anotherTable);
		_appendTable(anotherTable);
//		sql.append(" inner join ");
		sql.append(SPACE).append(K.innerJoin).append(SPACE);
		sql.append(anotherTable);
		return this;
	}

	@Override
	public Select leftJoin(String anotherTable) {
		checkExpression(anotherTable);
		_appendTable(anotherTable);
//		sql.append(" left join ");
		sql.append(SPACE).append(K.leftJoin).append(SPACE);
		sql.append(anotherTable);
		return this;
	}

	@Override
	public Select rightJoin(String anotherTable) {
		checkExpression(anotherTable);
		_appendTable(anotherTable);
//		sql.append(" right join ");
		sql.append(SPACE).append(K.rightJoin).append(SPACE);
		sql.append(anotherTable);
		return this;
	}

	@Override
	public Select on() {
		sql.append(SPACE).append(K.on).append(SPACE);
		isStartOn = false;

		return this;
	}

	@Override
	public Select on(String expression) {
		checkExpression(expression);
		if (isStartOn) {
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
//		checkField(field);
		checkFieldOrExpression(field);
		if (isStartGroupBy) {
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
//		checkField(field);
		checkFieldOrExpression(field);
		if (isStartOrderBy) {
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
//		checkField(field);
		checkFieldOrExpression(field);
		if (isStartOrderBy) {
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

	private Select useSubSelect(String field, String keyword, String subSelect) { // in, not in
//		checkField(field);
		checkFieldOrExpression(field);

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
		updatePvList(subSelect);
		return useSubSelect(K.exists, subSelect.toSQL());
	}

	@Override
	public Select notExists(Select subSelect) {
		updatePvList(subSelect);
		return useSubSelect(K.notExists, subSelect.toSQL());
	}

	@Override
	public Select in(String field, Select subSelect) {
		updatePvList(subSelect);
		return useSubSelect(field, K.in, subSelect.toSQL());
	}

	@Override
	public Select notIn(String field, Select subSelect) {
		updatePvList(subSelect);
		return useSubSelect(field, K.notIn, subSelect.toSQL()); // subSelect.toSQL() 不需要加缓存 ？？？
	}

	private static final String START_GREAT_EQ_0 = StringConst.START_GREAT_EQ_0;
	private static final String SIZE_GREAT_0 = StringConst.SIZE_GREAT_0;

	@Override
	public Select start(int start) {
		if (start < 0) throw new BeeIllegalParameterException(START_GREAT_EQ_0);
		this.start = start;
		return this;
	}

	@Override
	public Select size(int size) {
		if (size <= 0) throw new BeeIllegalParameterException(SIZE_GREAT_0);
		this.size = size;
		return this;
	}

	@Override
	public Select forUpdate() {
		sql.append(SPACE).append(K.forUpdate);
		return this;
	}

	private void updatePvList(Select subSelect) {
		super.getPvList().addAll((List) subSelect.getPvList());
	}

	private void _appendTable(String table) {
		super.appendTable(table);
	}

}
