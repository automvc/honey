/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.mongodb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.teasoft.bee.osql.Op;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.exception.BeeErrorGrammarException;
import org.teasoft.bee.osql.exception.BeeIllegalSQLException;
import org.teasoft.honey.osql.core.ConditionImpl;
import org.teasoft.honey.osql.core.Expression;
import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.osql.core.NameTranslateHandle;
import org.teasoft.honey.osql.core.OpType;
import org.teasoft.honey.util.StringUtils;

/**
 * @author Jade
 * @since  2.0
 */
public class MongoConditionHelper {
	private static final String OR = "or";
	private static final String AND = "and";
	
	private MongoConditionHelper() {}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map<String, Object> processCondition(Condition condition) throws Exception {

		if (condition == null) return null;

//		condition中op,between,notBetween方法设置的字段,不受includeType的值影响

		ConditionImpl conditionImpl = (ConditionImpl) condition;
		List<Expression> expList = conditionImpl.getExpList();
		Expression expression = null;

		Integer start = conditionImpl.getStart();

		if (start != null && SuidType.SELECT != conditionImpl.getSuidType()) {
			throw new BeeErrorGrammarException(conditionImpl.getSuidType() + " do not support paging with start !");
		}

		int len = expList.size();
		for (int k = 0; k < len - 2; k++) { // 将between,like,in,op两则的()删除
			if (expList.get(k).getOpType() == OpType.L_PARENTHESES
					&& expList.get(k + 2).getOpType() == OpType.R_PARENTHESES) {
				OpType opType = expList.get(k + 1).getOpType();
				if (OpType.BETWEEN == opType || OpType.NOT_BETWEEN == opType || OpType.LIKE == opType
						|| OpType.IN == opType || OpType.OP2 == opType) {
					expList.remove(k + 2);
					expList.remove(k);
					len = expList.size();
				}
			}
		}

		String columnName = "";
		boolean isNeedAnd = false;
		Stack stack = new Stack<>();
		for (int j = 0; j < expList.size(); j++) {
			expression = expList.get(j);
			OpType opType = expression.getOpType();
			Op op = expression.getOp();

			if (opType==OpType.L_PARENTHESES) { // (
				stack.push("(");
				continue;
			} else if (opType==OpType.R_PARENTHESES) {// )
				stack.push(")");
				isNeedAnd = true;
				continue;

			} else if (opType == OpType.ONE) { // or, and, not (2.1.10)
				stack.push(expression.getValue().toString().toLowerCase());
				isNeedAnd = false;
				continue;
			}

			if(opType == OpType.GROUP_BY || opType == OpType.HAVING){
				if (SuidType.SELECT != conditionImpl.getSuidType()) {
					throw new BeeErrorGrammarException(
							conditionImpl.getSuidType() + " do not support the opType: " + opType + "!");
				}
			}

			if(opType == OpType.GROUP_BY) {
//				在 selectWithGroupBy 处理
				continue;
			}

			if (OpType.ORDER_BY2 == opType || OpType.ORDER_BY3 == opType || OpType.ORDER_BY4 == opType) {
				continue;
			} // end orderBy

			columnName = _toColumnName(expression.getFieldName(), null);
			if ("id".equalsIgnoreCase(columnName)) {// 替换id为_id
				columnName = "_id";
			}

			if (isNeedAnd) { // 自动加 and
				stack.push(AND);
			}

			if (opType==OpType.IN) {
				Object v = expression.getValue();
				if (v == null) continue;
				Object listOrSet = processIn(v);

				if (Op.in == op) {
//					documentAsMap.put(columnName, EasyMapUtil.createMap("$in", listOrSet));
					stack.push(EasyMapUtil.createMap(columnName, EasyMapUtil.createMap("$in", listOrSet)));
				} else if (Op.notIn == op) {
					stack.push(EasyMapUtil.createMap(columnName, EasyMapUtil.createMap("$nin", listOrSet)));
				}

				isNeedAnd = true;
				continue;
			} else if (opType==OpType.LIKE) {

					String v = (String) expression.getValue(); // mongodb 有这种用法吗
					String v2 = "^$"; // 只匹配空字符
					if (v != null && !"".equals(v)) {
						v = StringUtils.escapeMatch(v);

						if (Op.likeLeft == op) {
							v2 = v + "$";
						} else if (Op.likeRight == op) {
							v2 = "^" + v;
						} else if (Op.likeLeftRight == op) {
							// 不加 ^ $ 则左右匹配
							v2 = v;
						} else { // Op.like
//						if (StringUtils.justLikeChar(v)) {
//							throw new BeeIllegalSQLException("Like has SQL injection risk! "
//									+ columnName + " like '" + v + "'");
//						}
							v2 = v;
						}
					} else {
						if (v == null) Logger.warn("the parameter value in like is null !", new BeeIllegalSQLException());
					}

					stack.push(EasyMapUtil.createMap(columnName, EasyMapUtil.createMap("$regex", v2)));
//				Map<String,Object> map=new LinkedHashMap<>(); 
//				documentAsMap.put(columnName, map.put("$regex", v2));  //不行

					isNeedAnd = true;
					continue;
				} else if (OpType.BETWEEN == opType || OpType.NOT_BETWEEN == opType) {

					if (OpType.BETWEEN == opType) { // eg: price between 1 and 2 -> (price >=1 and price<=2)
						stack.push("(");
						stack.push(EasyMapUtil.createMap(columnName, EasyMapUtil.createMap("$gte", expression.getValue())));
						stack.push(AND);
						stack.push(EasyMapUtil.createMap(columnName, EasyMapUtil.createMap("$lte", expression.getValue2())));
						stack.push(")");

					} else if (OpType.NOT_BETWEEN == opType) { // price not between 1 and 2 -> (price <1 or price>2)
						stack.push("(");
						stack.push(EasyMapUtil.createMap(columnName, EasyMapUtil.createMap("$lt", expression.getValue())));
						stack.push(OR);
						stack.push(EasyMapUtil.createMap(columnName, EasyMapUtil.createMap("$gt", expression.getValue2())));
						stack.push(")");
					}

					isNeedAnd = true;
					continue;
				}

//			if (expression.getValue() == null) { // column is null
//				stack.push(EasyMapUtil.createMap(columnName, null));
//				isNeedAnd = true;
//			} else {
				boolean find = true;
				String type = expression.getOp().getOperator();
				switch (type) {
					case "=":
//					list.add(Filters.eq(columnName, expression.getValue()));
						stack.push(EasyMapUtil.createMap(columnName, expression.getValue()));
						break;

					case "!=":
//					list.add(Filters.ne(columnName, expression.getValue()));
						stack.push(EasyMapUtil.createMap(columnName, EasyMapUtil.createMap("$ne", expression.getValue())));
						break;

					case ">":
						stack.push(EasyMapUtil.createMap(columnName, EasyMapUtil.createMap("$gt", expression.getValue())));
						break;

					case ">=":
						stack.push(EasyMapUtil.createMap(columnName, EasyMapUtil.createMap("$gte", expression.getValue())));
						break;

					case "<":
						stack.push(EasyMapUtil.createMap(columnName, EasyMapUtil.createMap("$lt", expression.getValue())));
						break;

					case "<=":
						stack.push(EasyMapUtil.createMap(columnName, EasyMapUtil.createMap("$lte", expression.getValue())));
						break;

					default:
						find = false;
				}
				if (find) isNeedAnd = true;
//			}

		} // end expList for

		if (stack.size() == 0) return null;

		return ParseExpMap.parse(stack);
	}

	private static Object processIn(Object v) {

		if (List.class.isAssignableFrom(v.getClass()) || Set.class.isAssignableFrom(v.getClass())) { // List,Set
			return v;
		}

		List<Object> inList = new ArrayList<>();
		if (HoneyUtil.isNumberArray(v.getClass())) { // Number Array
			Number n[] = (Number[]) v;
			for (Number number : n) {
				inList.add(number);
			}
//		} else if (String.class.equals(v.getClass())) { // String 逗号(,)为分隔符
		} else if (v instanceof String) { // String 逗号(,)为分隔符
			Object values[] = v.toString().trim().split(",");
			for (Object e : values) {
				inList.add(e);
			}
		} else { // other one elements
			inList.add(v);
		}
		return inList;
	}

	@SuppressWarnings("rawtypes")
	private static String _toColumnName(String fieldName, Class entityClass) {
		return NameTranslateHandle.toColumnName(fieldName, entityClass);
	}

}
