/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.List;

import org.teasoft.bee.osql.Condition;
import org.teasoft.bee.osql.FunctionType;
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
	
	//v1.7.2  add return value for delete control
	static boolean processCondition(StringBuffer sqlBuffer, StringBuffer valueBuffer, 
			List<PreparedValue> list, Condition condition, boolean firstWhere) {
		
		 return processCondition(sqlBuffer, valueBuffer, list, condition, firstWhere, null);
	}
	//v1.7.2  add return value for delete control
	static boolean processCondition(StringBuffer sqlBuffer, StringBuffer valueBuffer, 
			List<PreparedValue> list, Condition condition, boolean firstWhere,String useSubTableNames[]) {
		PreparedValue preparedValue = null;
		
		boolean isFirstWhere=firstWhere; //v1.7.2 return for control whether allow to delete whole records in one table

		ConditionImpl conditionImpl = (ConditionImpl) condition;
		List<Expression> expList = conditionImpl.getExpList();
		Expression expression = null;
		
		Integer start = conditionImpl.getStart();
		
		if (start!=null && SuidType.SELECT != conditionImpl.getSuidType()) {
			throw new BeeErrorGrammarException(conditionImpl.getSuidType() + " do not support paging with start !");
		} 
		
		
		for (int j = 0; j < expList.size(); j++) {
			expression = expList.get(j);
			String opType = expression.getOpType();
			
			if ( "groupBy".equalsIgnoreCase(opType) || "having".equalsIgnoreCase(opType) ) {
				if (SuidType.SELECT != conditionImpl.getSuidType()) {
					throw new BeeErrorGrammarException(conditionImpl.getSuidType() + " do not support the opType: "+opType+"!");
				} 
			}
			//mysql's delete,update can use order by.

			if (firstWhere) {
				if ( "groupBy".equalsIgnoreCase(opType) || "having".equalsIgnoreCase(opType) || "orderBy".equalsIgnoreCase(opType)) {
					firstWhere = false;
				} else {
					sqlBuffer.append(" where ");
					firstWhere = false;
					isNeedAnd = false;
					isFirstWhere=false; //for return.where过滤条件
				}
			}
			//			} else {
			if (Op.in.getOperator().equalsIgnoreCase(opType) || Op.notIn.getOperator().equalsIgnoreCase(opType)) {
				adjustAnd(sqlBuffer);
				sqlBuffer.append(_toColumnName(expression.getFieldName(),useSubTableNames));
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

				isNeedAnd = true;
				continue;
			} else if (Op.like.getOperator().equalsIgnoreCase(opType) || Op.notLike.getOperator().equalsIgnoreCase(opType)) {
				//				else if (opType == Op.like  || opType == Op.notLike) {
				adjustAnd(sqlBuffer);

				sqlBuffer.append(_toColumnName(expression.getFieldName(),useSubTableNames));
				sqlBuffer.append(expression.getOpType());
				sqlBuffer.append("?");

				valueBuffer.append(","); //valueBuffer
				valueBuffer.append(expression.getValue());

				preparedValue = new PreparedValue();
				preparedValue.setType(expression.getValue().getClass().getName());
				preparedValue.setValue(expression.getValue());
				list.add(preparedValue);

				isNeedAnd = true;
				continue;
			} else if (" between ".equalsIgnoreCase(opType) || " not between ".equalsIgnoreCase(opType)) {

				adjustAnd(sqlBuffer);

				sqlBuffer.append(_toColumnName(expression.getFieldName(),useSubTableNames));
				sqlBuffer.append(opType);
				sqlBuffer.append("?");
				sqlBuffer.append(" and ");
				sqlBuffer.append("?");

				valueBuffer.append(","); //valueBuffer
				valueBuffer.append(expression.getValue()); //low
				valueBuffer.append(","); //valueBuffer
				valueBuffer.append(expression.getValue2()); //high

				preparedValue = new PreparedValue();
				preparedValue.setType(expression.getValue().getClass().getName());
				preparedValue.setValue(expression.getValue());
				list.add(preparedValue);

				preparedValue = new PreparedValue();
				preparedValue.setType(expression.getValue2().getClass().getName());
				preparedValue.setValue(expression.getValue2());
				list.add(preparedValue);

				isNeedAnd = true;
				continue;

			} else if ("groupBy".equalsIgnoreCase(opType)) {
				if (SuidType.SELECT != conditionImpl.getSuidType()) {
					throw new BeeErrorGrammarException("BeeErrorGrammarException: "+conditionImpl.getSuidType() + " do not support 'group by' !");
				}

				sqlBuffer.append(expression.getValue());//group by或者,
				sqlBuffer.append(_toColumnName(expression.getFieldName(),useSubTableNames));

				continue;
			} else if ("having".equalsIgnoreCase(opType)) {
				if (SuidType.SELECT != conditionImpl.getSuidType()) {
					throw new BeeErrorGrammarException(conditionImpl.getSuidType() + " do not support 'having' !");
				}

				if (2 == expression.getOpNum()) {
					sqlBuffer.append(expression.getValue());//having 或者 and
					sqlBuffer.append(expression.getValue2()); //表达式
				} else if (5 == expression.getOpNum()) { //having min(field)>=60
					sqlBuffer.append(expression.getValue());//having 或者 and
					sqlBuffer.append(expression.getValue3()); //fun
					sqlBuffer.append("(");
					if (FunctionType.COUNT.getName().equals(expression.getValue3()) && expression.getFieldName() != null && "*".equals(expression.getFieldName().trim())) {
						sqlBuffer.append("*");
					} else {
						sqlBuffer.append(_toColumnName(expression.getFieldName(),useSubTableNames));
					}

					sqlBuffer.append(")");
					sqlBuffer.append(expression.getValue4()); //Op
					//		                  sqlBuffer.append(expression.getValue2()); 
					sqlBuffer.append("?");

					valueBuffer.append(",");
					valueBuffer.append(expression.getValue2()); // here is value2

					preparedValue = new PreparedValue();
					preparedValue.setType(expression.getValue2().getClass().getName());
					preparedValue.setValue(expression.getValue2());
					list.add(preparedValue);
				}

				continue;
			}

			else if ("orderBy".equalsIgnoreCase(opType)) {

				if (SuidType.SELECT != conditionImpl.getSuidType()) {
					throw new BeeErrorGrammarException(conditionImpl.getSuidType() + " do not support 'order by' !");
				}

				sqlBuffer.append(expression.getValue());//order by或者,
				if (4 == expression.getOpNum()) { //order by max(total) desc
					sqlBuffer.append(expression.getValue3());
					sqlBuffer.append("(");
					sqlBuffer.append(_toColumnName(expression.getFieldName(),useSubTableNames));
					sqlBuffer.append(")");
				} else {
					sqlBuffer.append(_toColumnName(expression.getFieldName(),useSubTableNames));
				}

				if (3 == expression.getOpNum() || 4 == expression.getOpNum()) { //指定 desc,asc
					sqlBuffer.append(ONE_SPACE);
					sqlBuffer.append(expression.getValue2());
				}
				continue;
			}//end orderBy

			if (expression.getOpNum() == -2) { // (
				adjustAnd(sqlBuffer);
				sqlBuffer.append(expression.getValue());
				continue;
			}
			if (expression.getOpNum() == -1) {// )
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

			//}

			sqlBuffer.append(_toColumnName(expression.getFieldName(),useSubTableNames));

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
		
		if (SuidType.SELECT == conditionImpl.getSuidType()) {
			
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
		}
		//>>>>>>>>>>>>>>>>>>>paging
		
		return isFirstWhere;
	}

	private static String _toColumnName(String fieldName) {
		return NameTranslateHandle.toColumnName(fieldName);
	}
			
	private static String _toColumnName(String fieldName,String useSubTableNames[]) {
		
		if(useSubTableNames==null) return _toColumnName(fieldName);   //one table type
		
		String t_fieldName="";
		String t_tableName="";
		String t_tableName_dot;
		String find_tableName="";
		int index=fieldName.indexOf(".");
		if(index>-1){
			t_fieldName=fieldName.substring(index+1);
			t_tableName=fieldName.substring(0,index);
			t_tableName_dot=fieldName.substring(0,index+1);
			// check whether is useSubTableName
			if(useSubTableNames[0]!=null && useSubTableNames[0].startsWith(t_tableName_dot)){
				find_tableName=t_tableName;
			}else if(useSubTableNames[1]!=null && useSubTableNames[1].startsWith(t_tableName_dot)){
				find_tableName=t_tableName;
			}else{
				find_tableName=NameTranslateHandle.toTableName(t_tableName);
			}
			
			return find_tableName+"."+NameTranslateHandle.toColumnName(t_fieldName);
		}
		return NameTranslateHandle.toColumnName(fieldName);
	}

	private static void adjustAnd(StringBuffer sqlBuffer) {
		if (isNeedAnd) {
			sqlBuffer.append(" and ");
			isNeedAnd = false;
		}
	}
}
