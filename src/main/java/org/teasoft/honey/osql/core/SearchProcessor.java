/*
 * Copyright 2013-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import org.teasoft.bee.osql.FunctionType;
import org.teasoft.bee.osql.Op;
import org.teasoft.bee.osql.OrderType;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.exception.BeeErrorGrammarException;
import org.teasoft.bee.osql.search.Operator;
import org.teasoft.bee.osql.search.Search;
import org.teasoft.honey.util.StringUtils;

/**
 * 复杂查询的Search结构处理器.Search Processor.
 * @author Kingstar
 * @since  1.9.8
 */
public class SearchProcessor {

	private SearchProcessor() {}

	public static Condition parseSearch(Search search[]) {

		Condition condition = BeeFactoryHelper.getCondition();

		for (int i = 0; i < search.length; i++) {

			String field = search[i].getField();
			Operator operator = search[i].getOp();
			String value = search[i].getValue1();
			String value2 = search[i].getValue2();
			String op2 = search[i].getOp2();
			op2 = trim(op2);

//			or("or"),
//			and("and")	//default
			if ("or".equalsIgnoreCase(op2)) condition.or();
			else if ("(".equals(op2)) condition.lParentheses();
			else if (")".equals(op2)) condition.rParentheses();
			else if ("or (".equalsIgnoreCase(op2)) condition.or().lParentheses();
			else if (") or".equalsIgnoreCase(op2)) condition.rParentheses().or();
			else if ("and (".equalsIgnoreCase(op2)) condition.and().lParentheses();
			else if (") and".equalsIgnoreCase(op2)) condition.rParentheses().and();

			if (operator == null) continue;

			switch (operator) {
				case like:
					if ("Left".equalsIgnoreCase(value2)) condition.op(field, Op.likeLeft, value);
					else if ("Right".equalsIgnoreCase(value2)) condition.op(field, Op.likeRight, value);
					else if ("LeftRight".equalsIgnoreCase(value2)) condition.op(field, Op.likeLeftRight, value);
					else {
						condition.op(field, Op.like, value);
					}
					break;
				case notLike:
					value = adjustValueForLike(value, value2);
					condition.op(field, Op.notLike, value);
					break;

				case between:
					checkForBetween(value, value2);
					setBetweenValue(field, value, value2, condition);
					break;
				case notBetween:
					checkForBetween(value, value2);
//					condition.notBetween(field, value, value2);
					setNotBetweenValue(field, value, value2, condition);
					break;

				case in:
					condition.op(field, Op.in, value);
					break;
				case notIn:
					condition.op(field, Op.notIn, value);
					break;

				// 简单sql,函数只能select用??
				case max:
					if (StringUtils.isNotBlank(value)) { // value 为别名 alias
						condition.selectFun(FunctionType.MAX, field, value);
					} else {
						condition.selectFun(FunctionType.MAX, field);
					}
					break;
				case min:
					if (StringUtils.isNotBlank(value)) { // value 为别名 alias
						condition.selectFun(FunctionType.MIN, field, value);
					} else {
						condition.selectFun(FunctionType.MIN, field);
					}
					break;
				case sum:
					if (StringUtils.isNotBlank(value)) { // value 为别名 alias
						condition.selectFun(FunctionType.SUM, field, value);
					} else {
						condition.selectFun(FunctionType.SUM, field);
					}
					break;
				case avg:
					if (StringUtils.isNotBlank(value)) { // value 为别名 alias
						condition.selectFun(FunctionType.AVG, field, value);
					} else {
						condition.selectFun(FunctionType.AVG, field);
					}
					break;
				case count:
					if (StringUtils.isNotBlank(value)) { // value 为别名 alias
						condition.selectFun(FunctionType.COUNT, field, value);
					} else {
						condition.selectFun(FunctionType.COUNT, field);
					}
					break;
				case distinct:
					if (StringUtils.isNotBlank(value)) { // value 为别名 alias
						condition.selectDistinctField(field, value);
					} else {
						condition.selectDistinctField(field);
					}
					break;

//			    select特有		
//				groupBy("groupBy"),
//				having("having"),
//				orderBy("orderBy"),	
				case groupBy:
					condition.groupBy(field);
					break;
//				case having:   
//					condition.having(functionType, field, Op, value);  //要判断Op
//					break;	
				case orderBy: // 一次只能设置一个字段
					if (StringUtils.isNotBlank(value)) { // orderType
						if ("asc".equalsIgnoreCase(value)) condition.orderBy(field, OrderType.ASC);
						else if ("desc".equalsIgnoreCase(value)) condition.orderBy(field, OrderType.DESC);
						else throw new BeeErrorGrammarException("OrderType just support ASC or DESC !");
					} else {
						condition.orderBy(field);
					}
					break;

				case eq:
					condition.op(field, Op.eq, value);
					break;
				case nq:
					condition.op(field, Op.nq, value);
					break;
				case ge:
					condition.op(field, Op.ge, value);
					break;
				case le:
					condition.op(field, Op.le, value);
					break;
				case gt:
					condition.op(field, Op.gt, value);
					break;
				case lt:
					condition.op(field, Op.lt, value);
					break;

				default:
					break;

			}
		}

		return condition;
	}

	private static String adjustValueForLike(String value, String value2) {
		if (StringUtils.isNotBlank(value2)) {
			if ("Left".equalsIgnoreCase(value2)) value = "%" + value;
			else if ("Right".equalsIgnoreCase(value2)) value = value + "%";
			else if ("LeftRight".equalsIgnoreCase(value2)) value = "%" + value + "%";
		}

		return value;
	}

	private static void checkForBetween(String value, String value2) {
		if (StringUtils.isBlank(value)) {
			throw new BeeErrorGrammarException("the min value of Between invalid!");
		}
		if (StringUtils.isBlank(value2)) {
			throw new BeeErrorGrammarException("the max value of Between invalid!");
		}
	}

	private static String trim(String str) {
		if (str != null) str = str.trim();
		return str;
	}

	private static void setBetweenValue(String field, String value, String value2, Condition condition) {
		try {
			if (StringUtils.isInteger(value) && StringUtils.isInteger(value2)) {
				condition.between(field, Long.parseLong(value), Long.parseLong(value2));
			} else if (StringUtils.isNumber(value) && StringUtils.isNumber(value2)) {
				condition.between(field, Double.parseDouble(value), Double.parseDouble(value2));
			} else {
				condition.between(field, value, value2);
			}
		} catch (Exception e) {
			condition.between(field, value, value2);
		}
	}

	private static void setNotBetweenValue(String field, String value, String value2, Condition condition) {
		try {
			if (StringUtils.isInteger(value) && StringUtils.isInteger(value2)) {
				condition.notBetween(field, Long.parseLong(value), Long.parseLong(value2));
			} else if (StringUtils.isNumber(value) && StringUtils.isNumber(value2)) {
				condition.notBetween(field, Double.parseDouble(value), Double.parseDouble(value2));
			} else {
				condition.notBetween(field, value, value2);
			}
		} catch (Exception e) {
			condition.notBetween(field, value, value2);
		}
	}

}
