/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.teasoft.bee.osql.Condition;
import org.teasoft.bee.osql.Op;
import org.teasoft.bee.osql.OrderType;
import org.teasoft.bee.osql.SuidType;

/**
 * @author Kingstar
 * @since  1.6
 */
public class ConditionImpl implements Condition {

	private SuidType suidType;
	public List<Expression> list = new ArrayList<>();
	private Set<String> fieldSet = new HashSet<>();

	private boolean isStartGroupBy = true;
	private boolean isStartHaving = true;
	private boolean isStartOrderBy = true;

	private static String COMMA = ",";

	private Integer start;
	private Integer size;

	@Override
	public Condition start(Integer start) {
		this.start = start;
		return this;
	}

	@Override
	public Condition size(Integer size) {
		this.size = size;
		return this;
	}

	@Override
	public Condition op(String field, Op Op, Object value) {
		list.add(new Expression(field, Op, value));
		this.fieldSet.add(field);
		return this;
	}

	@Override
	public Set getFieldSet() {
		return fieldSet;
	}

	@Override
	public Condition and() {
		Expression exp = new Expression();
		exp.setOpNum(1);
		exp.value = "and";
		list.add(exp);

		return this;
	}

	@Override
	public Condition or() {
		Expression exp = new Expression();
		exp.setOpNum(1);
		exp.value = "or";
		list.add(exp);

		return this;
	}

	@Override
	public Condition lParentheses() {
		Expression exp = new Expression();
		exp.setOpNum(-1);
		exp.value = "(";
		list.add(exp);

		return this;
	}

	@Override
	public Condition rParentheses() {
		Expression exp = new Expression();
		exp.setOpNum(0);
		exp.value = ")";
		list.add(exp);

		return this;
	}

	@Override
	public Condition groupBy(String field) {
		Expression exp = new Expression();
		if (isStartGroupBy) {
			//			sql.append(" group by ");
			//			sql.append(field);

			isStartGroupBy = false;

			exp.fieldName = field;
			exp.opType = "groupBy";
		} else {
			//			sql.append(COMMA);
			//			sql.append(field);
			//			exp.fieldName=","+field; //不能这样写,field需要转换
			exp.fieldName = field;
			exp.opType = "groupBy";
			exp.value = COMMA;
		}
		list.add(exp);
		return this;
	}

	@Override
	public Condition having(String expression) {
		if (isStartHaving) {
			//			sql.append(" having ");
			//			sql.append(expression);
			//TODO 是否受到字段转换的影响
			isStartHaving = false;
		} else {
			//			sql.append(" and ");
			//			sql.append(expression);
		}
		//		list.add(exp);
		return this;
	}

	@Override
	public Condition orderBy(String field) {

		Expression exp = new Expression();
		exp.opType = "orderBy";
		//		exp.value
		exp.fieldName = field;
		exp.opNum = 2;

		if (isStartOrderBy) {
			isStartOrderBy = false;
			exp.value = " order by ";
		} else {
			exp.value = COMMA;
		}
		list.add(exp);
		return this;
	}

	@Override
	public Condition orderBy(String field, OrderType orderType) {

		Expression exp = new Expression();
		exp.opType = "orderBy";
		//		exp.value
		exp.fieldName = field;
		exp.value2 = orderType.getName();
		exp.opNum = 3;

		if (isStartOrderBy) {
			isStartOrderBy = false;
			exp.value = " order by ";
		} else {
			exp.value = COMMA;
		}
		list.add(exp);
		return this;
	}

	@Override
	public void setSuidType(SuidType suidType) {
		this.suidType = suidType;
	}

	public SuidType getSuidType() {
		return suidType;
	}

	public List getExpList() {
		return list;
	}

	public Integer getStart() {
		return start;
	}

	public Integer getSize() {
		return size;
	}

}
