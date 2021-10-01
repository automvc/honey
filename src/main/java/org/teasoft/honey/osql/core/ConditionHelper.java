/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.Condition;
import org.teasoft.bee.osql.FunctionType;
import org.teasoft.bee.osql.Op;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.osql.exception.BeeErrorGrammarException;
import org.teasoft.honey.osql.core.ConditionImpl.FunExpress;
import org.teasoft.honey.util.StringUtils;

/**
 * @author Kingstar
 * @since  1.6
 */
public class ConditionHelper {
	static boolean isNeedAnd = true;
	private static final String ONE_SPACE = " ";

//	private static DbFeature dbFeature = BeeFactory.getHoneyFactory().getDbFeature();
	
	
	private static final String setAdd = "setAdd";
	private static final String setMultiply = "setMultiply";
	
	private static final String setAddField = "setAddField";
	private static final String setMultiplyField = "setMultiplyField";
	
	private static final String setWithField="setWithField";
	
	private static DbFeature getDbFeature() {
		return BeeFactory.getHoneyFactory().getDbFeature();
	}
	
	//ForUpdate
//	static boolean processConditionForUpdateSet(StringBuffer sqlBuffer, StringBuffer valueBuffer, List<PreparedValue> list, Condition condition) {
	static boolean processConditionForUpdateSet(StringBuffer sqlBuffer, List<PreparedValue> list, Condition condition) { //delete valueBuffer
		ConditionImpl conditionImpl = (ConditionImpl) condition;
		List<Expression> updateSetList = conditionImpl.getUpdateExpList();
		boolean firstSet = true;

//		if ( setAdd.equalsIgnoreCase(opType) || setMultiply.equalsIgnoreCase(opType) ) {
		if (updateSetList != null && updateSetList.size() > 0) {
			if (SuidType.UPDATE != conditionImpl.getSuidType()) {
				throw new BeeErrorGrammarException(conditionImpl.getSuidType() + " do not support the method set ,setAdd or setMultiply!");
			}
		}

		PreparedValue preparedValue = null;
		Expression expression = null;

		for (int j = 0; updateSetList!=null && j < updateSetList.size(); j++) {
			expression = updateSetList.get(j);
			String opType = expression.getOpType();

//				update orders set total=total+0.5;
//				mysql is ok. as below:
//				update orders set total=total+?   [values]: -0.1  
			
			if (opType!=null && expression.getValue() == null) {  //TODO BUG  // UPDATE,  fieldName: toolPayWay, the num of null is null
//				throw new BeeErrorGrammarException(conditionImpl.getSuidType() + ", method:"+opType+", fieldName:"+expression.getFieldName()+", the value is null");
				throw new BeeErrorGrammarException("the value is null ("+conditionImpl.getSuidType() + ", method:"+opType+", fieldName:"+expression.getFieldName()+")!");
//			    setWithField("name",null);   //这种,不在这里抛出,字段检测时会抛
//				String n=null; setAdd("total", n); //这个也是.第二个参数是作为字段,会被检测
			} else {

				if (firstSet) {
					firstSet = false;
				} else {
					sqlBuffer.append(",");
				}
				sqlBuffer.append(_toColumnName(expression.getFieldName(), null));
				sqlBuffer.append("=");
				
				//v1.9.8
				if(opType==null && expression.getValue() == null) { //set("fieldName",null)
					sqlBuffer.append(K.Null);
					continue;
				}
				
				if(opType!=null) { //只有set(arg1,arg2) opType=null
					if (setWithField.equals(opType)) {
						sqlBuffer.append(_toColumnName((String)expression.getValue()));
					}else {
						sqlBuffer.append(_toColumnName(expression.getFieldName()));  //price=[price]+delta   doing [price]
					}
				}
				   
				
				if (setAddField.equals(opType)) {//eg:setAdd("price","delta")--> price=price+delta
					sqlBuffer.append("+");
					sqlBuffer.append(_toColumnName((String)expression.getValue()));
					continue; //no ?,  don't need set value
				} else if (setMultiplyField.equals(opType)) {
					sqlBuffer.append("*");
					sqlBuffer.append(_toColumnName((String)expression.getValue()));
					continue; //no ?,  don't need set value
				}
				
				if (setAdd.equals(opType)) {
//					if ((double) expression.getValue() < 0)
//						sqlBuffer.append("-"); // bug 负负得正
//					else
					sqlBuffer.append("+");
				} else if (setMultiply.equals(opType)) {
					sqlBuffer.append("*");
				}
				
				if (setWithField.equals(opType)) {
                      //nothing 
					  //for : set field1=field2
				} else {
					sqlBuffer.append("?");

//					valueBuffer.append(","); // do not need check. at final will delete the first letter.
//					valueBuffer.append(expression.getValue());

					preparedValue = new PreparedValue();
					preparedValue.setType(expression.getValue().getClass().getName());
					preparedValue.setValue(expression.getValue());
					list.add(preparedValue);
				}
			}

		}

		return firstSet;
	}
	
