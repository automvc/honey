/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.List;

import org.teasoft.bee.osql.Condition;
import org.teasoft.bee.osql.Op;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.osql.exception.BeeErrorGrammarException;

/**
 * @author Kingstar
 * @since  1.6
 */
public class ConditionHelper {
	static boolean isNeedAnd = true;
	private static String ONE_SPACE = " ";

	private static DbFeature dbFeature = BeeFactory.getHoneyFactory().getDbFeature();

	static void processCondition(StringBuffer sqlBuffer, StringBuffer valueBuffer, List<PreparedValue> list, Condition condition, boolean firstWhere) {
		PreparedValue preparedValue = null;

		ConditionImpl conditionImpl = (ConditionImpl) condition;
		List<Expression> expList = conditionImpl.getExpList();
		Expression expression = null;
		for (int j = 0; j < expList.size(); j++) {
			expression = expList.get(j);

			if (firstWhere) {
				sqlBuffer.append(" where ");
				firstWhere = false;
			} else {
				String opType = expression.getOpType();
				if (Op.in.getOperator().equalsIgnoreCase(opType) || Op.notIn.getOperator().equalsIgnoreCase(opType)) {
					adjustAnd(sqlBuffer);
					sqlBuffer.append(_toColumnName(expression.getFieldName()));
					sqlBuffer.append(" ");
					sqlBuffer.append(expression.getOpType());
					sqlBuffer.append(" (");
					sqlBuffer.append("?");
					String str = expression.getValue().toString();
					String values[] = str.trim().split(",");

					for (int i = 1; i < values.length; i++) { //start 1
						sqlBuffer.append(",?");
					}

					sqlBuffer.append(")");

					valueBuffer.append(","); //valueBuffer
					valueBuffer.append(expression.getValue());

					for (int i = 0; i < values.length; i++) {

						preparedValue = new PreparedValue();
						preparedValue.setType(values[i].getClass().getName());
						preparedValue.setValue(values[i]);
						list.add(preparedValue);
					}
					continue;
				} else if (Op.like.getOperator().equalsIgnoreCase(opType) || Op.notLike.getOperator().equalsIgnoreCase(opType)) {
					//				else if (opType == Op.like  || opType == Op.notLike) {
					adjustAnd(sqlBuffer);

					sqlBuffer.append(_toColumnName(expression.getFieldName()));
					sqlBuffer.append(expression.getOpType());
					sqlBuffer.append("?");

					valueBuffer.append(","); //valueBuffer
					valueBuffer.append(expression.getValue());

					preparedValue = new PreparedValue();
					preparedValue.setType(expression.getValue().getClass().getName());
					preparedValue.setValue(expression.getValue());
					list.add(preparedValue);

					continue;
				} else if ("orderBy".equalsIgnoreCase(opType)) {

					if (SuidType.SELECT != conditionImpl.getSuidType()) {
						throw new BeeErrorGrammarException(conditionImpl.getSuidType() + " do not support order by !");
					}

					sqlBuffer.append(expression.getValue());//order by或者,
					sqlBuffer.append(_toColumnName(expression.getFieldName()));
					if (3 == expression.getOpNum()) { //指定 desc,asc
						sqlBuffer.append(ONE_SPACE);
						sqlBuffer.append(expression.getValue2());
					}
					continue;
				}

				if (expression.getOpNum() == -1) { // (
					adjustAnd(sqlBuffer);
					sqlBuffer.append(expression.getValue());
					continue;
				}
				if (expression.getOpNum() == 0) {// )
					sqlBuffer.append(expression.getValue());
					isNeedAnd = true;
					continue;

				} else if (expression.getOpNum() == 1) { // or operation 
					sqlBuffer.append(" ");
					sqlBuffer.append(expression.getValue());
					sqlBuffer.append(" ");
					isNeedAnd = false;
					continue;
				}
				adjustAnd(sqlBuffer);

			}

			sqlBuffer.append(_toColumnName(expression.getFieldName()));

			if (expression.getValue() == null) {
				sqlBuffer.append(" is null");
			} else {
				//				sqlBuffer.append("=");
				sqlBuffer.append(expression.getOpType());
				sqlBuffer.append("?");

				valueBuffer.append(",");
				valueBuffer.append(expression.getValue());

				preparedValue = new PreparedValue();
				preparedValue.setType(expression.getValue().getClass().getName());
				preparedValue.setValue(expression.getValue());
				list.add(preparedValue);
			}
			isNeedAnd = true;
		} //end expList for 

		//>>>>>>>>>>>>>>>>>>>paging
		Integer start = conditionImpl.getStart();
		Integer size = conditionImpl.getSize();
		String sql = "";
		if (start != null && size != null) {
			sql = dbFeature.toPageSql(sqlBuffer.toString(), start, size);
			//			sqlBuffer=new StringBuffer(sql); //new 之后不是原来的sqlBuffer,不能带回去.
			sqlBuffer.delete(0, sqlBuffer.length());
			sqlBuffer.append(sql);
		} else if (size != null) {
			sql = dbFeature.toPageSql(sqlBuffer.toString(), size);
			//			sqlBuffer=new StringBuffer(sql);
			sqlBuffer.delete(0, sqlBuffer.length());
			sqlBuffer.append(sql);
		}
		//>>>>>>>>>>>>>>>>>>>paging
	}

	private static String _toColumnName(String fieldName) {
		return NameTranslateHandle.toColumnName(fieldName);
	}

	private static void adjustAnd(StringBuffer sqlBuffer) {
		if (isNeedAnd) {
			sqlBuffer.append(" and ");
			isNeedAnd = false;
		}
	}
}
