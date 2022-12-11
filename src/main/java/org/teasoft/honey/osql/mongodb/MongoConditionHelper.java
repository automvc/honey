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

import org.teasoft.bee.osql.Condition;
import org.teasoft.bee.osql.Op;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.exception.BeeErrorGrammarException;
import org.teasoft.bee.osql.exception.BeeIllegalSQLException;
import org.teasoft.honey.osql.core.ConditionImpl;
import org.teasoft.honey.osql.core.Expression;
import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.osql.core.NameTranslateHandle;
import org.teasoft.honey.util.StringUtils;

/**
 * @author Jade
 * @since  2.0
 */
public class MongoConditionHelper {
    private static final String OR = "or";

	private static final String AND = "and";
	private static final String GROUP_BY = "groupBy";
	private static final String HAVING = "having";

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map<String, Object> processCondition(Condition condition) throws Exception {

		if (condition == null) return null;
		
//		condition中op,between,notBetween方法设置的字段,不受includeType的值影响

		ConditionImpl conditionImpl = (ConditionImpl) condition;
		List<Expression> expList = conditionImpl.getExpList();
		Expression expression = null;

		Integer start = conditionImpl.getStart();

		if (start != null && SuidType.SELECT != conditionImpl.getSuidType()) {
			throw new BeeErrorGrammarException(
					conditionImpl.getSuidType() + " do not support paging with start !");
		}
		String columnName = "";
		boolean isNeedAnd = false;
		Stack stack=new Stack<>();
		for (int j = 0; j < expList.size(); j++) {
			expression = expList.get(j);
			
			if (expression.getOpNum() == -2) { // (
				stack.push("(");
				continue;
			}else if (expression.getOpNum() == -1) {// )
				stack.push(")");
				isNeedAnd = true;
				continue;

			} else if (expression.getOpNum() == 1) { // or || and operation
				stack.push(expression.getValue().toString().toLowerCase());
				isNeedAnd = false;
				continue;
			}
			
			String opType = expression.getOpType();

			if (GROUP_BY.equalsIgnoreCase(opType) || HAVING.equalsIgnoreCase(opType)) {
				if (SuidType.SELECT != conditionImpl.getSuidType()) {
					throw new BeeErrorGrammarException(conditionImpl.getSuidType()
							+ " do not support the opType: " + opType + "!");
				}
			} 
			
			if (GROUP_BY.equalsIgnoreCase(opType)) {
				Logger.debug("------------------process in selectWithGroupBy...");
//				在 selectWithGroupBy 处理
				continue;
			}
					

	        if ("orderBy".equalsIgnoreCase(opType)) {//TODO 


				continue;
			} // end orderBy
			
//			columnName=_toColumnName(expression.getFieldName(),useSubTableNames,entityClass);
			columnName = _toColumnName(expression.getFieldName(), null);

			if ("id".equalsIgnoreCase(columnName)) {// 替换id为_id
				columnName = "_id";
			}
			
			if(isNeedAnd) { //自动加 and
				stack.push(AND);
			}
			
			if (Op.in.getOperator().equalsIgnoreCase(opType) || Op.notIn.getOperator().equalsIgnoreCase(opType)) {

				Object v = expression.getValue();
				if (v == null) continue;

				Object listOrSet = processIn(v);

				if (Op.in.getOperator().equalsIgnoreCase(opType)) {
//					documentAsMap.put(columnName, EasyMapUtil.createMap("$in", listOrSet));
				    stack.push(EasyMapUtil.createMap(columnName, EasyMapUtil.createMap("$in", listOrSet)));
				}else if (Op.notIn.getOperator().equalsIgnoreCase(opType)) {
					stack.push(EasyMapUtil.createMap(columnName, EasyMapUtil.createMap("$nin", listOrSet)));
				}

				isNeedAnd=true;
				continue;
			} else if (Op.like.getOperator().equalsIgnoreCase(opType) || Op.notLike.getOperator().equalsIgnoreCase(opType)) {

				String v = (String) expression.getValue(); // mongodb 有这种用法吗
				String v2 = "^$"; // 只匹配空字符
				if (v != null && !"".equals(v)) {
					Op op = expression.getOp();
					
					v = StringUtils.escapeMatch(v);
					
					if (Op.likeLeft == op) {
						v2 = v + "$";
					} else if (Op.likeRight == op) {
						v2 = "^" + v;
					} else if (Op.likeLeftRight == op) {
						// 不加 ^ $ 则左右匹配
						v2=v;
					} else { // Op.like
//						if (StringUtils.justLikeChar(v)) {
//							throw new BeeIllegalSQLException("Like has SQL injection risk! "
//									+ columnName + " like '" + v + "'");
//						}
						v2=v;
					}
				} else {
				  if (v == null)
					Logger.warn("the parameter value in like is null !",
							new BeeIllegalSQLException());
				}

				stack.push(EasyMapUtil.createMap(columnName, EasyMapUtil.createMap("$regex", v2)));
//				Map<String,Object> map=new LinkedHashMap<>(); 
//				documentAsMap.put(columnName, map.put("$regex", v2));  //不行

				isNeedAnd=true;
				continue;
			} else if (" between ".equalsIgnoreCase(opType) || " not between ".equalsIgnoreCase(opType)) {

				if (" between ".equalsIgnoreCase(opType)) {  //eg: price between 1 and 2 ->  (price >=1 and price<=2)
					stack.push("(");
					stack.push(EasyMapUtil.createMap(columnName, EasyMapUtil.createMap("$gte", expression.getValue())));
					stack.push(AND);
					stack.push(EasyMapUtil.createMap(columnName, EasyMapUtil.createMap("$lte", expression.getValue2())));
					stack.push(")");
					
				} else if (" not between ".equalsIgnoreCase(opType)) { //price not between 1 and 2 ->  (price <1 and price>2)
					stack.push("(");
					stack.push(EasyMapUtil.createMap(columnName, EasyMapUtil.createMap("$lt", expression.getValue())));
					stack.push(OR);
					stack.push(EasyMapUtil.createMap(columnName, EasyMapUtil.createMap("$gt", expression.getValue2())));
					stack.push(")");
				}

				isNeedAnd = true;
				continue;
			}


			if (expression.getValue() == null) { // column is null
				stack.push(EasyMapUtil.createMap(columnName,null));
			} else {

				String type = expression.getOpType();
				switch (type) {
					case "=":
//					list.add(Filters.eq(columnName, expression.getValue()));
						stack.push(EasyMapUtil.createMap(columnName, expression.getValue()));
						break;

					case "!=":
//					list.add(Filters.ne(columnName, expression.getValue()));
						stack.push(EasyMapUtil.createMap(columnName,EasyMapUtil.createMap("$ne", expression.getValue())));
						break;

					case ">":
						stack.push(EasyMapUtil.createMap(columnName,EasyMapUtil.createMap("$gt", expression.getValue())));
						break;

					case ">=":
						stack.push(EasyMapUtil.createMap(columnName,EasyMapUtil.createMap("$gte", expression.getValue())));
						break;

					case "<":
						stack.push(EasyMapUtil.createMap(columnName,EasyMapUtil.createMap("$lt", expression.getValue())));
						break;

					case "<=":
						stack.push(EasyMapUtil.createMap(columnName,EasyMapUtil.createMap("$lte", expression.getValue())));
						break;
				}

			}
			
			isNeedAnd=true;

		} // end expList for

		if (stack.size() == 0) return null;

		return ParseExpMap.parse(stack);
	}

	private static Object processIn(Object v) {
		List<Object> inList = new ArrayList<>();
		if (List.class.isAssignableFrom(v.getClass())
				|| Set.class.isAssignableFrom(v.getClass())) { // List,Set
			return v;

		} else if (HoneyUtil.isNumberArray(v.getClass())) { // Number Array
			Number n[] = (Number[]) v;
			for (Number number : n) {
				inList.add(number);
			}
		} else if (String.class.equals(v.getClass())) { // String 逗号(,)为分隔符
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