	static boolean processCondition(StringBuffer sqlBuffer, 
		 List<PreparedValue> list, Condition condition, boolean firstWhere) {
//		 StringBuffer valueBuffer=new StringBuffer(); //don't use, just adapt the old method
//		 return processCondition(sqlBuffer, valueBuffer, list, condition, firstWhere, null);
		 return processCondition(sqlBuffer, list, condition, firstWhere, null);
	}
	
	//v1.7.2  add return value for delete/update control
//	static boolean processCondition(StringBuffer sqlBuffer, StringBuffer valueBuffer, 
//			List<PreparedValue> list, Condition condition, boolean firstWhere) {
//		
////		 return processCondition(sqlBuffer, valueBuffer, list, condition, firstWhere, null);
//		 return processCondition(sqlBuffer, list, condition, firstWhere, null);
//	}
	//v1.7.2  add return value for delete/update control
	static boolean processCondition(StringBuffer sqlBuffer, 
			List<PreparedValue> list, Condition condition, boolean firstWhere,String useSubTableNames[]) {
		
		if(condition==null) return firstWhere;
		
		PreparedValue preparedValue = null;
		
		boolean isFirstWhere=firstWhere; //v1.7.2 return for control whether allow to delete/update whole records in one table

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
//					sqlBuffer.append(" where ");
					sqlBuffer.append(" ").append(K.where).append(" ");
					firstWhere = false;
					isNeedAnd = false;
					isFirstWhere=false; //for return. where过滤条件
				}
			}
			//			} else {
			if (Op.in.getOperator().equalsIgnoreCase(opType) || Op.notIn.getOperator().equalsIgnoreCase(opType)) {
				
				String v = expression.getValue().toString();
				
//				if(StringUtils.isBlank(v)) continue; //v1.9.8    in的值不允许为空             这样会有安全隐患, 少了一个条件,会更改很多数据.
				
				adjustAnd(sqlBuffer);
				sqlBuffer.append(_toColumnName(expression.getFieldName(),useSubTableNames));
//				sqlBuffer.append(" ");
//				sqlBuffer.append(expression.getOpType());
				if(HoneyUtil.isSqlKeyWordUpper()) sqlBuffer.append(expression.getOpType().toUpperCase());
				else sqlBuffer.append(expression.getOpType());
				sqlBuffer.append(" (");
				sqlBuffer.append("?");
				String values[] = v.trim().split(",");

				for (int i = 1; i < values.length; i++) { //start 1
					sqlBuffer.append(",?");
				}

				sqlBuffer.append(")");

//				valueBuffer.append(","); //valueBuffer
//				valueBuffer.append(expression.getValue());

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
//				sqlBuffer.append(expression.getOpType());
				if(HoneyUtil.isSqlKeyWordUpper()) sqlBuffer.append(expression.getOpType().toUpperCase());
				else sqlBuffer.append(expression.getOpType());
				sqlBuffer.append("?");

//				valueBuffer.append(","); //valueBuffer
//				valueBuffer.append(expression.getValue());

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
				sqlBuffer.append(" "+K.and+" ");
				sqlBuffer.append("?");

//				valueBuffer.append(","); //valueBuffer
//				valueBuffer.append(expression.getValue()); //low
//				valueBuffer.append(","); //valueBuffer
//				valueBuffer.append(expression.getValue2()); //high

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

//				if (2 == expression.getOpNum()) {//having("count(*)>5")
//					sqlBuffer.append(expression.getValue());//having 或者 and
//					sqlBuffer.append(expression.getValue2()); //表达式
//				} else if (5 == expression.getOpNum()) { //having(FunctionType.MIN, "field", Op.ge, 60)
				if (5 == expression.getOpNum()) { //having(FunctionType.MIN, "field", Op.ge, 60)
					sqlBuffer.append(expression.getValue());//having 或者 and
//					sqlBuffer.append(expression.getValue3()); //fun
					sqlBuffer.append(FunAndOrderTypeMap.transfer(expression.getValue3().toString())); //fun
					sqlBuffer.append("(");
					if (FunctionType.COUNT.getName().equals(expression.getValue3()) && "*".equals(expression.getFieldName().trim())) {
						sqlBuffer.append("*");
					} else {
						sqlBuffer.append(_toColumnName(expression.getFieldName(),useSubTableNames));
					}

					sqlBuffer.append(")");
					sqlBuffer.append(expression.getValue4()); //Op
					//		                  sqlBuffer.append(expression.getValue2()); 
					sqlBuffer.append("?");

//					valueBuffer.append(",");
//					valueBuffer.append(expression.getValue2()); // here is value2

					preparedValue = new PreparedValue();
					preparedValue.setType(expression.getValue2().getClass().getName());
					preparedValue.setValue(expression.getValue2());
					list.add(preparedValue);
				}

				continue;
			}else if ("orderBy".equalsIgnoreCase(opType)) {

				if (SuidType.SELECT != conditionImpl.getSuidType()) {
					throw new BeeErrorGrammarException(conditionImpl.getSuidType() + " do not support 'order by' !");
				}

				sqlBuffer.append(expression.getValue());//order by或者,
				if (4 == expression.getOpNum()) { //order by max(total)
//					sqlBuffer.append(expression.getValue3());
					sqlBuffer.append(FunAndOrderTypeMap.transfer(expression.getValue3().toString()));
					sqlBuffer.append("(");
					sqlBuffer.append(_toColumnName(expression.getFieldName(),useSubTableNames));
					sqlBuffer.append(")");
				} else {
					sqlBuffer.append(_toColumnName(expression.getFieldName(),useSubTableNames));
				}

				if (3 == expression.getOpNum() || 4 == expression.getOpNum()) { //指定 desc,asc
					sqlBuffer.append(ONE_SPACE);
//					sqlBuffer.append(expression.getValue2());
					sqlBuffer.append(FunAndOrderTypeMap.transfer(expression.getValue2().toString()));
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

			sqlBuffer.append(_toColumnName(expression.getFieldName(),useSubTableNames));   //TODO ???

			if (expression.getValue() == null) {
				if("=".equals(expression.getOpType())){
//					sqlBuffer.append(" is null");
					sqlBuffer.append(" "+K.isNull);
				}else{
					sqlBuffer.append(" "+K.isNotNull);
					if(! "!=".equals(expression.getOpType())) {
						String fieldName=_toColumnName(expression.getFieldName(),useSubTableNames);
						Logger.warn(fieldName+expression.getOpType()+"null transfer to : " +fieldName+" "+K.isNotNull);
					}
				}
			} else {
				if (expression.getOpNum() == -3) { //eg:field1=field2   could not use for having in mysql 
					sqlBuffer.append(expression.getOpType());
					sqlBuffer.append(expression.getValue());
				} else {
					sqlBuffer.append(expression.getOpType());
					sqlBuffer.append("?");

//				    valueBuffer.append(",");
//				    valueBuffer.append(expression.getValue());

					preparedValue = new PreparedValue();
					preparedValue.setType(expression.getValue().getClass().getName());
					preparedValue.setValue(expression.getValue());
					list.add(preparedValue);
				}
			}
			isNeedAnd = true;
		} //end expList for 

		//>>>>>>>>>>>>>>>>>>>paging start
		
		if (SuidType.SELECT == conditionImpl.getSuidType()) {
			if (! OneTimeParameter.isTrue(StringConst.Select_Fun)) {
				Integer size = conditionImpl.getSize();
				String sql = "";
				if (start != null && size != null) {
					HoneyUtil.regPagePlaceholder();
					sql = getDbFeature().toPageSql(sqlBuffer.toString(), start, size);
					//			sqlBuffer=new StringBuffer(sql); //new 之后不是原来的sqlBuffer,不能带回去.
					sqlBuffer.delete(0, sqlBuffer.length());
					sqlBuffer.append(sql);
					HoneyUtil.setPageNum(list);
				} else if (size != null) {
					HoneyUtil.regPagePlaceholder();
					sql = getDbFeature().toPageSql(sqlBuffer.toString(), size);
					//			sqlBuffer=new StringBuffer(sql);
					sqlBuffer.delete(0, sqlBuffer.length());
					sqlBuffer.append(sql);
					HoneyUtil.setPageNum(list);
				}
			}
		}
		//>>>>>>>>>>>>>>>>>>>paging end
		

		//>>>>>>>>>>>>>>>>>>>for update
		//仅用于SQL的单个表select
		if (useSubTableNames==null && SuidType.SELECT == conditionImpl.getSuidType()) {
			
			Boolean isForUpdate=conditionImpl.getForUpdate();
			if(isForUpdate!=null && isForUpdate.booleanValue()){
//				sqlBuffer.append(" for update ");
				sqlBuffer.append(" "+K.forUpdate+" ");
			}
		}
		//>>>>>>>>>>>>>>>>>>>for update
		
		
		//check
		if (SuidType.SELECT == conditionImpl.getSuidType()) {
			List<Expression> updateSetList = conditionImpl.getUpdateExpList();
			if (updateSetList != null && updateSetList.size() > 0) {
				Logger.warn("Use Condition's set method(s) in SELECT type, but it just effect in UPDATE type! Involved field(s): "+conditionImpl.getUpdatefields());
			}
		}
		
		return isFirstWhere;
	}
	
	static <T> String processSelectField(String columnNames, Condition condition) {
		return processSelectField(columnNames, condition, null);
	}
	
	static <T> String processSelectField(String columnNames, Condition condition,Map<String,String> subDulFieldMap) {
		
		if(condition==null) return null;

		ConditionImpl conditionImpl = (ConditionImpl) condition;
		if (SuidType.SELECT != conditionImpl.getSuidType()) {
			throw new BeeErrorGrammarException(conditionImpl.getSuidType() + " do not support specifying partial fields by method selectField(String) !");
		}
		String selectField = conditionImpl.getSelectField();

		if (selectField == null) return null;

		return HoneyUtil.checkAndProcessSelectFieldViaString(columnNames, selectField,subDulFieldMap);
	}
	
	public static String processFunction(String columnNames,Condition condition) {
//		if(condition==null) return null;

		ConditionImpl conditionImpl = (ConditionImpl) condition;
		List<FunExpress> funExpList=conditionImpl.getFunExpList();
		String columnName;
		String funStr="";
		boolean isFirst=true;
		String alias;
		for (int i = 0; i < funExpList.size(); i++) {
			if("*".equals(funExpList.get(i).getField())) {
				columnName="*";
			}else {
			columnName=HoneyUtil.checkAndProcessSelectFieldViaString(columnNames, funExpList.get(i).getField(),null);
			}
			if(isFirst) {
				isFirst=false;
			}else {
				funStr+=",";
			}
//			funStr+=funExpList.get(i).getFunctionType().getName()+"("+columnName+")"; // funType要能转大小写风格
//			String functionTypeName=funExpList.get(i).getFunctionType().getName();
			String functionTypeName=funExpList.get(i).getFunctionType();
			funStr+=FunAndOrderTypeMap.transfer(functionTypeName)+"("+columnName+")"; 
			
			alias=funExpList.get(i).getAlias();
			if(StringUtils.isNotBlank(alias)) funStr+=" "+K.as+" "+alias;
		}
		
		return funStr;
	}
	
	public static void processOnExpression(Condition condition, MoreTableStruct moreTableStruct[],
			List<PreparedValue> list) {
		
		if (condition == null || moreTableStruct == null) return;
		
		List<PreparedValue> list2=new ArrayList<>();

		ConditionImpl conditionImpl = (ConditionImpl) condition;
		List<Expression> onExpList = conditionImpl.getOnExpList();
		StringBuffer onExpBuffer = new StringBuffer();
		Expression exp = null;
		int sub1 = 0, sub2 = 0;
		for (int i = 0; i < onExpList.size(); i++) {

			exp = onExpList.get(i);
			if (moreTableStruct[0].joinTableNum == 1 && i != 0) {
				onExpBuffer.append(K.space).append(K.and).append(K.space);
			}
			onExpBuffer.append(_toColumnName(exp.getFieldName()));
			onExpBuffer.append(K.space);
			onExpBuffer.append(exp.opType);
			//			onExpBuffer.append(K.space);
			//			onExpBuffer.append(exp.getValue());
			onExpBuffer.append("?");

			if (moreTableStruct[0].joinTableNum == 2) {
				String fieldName = exp.getFieldName();
				if (fieldName.startsWith(moreTableStruct[2].tableName + ".")
						|| (moreTableStruct[2].hasSubAlias
								&& fieldName.startsWith(moreTableStruct[2].subAlias + "."))) { //第2个从表
					if (sub2 != 0) moreTableStruct[2].onExpression += K.space + K.and + K.space;
					moreTableStruct[2].onExpression += onExpBuffer.toString();
					sub2++;
					addValeToList(list2, exp);
				} else {
					if (sub1 != 0) moreTableStruct[2].onExpression += K.space + K.and + K.space;
					moreTableStruct[1].onExpression += onExpBuffer.toString();
					sub1++;
					addValeToList(list, exp);
				}
				if (i != onExpList.size() - 1) onExpBuffer = new StringBuffer();
			} else {
				addValeToList(list, exp);
			}

			if (i == onExpList.size() - 1) {
				if (moreTableStruct[0].joinTableNum == 1)
					moreTableStruct[1].onExpression = onExpBuffer.toString();
				else if (sub2 != 0) list.addAll(list2);
			}
		}

	}
	
	private static void addValeToList(List<PreparedValue> list,Expression exp) {
		PreparedValue preparedValue = new PreparedValue();
		preparedValue.setType(exp.getValue().getClass().getName());
		preparedValue.setValue(exp.getValue());
		list.add(preparedValue);
	}
	
	
	private static String _toColumnName(String fieldName) {
		return NameTranslateHandle.toColumnName(fieldName);
	}
	
	private static String _toColumnName(String fieldName,String useSubTableNames[]) {
		if(StringUtils.isBlank(fieldName)) return fieldName;
		if(!fieldName.contains(",")) return _toColumnName0(fieldName, useSubTableNames);
		
		String str[]=fieldName.split(",");
		String newFields="";
		int len=str.length;
		for (int i = 0; i < len; i++) {
			newFields+=_toColumnName0(str[i],useSubTableNames);
			if(i!=len-1) newFields+=",";
		}
		return newFields;
		
	}
			
	private static String _toColumnName0(String fieldName,String useSubTableNames[]) {
		
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
				OneTimeParameter.setTrueForKey(StringConst.DoNotCheckAnnotation);//adjust for @Table
				find_tableName=NameTranslateHandle.toTableName(t_tableName);
			}
			
			return find_tableName+"."+NameTranslateHandle.toColumnName(t_fieldName);
		}else {
			fieldName=useSubTableNames[2]+"."+fieldName;
		}
		return NameTranslateHandle.toColumnName(fieldName);
	}

	private static void adjustAnd(StringBuffer sqlBuffer) {
		if (isNeedAnd) {
			sqlBuffer.append(" "+K.and+" ");
			isNeedAnd = false;
		}
	}
	
	public static Integer getPageSize(Condition condition) {
		if(condition==null) return null;
		ConditionImpl conditionImpl = (ConditionImpl) condition;
		return conditionImpl.getSize();
	}
}
