/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.teasoft.bee.osql.FunctionType;
import org.teasoft.bee.osql.Op;
import org.teasoft.bee.osql.OrderType;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.osql.exception.BeeErrorGrammarException;
import org.teasoft.bee.osql.exception.BeeIllegalSQLException;
import org.teasoft.bee.sharding.FunStruct;
import org.teasoft.bee.sharding.GroupFunStruct;
import org.teasoft.honey.osql.core.ConditionImpl.FunExpress;
import org.teasoft.honey.osql.dialect.sqlserver.SqlServerPagingStruct;
import org.teasoft.honey.osql.util.NameCheckUtil;
import org.teasoft.honey.sharding.ShardingReg;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.util.StringUtils;

/**
 * Condition的帮助类.Condition Helper class.
 * @author Kingstar
 * @since  1.6
 */
public class ConditionHelper {
//	static boolean isNeedAnd = true;    //bug 2021-10-14   not thread safe
	private static final String ONE_SPACE = " ";

	private static final String setAdd = "setAdd";
	private static final String setMultiply = "setMultiply";
	
	private static final String setAddField = "setAddField";
	private static final String setMultiplyField = "setMultiplyField";
	
	private static final String setWithField="setWithField";
	
	private static final String GROUP_BY = "groupBy";
	private static final String HAVING = "having";
	
	private ConditionHelper(){}
	
	private static DbFeature getDbFeature() {
		return BeeFactory.getHoneyFactory().getDbFeature();
	}
	
	//ForUpdate
	static boolean processConditionForUpdateSet(StringBuffer sqlBuffer, List<PreparedValue> list, 
			Condition condition) {
		return processUpdateSetCondition(sqlBuffer, list, condition,true).isFirst(); // just return isFirst
	}
	
	//2.4.0
	static UpdateSetConditionWrap processUpdateSetCondition(Condition condition) {
		return processUpdateSetCondition(new StringBuffer(), new ArrayList<PreparedValue>(), condition,true);
	}
	
	static UpdateSetConditionWrap processUpdateSetCondition(StringBuffer sqlBuffer, List<PreparedValue> list, 
			Condition condition,boolean firstSet) {
		
		Class entityClass = (Class) OneTimeParameter.getAttribute(StringConst.Column_EC);
//		boolean firstSet = true;
		if(condition==null) return new UpdateSetConditionWrap(sqlBuffer, list, firstSet);;
		
		ConditionImpl conditionImpl = (ConditionImpl) condition;
		List<Expression> updateSetList = conditionImpl.getUpdateExpList();
		

		if (updateSetList != null && updateSetList.size() > 0) {
			if (SuidType.UPDATE != conditionImpl.getSuidType()) {
				throw new BeeErrorGrammarException(conditionImpl.getSuidType() + " do not support the method set ,setAdd or setMultiply!");
			}
		}

		Expression expression = null;

		for (int j = 0; updateSetList!=null && j < updateSetList.size(); j++) {
			expression = updateSetList.get(j);
			String opType = expression.getOpType();

//				update orders set total=total+0.5;
//				mysql is ok. as below:
//				update orders set total=total+?   [values]: -0.1  
			
			if (opType!=null && expression.getValue() == null) {  // BUG  // UPDATE,  fieldName: toolPayWay, the num of null is null
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
				sqlBuffer.append(_toColumnName(expression.getFieldName(), null,entityClass));
				sqlBuffer.append("=");
				
				//v1.9.8
				if(opType==null && expression.getValue() == null) { //set("fieldName",null)
					sqlBuffer.append(K.Null);
					continue;
				}
				
				if(opType!=null) {
					if (setWithField.equals(opType)) {
						sqlBuffer.append(_toColumnName((String)expression.getValue(),entityClass));
					}else {
						sqlBuffer.append(_toColumnName(expression.getFieldName(),entityClass));  //price=[price]+delta   doing [price]
					}
				}
				   
				
				if (setAddField.equals(opType)) {//eg:setAdd("price","delta")--> price=price+delta
					sqlBuffer.append("+");
					sqlBuffer.append(_toColumnName((String)expression.getValue(),entityClass));
					continue; //no ?,  don't need set value
				} else if (setMultiplyField.equals(opType)) {
					sqlBuffer.append("*");
					sqlBuffer.append(_toColumnName((String)expression.getValue(),entityClass));
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

					addValeToPvList(list, expression.getValue());
				}
			}
		}
		
//		return firstSet;
		return new UpdateSetConditionWrap(sqlBuffer, list, firstSet);
	}
	
	static boolean processCondition(StringBuffer sqlBuffer, 
		 List<PreparedValue> list, Condition condition, boolean firstWhere) {
		 return processCondition(sqlBuffer, list, condition, firstWhere, null);
	}
	
	//v1.7.2  add return value for delete/update control
	static boolean processCondition(StringBuffer sqlBuffer, 
			List<PreparedValue> list, Condition condition, boolean firstWhere,String useSubTableNames[]) {
		
		Class entityClass = (Class) OneTimeParameter.getAttribute(StringConst.Column_EC);
		
		if(condition==null) return firstWhere;
		
		boolean isNeedAnd = true;
		
		boolean isFirstWhere=firstWhere; //v1.7.2 return for control whether allow to delete/update whole records in one table

		ConditionImpl conditionImpl = (ConditionImpl) condition;
		List<Expression> expList = conditionImpl.getExpList();
		Expression expression = null;
		
		Integer start = conditionImpl.getStart();
		
		if (start!=null && SuidType.SELECT != conditionImpl.getSuidType()) {
			throw new BeeErrorGrammarException(conditionImpl.getSuidType() + " do not support paging with start !");
		} 
		String columnName="";
		for (int j = 0; j < expList.size(); j++) {
			expression = expList.get(j);
			String opType = expression.getOpType();
			
			columnName=_toColumnName(expression.getFieldName(),useSubTableNames,entityClass);
			
			if ( GROUP_BY.equalsIgnoreCase(opType) || HAVING.equalsIgnoreCase(opType) ) {
				if (SuidType.SELECT != conditionImpl.getSuidType()) {
					throw new BeeErrorGrammarException(conditionImpl.getSuidType() + " do not support the opType: "+opType+"!");
				} 
			}
			//mysql's delete,update can use order by.

			if (firstWhere) {
				if ( GROUP_BY.equalsIgnoreCase(opType) || HAVING.equalsIgnoreCase(opType) || "orderBy".equalsIgnoreCase(opType)) {
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
				
//				String v = expression.getValue().toString(); //close in V1.17
				Object v = expression.getValue();
				
//				if(StringUtils.isBlank(v)) continue; //v1.9.8    in的值不允许为空             这样会有安全隐患, 少了一个条件,会更改很多数据.
				
				isNeedAnd=adjustAnd(sqlBuffer,isNeedAnd);
				sqlBuffer.append(columnName);
//				sqlBuffer.append(" ");
//				sqlBuffer.append(expression.getOpType());
				if(HoneyUtil.isSqlKeyWordUpper()) sqlBuffer.append(expression.getOpType().toUpperCase());
				else sqlBuffer.append(expression.getOpType());
				
	            processIn(sqlBuffer, list, v); //2.4.0

				isNeedAnd = true;
				continue;
			} else if (Op.like.getOperator().equalsIgnoreCase(opType) || Op.notLike.getOperator().equalsIgnoreCase(opType)) {
//				else if (opType == Op.like  || opType == Op.notLike) {
//				adjustAnd(sqlBuffer);
				isNeedAnd=adjustAnd(sqlBuffer,isNeedAnd);

				sqlBuffer.append(columnName);
//				sqlBuffer.append(expression.getOpType());
				if(HoneyUtil.isSqlKeyWordUpper()) sqlBuffer.append(expression.getOpType().toUpperCase());
				else sqlBuffer.append(expression.getOpType());
				sqlBuffer.append("?");

				String v = (String) expression.getValue();
                v=processLike(expression.getOp(), v); //V2.4.0
                
				addValeToPvList(list, v);

				isNeedAnd = true;
				continue;
			} else if (" between ".equalsIgnoreCase(opType) || " not between ".equalsIgnoreCase(opType)) {

				isNeedAnd=adjustAnd(sqlBuffer,isNeedAnd);

				sqlBuffer.append(columnName);
				sqlBuffer.append(opType);
				sqlBuffer.append("?");
				sqlBuffer.append(" "+K.and+" ");
				sqlBuffer.append("?");

				addValeToPvList(list, expression.getValue());
				addValeToPvList(list, expression.getValue2());

				isNeedAnd = true;
				continue;

			} else if (GROUP_BY.equalsIgnoreCase(opType)) {
				if (SuidType.SELECT != conditionImpl.getSuidType()) {
					throw new BeeErrorGrammarException("BeeErrorGrammarException: "+conditionImpl.getSuidType() + " do not support 'group by' !");
				}

				sqlBuffer.append(expression.getValue());//group by或者,
				sqlBuffer.append(columnName);

				continue;
			} else if (HAVING.equalsIgnoreCase(opType)) {
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
						sqlBuffer.append(columnName);
					}

					sqlBuffer.append(")");
					sqlBuffer.append(expression.getValue4()); //Op
					sqlBuffer.append("?");

					addValeToPvList(list, expression.getValue2());
				}

				continue;
			}else if ("orderBy".equalsIgnoreCase(opType)) {

				if (SuidType.SELECT != conditionImpl.getSuidType()) {
					throw new BeeErrorGrammarException(conditionImpl.getSuidType() + " do not support 'order by' !");
				}

				sqlBuffer.append(expression.getValue());//order by或者,
				if (4 == expression.getOpNum()) { //order by max(total)
					sqlBuffer.append(FunAndOrderTypeMap.transfer(expression.getValue3().toString()));
					sqlBuffer.append("(");
					sqlBuffer.append(columnName);
					sqlBuffer.append(")");
				} else {
					sqlBuffer.append(columnName);
				}

				if (3 == expression.getOpNum() || 4 == expression.getOpNum()) { //指定 desc,asc
					sqlBuffer.append(ONE_SPACE);
					sqlBuffer.append(FunAndOrderTypeMap.transfer(expression.getValue2().toString()));
					
//					//V1.17
//					//SqlServer 2012版之前的复杂分页语法需要判断
//					if(!orderByIdDescInSqlServer && start>1 && HoneyUtil.isSqlServer()) {
//						pkName="";
//						try {
//							entityClass.getDeclaredField("id");
//							pkName="id";
//						} catch (NoSuchFieldException e) {
//							pkName = HoneyUtil.getPkFieldNameByClass(entityClass).split(",")[0]; //有多个,只取第一个
//						}
//						
//						String pkColumnName=_toColumnName(pkName,useSubTableNames,entityClass);
//						// 1判断是否是主键  // 2判断是否是DESC
//						if(pkColumnName.equalsIgnoreCase(columnName)) {
//							if("desc".equalsIgnoreCase(expression.getValue2().toString())) {
//								//需要调整内部分页排序
//								orderByIdDescInSqlServer=true;
//							}
//						}
//					}
					
				}
				continue;
			}//end orderBy

			if (expression.getOpNum() == -2) { // (
//				adjustAnd(sqlBuffer);
				isNeedAnd=adjustAnd(sqlBuffer,isNeedAnd);
				sqlBuffer.append(expression.getValue());
				continue;
			}
			if (expression.getOpNum() == -1) {// )
				sqlBuffer.append(expression.getValue());
				isNeedAnd = true;
				continue;

			} else if (expression.getOpNum() == 1) { // or || and operation, 还有:  not (2.1.10) 
				if ("!".equals(expression.getValue())) { //V2.1.10
					isNeedAnd=adjustAnd(sqlBuffer,isNeedAnd);
					sqlBuffer.append(expression.getValue());
				}else {
					sqlBuffer.append(" ");
					sqlBuffer.append(expression.getValue());
					sqlBuffer.append(" ");
				}
				isNeedAnd = false;
				continue;
			}
			isNeedAnd=adjustAnd(sqlBuffer,isNeedAnd);

			sqlBuffer.append(columnName);  

			if (expression.getValue() == null) {
				if("=".equals(expression.getOpType())){
					sqlBuffer.append(" "+K.isNull);
				}else{
					sqlBuffer.append(" "+K.isNotNull);
					if(! "!=".equals(expression.getOpType())) {
						String fieldName=columnName;
						Logger.warn(fieldName+expression.getOpType()+"null transfer to : " +fieldName+" "+K.isNotNull);
					}
				}
			} else {
				if (expression.getOpNum() == -3) { //eg:field1=field2   could not use for having in mysql 
					sqlBuffer.append(expression.getOpType());
					sqlBuffer.append(expression.getValue());
				} else {
					sqlBuffer.append(expression.getOpType());
					if (expression.getValue().getClass() == TO_DATE.class) { //2.4.0
						
						TO_DATE to_date = (TO_DATE) expression.getValue();
						String formatter = to_date.getFormatter();
						if (NameCheckUtil.isContainCommentChar(formatter)) {
							throw new BeeIllegalSQLException("formatter :" + formatter + " , have sql comment character");
						}
						if(! HoneyUtil.isOracle()) { 
							Logger.warn("Make sure the Database support TO_DATE() function!");
						}
						
						sqlBuffer.append("TO_DATE(?, '" + formatter + "')");

						addValeToPvList(list, to_date.getDatetimeValue());
						
					} else {
						sqlBuffer.append("?");
						addValeToPvList(list, expression.getValue());
					}
				}
			}
			isNeedAnd = true;
		} //end expList for 

		//>>>>>>>>>>>>>>>>>>>paging start
		if (SuidType.SELECT == conditionImpl.getSuidType()) {
			if (! OneTimeParameter.isTrue(StringConst.Select_Fun)) {
				Integer size = conditionImpl.getSize();
				String sql = "";
				if (start != null && start!=-1 && size != null) {
					HoneyUtil.regPagePlaceholder();
					
					// V1.17 sql server paging
					Map<String, String> orderByMap = conditionImpl.getOrderBy();
					adjustSqlServerPagingIfNeed(sqlBuffer, orderByMap, start, entityClass, useSubTableNames);
					
					sql = getDbFeature().toPageSql(sqlBuffer.toString(), start, size);
					ShardingReg.regShadingPage(sqlBuffer.toString(), sql, start, size);//2.0
//			        sqlBuffer=new StringBuffer(sql); //new 之后不是原来的sqlBuffer,不能带回去.
					sqlBuffer.delete(0, sqlBuffer.length());
					sqlBuffer.append(sql);
					HoneyUtil.setPageNum(list);
					
				} else if (size != null) {
					HoneyUtil.regPagePlaceholder();
					
					// V1.17 sql server paging
					Map<String, String> orderByMap = conditionImpl.getOrderBy();
					adjustSqlServerPagingIfNeed(sqlBuffer, orderByMap, 0, entityClass, useSubTableNames); //start=0,只用于2012的offset语法

					sql = getDbFeature().toPageSql(sqlBuffer.toString(), size);
					ShardingReg.regShadingPage(sqlBuffer.toString(), sql, null, size); //2.0
					sqlBuffer.delete(0, sqlBuffer.length());
					sqlBuffer.append(sql);
					HoneyUtil.setPageNum(list);
				}
			}
			
			//2.0 reg sort
//			private Map<String,String> orderByMap=new LinkedHashMap<>();
			ShardingReg.regShardingSort(conditionImpl.getOrderBy());
			
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
	
	public static List<PreparedValue> processIn(Object v) {
		List<PreparedValue> inList =new ArrayList<>();
		if (List.class.isAssignableFrom(v.getClass())
				|| Set.class.isAssignableFrom(v.getClass())) { // List,Set
			Collection<?> c = (Collection<?>) v;
//			len = c.size();
			for (Object e : c) {
				setPreValue(inList, e);
			}
//		} else if (HoneyUtil.isNumberArray(v.getClass())) { // Number Array
		} else if (HoneyUtil.isNumberArray(v)) { // Number Array
			Number n[] = (Number[]) v;
//			len = n.length;
			for (Number number : n) {
				setPreValue(inList, number);
			}
//		} else if (String.class.equals(v.getClass())) { // String 逗号(,)为分隔符
		} else if (v instanceof String) { // String 逗号(,)为分隔符
			Object values[] = v.toString().trim().split(",");
//			len = values.length;
			for (Object e : values) {
				setPreValue(inList, e);
			}
		} else { // other one elements
			setPreValue(inList, v);
		}
		
		return inList;
	}
	
	private static void checkLikeEmptyException(String value) {
		if ("".equals(value))
			throw new BeeIllegalSQLException("Like has SQL injection risk! the value can not be empty string!");
	}
	
	private static void setPreValue(List<PreparedValue> list, Object value) {
		addValeToPvList(list, value);
	}
	
	
	// V1.17 for Sql Server,分页需要
	private static void adjustSqlServerPagingIfNeed(StringBuffer sqlBuffer,
			Map<String, String> orderByMap, Integer start, Class entityClass, String useSubTableNames[]) {
		
		if (!HoneyUtil.isSqlServer()) return ;
		
		SqlServerPagingStruct struct=new SqlServerPagingStruct();
		
		boolean needAdjust = false;
		boolean justChangePk=false;
		String pkName = "id";
		int majorVersion=HoneyConfig.getHoneyConfig().getDatabaseMajorVersion();
		// 要是参数没有condition,或condition为null,则使用默认排序.
		if (HoneyUtil.isSqlServer()) {
			if (orderByMap.size() > 0) { // 2012版之前的复杂分页语法需要判断. 之后的语法有order by即可.
				struct.setHasOrderBy(true);
//				orderByMap有值时,offset语法,只需要将默认order by id删除.
				if (majorVersion >= 11) {
					needAdjust = true;
				}else if(start > 1) {//// 2012版之前的复杂分页语法,两个参数,要是有主键倒序,则要调整
					String order = orderByMap.get("id");
					if (order != null) {
						pkName = "id";
						if ("desc".equals(order)) {
							needAdjust = true;
							struct.setOrderType(OrderType.DESC);
						}
					}

					if (!needAdjust) {// 测试名称不叫id的主键
						String pkName0 = HoneyUtil.getPkFieldNameByClass(entityClass);
						if (!"".equals(pkName0)) {
							pkName = pkName0.split(",")[0]; // 有多个,只取第一个
							order = orderByMap.get(pkName);
							if (order != null) {
								if ("desc".equals(order)) {
									needAdjust = true;
									struct.setOrderType(OrderType.DESC);
								}else {
									justChangePk=true;    //只要更改主键名
									struct.setJustChangeOrderColumn(true);
								}
							}
						}
					}
				}
			}else {//检测是否要更改主键名
				String pkName0 = HoneyUtil.getPkFieldNameByClass(entityClass);
				if (!"".equals(pkName0)) {
					pkName = pkName0.split(",")[0]; // 有多个,只取第一个
					justChangePk=true;
					struct.setJustChangeOrderColumn(true);
				}
			}
		}
		
		pkName=_toColumnName(pkName,useSubTableNames,entityClass);
		if(pkName.contains(".")) {
			justChangePk=true;
			struct.setJustChangeOrderColumn(true);
		}
		
		struct.setOrderColumn(pkName);
		//保存struct
		HoneyContext.setSqlServerPagingStruct(sqlBuffer.toString(), struct); //作为key的sql不是最终sql;因此处理后,一般就要先分页
	}
	
	static String processSelectField(String columnNames, Condition condition) {
		return processSelectField(columnNames, condition, null);
	}
	
	static String processSelectField(String columnNames, Condition condition,Map<String,String> subDulFieldMap) {
		
		if(condition==null) return null;

		ConditionImpl conditionImpl = (ConditionImpl) condition;
		if (SuidType.SELECT != conditionImpl.getSuidType()) {
			throw new BeeErrorGrammarException(conditionImpl.getSuidType() + " do not support specifying partial fields by method selectField(String) !");
		}
		String selectField[] = conditionImpl.getSelectField();

		if (selectField == null) return null;

		return HoneyUtil.checkAndProcessSelectFieldViaString(columnNames, subDulFieldMap, selectField);
	}
	
	public static String processFunction(String columnNames,Condition condition) {
//		if(condition==null) return null;
		
		boolean get_FunStructForSharding=OneTimeParameter.isTrue(StringConst.Get_GroupFunStruct);  //V2.0

		ConditionImpl conditionImpl = (ConditionImpl) condition;
		List<FunExpress> funExpList=conditionImpl.getFunExpList();
		String columnName;
		String funStr="";
		boolean isFirst=true;
		String alias;
		
		int size=funExpList.size();
//		FunStruct funStructs[]=null;
		List<FunStruct> funStructs=null;
		
		String funUseName="";
		
		get_FunStructForSharding = get_FunStructForSharding && size > 0 && ShardingUtil.hadSharding();
		
//		String sumStr="";
//		String countStr="";
		
		if(get_FunStructForSharding) {
			funStructs=new ArrayList<>(size);	
//			sumStr="_sum_";
//			countStr="_count_";
//			if (HoneyUtil.isSqlKeyWordUpper()) {
//				sumStr=sumStr.toUpperCase();
//				countStr=countStr.toUpperCase();
//			}
				
		}
		
		boolean hasAvg=false;
//		int adjust=0;
		for (int i = 0; i < funExpList.size(); i++) {
			
			if("*".equals(funExpList.get(i).getField())) {
				columnName="*";
			}else { //TODO //不校验字段
//				//聚合函数,支持复合写法,eg:"DISTINCT(school_id)", 不用检测
				columnName = HoneyUtil.checkAndProcessSelectFieldViaString(columnNames, null, false,funExpList.get(i).getField());
				
			}
			if(isFirst) {
				isFirst=false;
			}else {
				funStr+=",";
			}
//			funStr+=funExpList.get(i).getFunctionType().getName()+"("+columnName+")"; // funType要能转大小写风格
//			String functionTypeName=funExpList.get(i).getFunctionType().getName();
			String functionTypeName=funExpList.get(i).getFunctionType();
			functionTypeName=FunAndOrderTypeMap.transfer(functionTypeName);
			
			funStr += functionTypeName + "(" + columnName + ")";
			
			alias=funExpList.get(i).getAlias();
			if(StringUtils.isNotBlank(alias)) {
				funStr+=" "+K.as+" "+alias;
				funUseName=alias;
			}else {
				funUseName=columnName;
			}
			
			if (get_FunStructForSharding) { //sharding
//				funStructs[i+adjust] = new FunStruct(funUseName, functionTypeName);
				funStructs.add(new FunStruct(funUseName, functionTypeName));
				if(!hasAvg && FunctionType.AVG.getName().equalsIgnoreCase(functionTypeName)) {
					hasAvg=true;
//					adjust++;
//					funStructs[i+adjust] = new FunStruct(funUseName+"_sum_", FunctionType.SUM.getName());
					funStructs.add(new FunStruct(funUseName+"_sum_", FunctionType.SUM.getName()));
//					adjust++;
//					funStructs[i+adjust] = new FunStruct(funUseName+"_count_", FunctionType.COUNT.getName());
					funStructs.add(new FunStruct(funUseName+"_count_", FunctionType.COUNT.getName()));
					
					funStr +=", sum("+columnName+") "+K.as+" "+funUseName+"_sum_ , count("+columnName+") "+K.as+" "+funUseName+"_count_ "; //改写
				}
			}
		}
		
		if (get_FunStructForSharding) {
			GroupFunStruct struct=new GroupFunStruct();
			struct.setFunStructs(funStructs);
			struct.setHasAvg(hasAvg);
			OneTimeParameter.setAttribute(StringConst.Return_GroupFunStruct, struct);
		}
		
		return funStr;
	}
	
	public static void processOnExpression(Condition condition, MoreTableStruct moreTableStruct[],
			List<PreparedValue> list) {
		Class entityClass = (Class) OneTimeParameter.getAttribute(StringConst.Column_EC);
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
			onExpBuffer.append(_toColumnName(exp.getFieldName(),entityClass));
			onExpBuffer.append(K.space);
			onExpBuffer.append(exp.opType);
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
	
	
	private static String _toColumnName(String fieldName,Class entityClass) {
		return NameTranslateHandle.toColumnName(fieldName,entityClass);
	}
	
	private static String _toColumnName(String fieldName,String useSubTableNames[],Class entityClass) {
		if(StringUtils.isBlank(fieldName)) return fieldName;
		if(!fieldName.contains(",")) return _toColumnName0(fieldName, useSubTableNames,entityClass);
		
		String str[]=fieldName.split(",");
		String newFields="";
		int len=str.length;
		for (int i = 0; i < len; i++) {
			newFields+=_toColumnName0(str[i],useSubTableNames,entityClass);
			if(i!=len-1) newFields+=",";
		}
		return newFields;
		
	}
			
	private static String _toColumnName0(String fieldName,String useSubTableNames[],Class entityClass) {
		
		if(useSubTableNames==null) return NameTranslateHandle.toColumnName(fieldName,entityClass);   //one table type
		
		String t_fieldName="";
		String t_tableName="";
		String t_tableName_dot;
		String find_tableName="";
		int index=fieldName.indexOf('.');
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
			
			return find_tableName+"."+NameTranslateHandle.toColumnName(t_fieldName,entityClass);
		}else {
			fieldName=useSubTableNames[2]+"."+fieldName;
		}
		return NameTranslateHandle.toColumnName(fieldName,entityClass);
	}

	private static boolean adjustAnd(StringBuffer sqlBuffer,boolean isNeedAnd) {
		if (isNeedAnd) {
			sqlBuffer.append(" "+K.and+" ");
			isNeedAnd = false;
		}
		return isNeedAnd;
	}
	
	public static Integer getPageSize(Condition condition) {
		if(condition==null) return null;
		ConditionImpl conditionImpl = (ConditionImpl) condition;
		return conditionImpl.getSize();
	}
	
	public static void processIn(StringBuffer sqlBuffer, List<PreparedValue> list, Object v) {

		sqlBuffer.append(" (");
		sqlBuffer.append("?");
		int len = 1;
		boolean needSetNull = false;
		if (v == null) {
			needSetNull = true;
		} else {
			List<PreparedValue> inList = processIn(v);
			len = inList.size();
			if (len > 0)
				list.addAll(inList);
			else if (len == 0) needSetNull = true;
		}

		if (needSetNull) {
			PreparedValue p = new PreparedValue();
			p.setValue(null);
			p.setType(Object.class.getName());
			list.add(p);
		}

		for (int i = 1; i < len; i++) { // start 1
			sqlBuffer.append(",?");
		}

		sqlBuffer.append(")");

	}
	
	public static String processLike(Op op, String v) {
		if (v != null) {
//			Op op = expression.getOp();
			if (Op.likeLeft == op) {
				checkLikeEmptyException(v);
				v = "%" + StringUtils.escapeLike(v);
			} else if (Op.likeRight == op) {
				checkLikeEmptyException(v);
				v = StringUtils.escapeLike(v) + "%";
			} else if (Op.likeLeftRight == op) {
				checkLikeEmptyException(v);
				v = "%" + StringUtils.escapeLike(v) + "%";
			} else { // Op.like
				if (StringUtils.justLikeChar(v)) {
//					throw new BeeIllegalSQLException("Like has SQL injection risk! " + columnName + " like '" + v+"'");
					throw new BeeIllegalSQLException("Like has SQL injection risk! "+ " like '" + v+"'");
				}
			}
		} else {
          Logger.warn("the parameter value in like is null !",new BeeIllegalSQLException());
		}
		
		return v;
	}
	
	
	static WhereConditionWrap processWhereCondition(Condition condition) {
		return processWhereCondition(condition, true, null);
	}

	static WhereConditionWrap processWhereCondition(Condition condition, boolean firstWhere,
			String useSubTableNames[]) {
		
		Class entityClass = (Class) OneTimeParameter.getAttribute(StringConst.Column_EC); // 要消费了
		if(condition==null) return null;
		
		StringBuffer sqlBuffer=new StringBuffer();
		List<PreparedValue> list=new Vector<>();
		

//		没有初始化路由，是否有影响？
		//condition里要设置操作类型
		
		boolean isNeedAnd = true;
		boolean isFirstWhere=firstWhere; //v1.7.2 return for control whether allow to delete/update whole records in one table

		ConditionImpl conditionImpl = (ConditionImpl) condition;
		List<Expression> expList = conditionImpl.getExpList();
		
		Integer start = conditionImpl.getStart();
		
		if (start!=null && SuidType.SELECT != conditionImpl.getSuidType()) {
			throw new BeeErrorGrammarException(conditionImpl.getSuidType() + " do not support paging with start !");
		} 
		String columnName="";
		Expression expression = null;
		for (int j = 0; j < expList.size(); j++) {
			expression = expList.get(j);
			String opType = expression.getOpType();
			
			columnName=_toColumnName(expression.getFieldName(),useSubTableNames,entityClass);
			
			if ( GROUP_BY.equalsIgnoreCase(opType) || HAVING.equalsIgnoreCase(opType) ) {
				if (SuidType.SELECT != conditionImpl.getSuidType()) {
					throw new BeeErrorGrammarException(conditionImpl.getSuidType() + " do not support the opType: "+opType+"!");
				} 
			}
			//mysql's delete,update can use order by.

			if (firstWhere) {
				if ( GROUP_BY.equalsIgnoreCase(opType) || HAVING.equalsIgnoreCase(opType) || "orderBy".equalsIgnoreCase(opType)) {
					firstWhere = false;
				} else {
					sqlBuffer.append(" ").append(K.where).append(" ");
					firstWhere = false;
					isNeedAnd = false;
					isFirstWhere=false; //for return. where过滤条件
				}
			}
			if (Op.in.getOperator().equalsIgnoreCase(opType) || Op.notIn.getOperator().equalsIgnoreCase(opType)) {
				
				Object v = expression.getValue();
				
				isNeedAnd=adjustAnd(sqlBuffer,isNeedAnd);
				sqlBuffer.append(columnName);
				if(HoneyUtil.isSqlKeyWordUpper()) sqlBuffer.append(expression.getOpType().toUpperCase());
				else sqlBuffer.append(expression.getOpType());
				
				processIn(sqlBuffer, list, v);

				isNeedAnd = true;
				continue;
			} else if (Op.like.getOperator().equalsIgnoreCase(opType) || Op.notLike.getOperator().equalsIgnoreCase(opType)) {
				isNeedAnd=adjustAnd(sqlBuffer,isNeedAnd);

				sqlBuffer.append(columnName);
				if(HoneyUtil.isSqlKeyWordUpper()) sqlBuffer.append(expression.getOpType().toUpperCase());
				else sqlBuffer.append(expression.getOpType());
				sqlBuffer.append("?");

				String v = (String) expression.getValue();
				v=processLike(expression.getOp(), v);
				addValeToPvList(list, v);

				isNeedAnd = true;
				continue;
			} else if (" between ".equalsIgnoreCase(opType) || " not between ".equalsIgnoreCase(opType)) {

				isNeedAnd=adjustAnd(sqlBuffer,isNeedAnd);

				sqlBuffer.append(columnName);
				sqlBuffer.append(opType);
				sqlBuffer.append("?");
				sqlBuffer.append(" "+K.and+" ");
				sqlBuffer.append("?");

				addValeToPvList(list, expression.getValue());
				addValeToPvList(list, expression.getValue2());

				isNeedAnd = true;
				continue;

			} else if (GROUP_BY.equalsIgnoreCase(opType)) {
				if (SuidType.SELECT != conditionImpl.getSuidType()) {
					throw new BeeErrorGrammarException("BeeErrorGrammarException: "+conditionImpl.getSuidType() + " do not support 'group by' !");
				}

				sqlBuffer.append(expression.getValue());//group by或者,
				sqlBuffer.append(columnName);

				continue;
			} else if (HAVING.equalsIgnoreCase(opType)) {
				if (SuidType.SELECT != conditionImpl.getSuidType()) {
					throw new BeeErrorGrammarException(conditionImpl.getSuidType() + " do not support 'having' !");
				}

				if (5 == expression.getOpNum()) { //having(FunctionType.MIN, "field", Op.ge, 60)
					sqlBuffer.append(expression.getValue());//having 或者 and
					sqlBuffer.append(FunAndOrderTypeMap.transfer(expression.getValue3().toString())); //fun
					sqlBuffer.append("(");
					if (FunctionType.COUNT.getName().equals(expression.getValue3()) && "*".equals(expression.getFieldName().trim())) {
						sqlBuffer.append("*");
					} else {
						sqlBuffer.append(columnName);
					}

					sqlBuffer.append(")");
					sqlBuffer.append(expression.getValue4()); //Op
					sqlBuffer.append("?");

					addValeToPvList(list, expression.getValue2());
				}

				continue;
			}else if ("orderBy".equalsIgnoreCase(opType)) {

				if (SuidType.SELECT != conditionImpl.getSuidType()) {
					throw new BeeErrorGrammarException(conditionImpl.getSuidType() + " do not support 'order by' !");
				}

				sqlBuffer.append(expression.getValue());//order by或者,
				if (4 == expression.getOpNum()) { //order by max(total)
					sqlBuffer.append(FunAndOrderTypeMap.transfer(expression.getValue3().toString()));
					sqlBuffer.append("(");
					sqlBuffer.append(columnName);
					sqlBuffer.append(")");
				} else {
					sqlBuffer.append(columnName);
				}

				if (3 == expression.getOpNum() || 4 == expression.getOpNum()) { //指定 desc,asc
					sqlBuffer.append(ONE_SPACE);
					sqlBuffer.append(FunAndOrderTypeMap.transfer(expression.getValue2().toString()));
				}
				continue;
			}//end orderBy

			if (expression.getOpNum() == -2) { // (
//				adjustAnd(sqlBuffer);
				isNeedAnd=adjustAnd(sqlBuffer,isNeedAnd);
				sqlBuffer.append(expression.getValue());
				continue;
			}
			
			if (expression.getOpNum() == -1) {// )
				sqlBuffer.append(expression.getValue());
				isNeedAnd = true;
				continue;

			} else if (expression.getOpNum() == 1) { // or || and operation, 还有:  not (2.1.10) 
				if ("!".equals(expression.getValue())) { //V2.1.10
					isNeedAnd=adjustAnd(sqlBuffer,isNeedAnd);
					sqlBuffer.append(expression.getValue());
				}else {
					sqlBuffer.append(" ");
					sqlBuffer.append(expression.getValue());
					sqlBuffer.append(" ");
				}
				isNeedAnd = false;
				continue;
			}
			isNeedAnd=adjustAnd(sqlBuffer,isNeedAnd);

			sqlBuffer.append(columnName);  

			if (expression.getValue() == null) {
				if("=".equals(expression.getOpType())){
					sqlBuffer.append(" "+K.isNull);
				}else{
					sqlBuffer.append(" "+K.isNotNull);
					if(! "!=".equals(expression.getOpType())) {
						String fieldName=columnName;
						Logger.warn(fieldName+expression.getOpType()+"null transfer to : " +fieldName+" "+K.isNotNull);
					}
				}
			} else {
				if (expression.getOpNum() == -3) { //eg:field1=field2   could not use for having in mysql 
					sqlBuffer.append(expression.getOpType());
					sqlBuffer.append(expression.getValue());
				} else {
					sqlBuffer.append(expression.getOpType());
					if (expression.getValue().getClass() == TO_DATE.class) { //2.4.0
						
						TO_DATE to_date = (TO_DATE) expression.getValue();
						String formatter = to_date.getFormatter();
						if (NameCheckUtil.isContainCommentChar(formatter)) {
							throw new BeeIllegalSQLException("formatter :" + formatter + " , have sql comment character");
						}
						if(! HoneyUtil.isOracle()) { 
							Logger.warn("Make sure the Database support TO_DATE() function!");
						}
						
						sqlBuffer.append("TO_DATE(?, '" + formatter + "')");

						addValeToPvList(list, to_date.getDatetimeValue());
						
					} else {
						sqlBuffer.append("?");
						addValeToPvList(list, expression.getValue());
					}
				}
			}
			isNeedAnd = true;
		} //end expList for 
		
		return new WhereConditionWrap(sqlBuffer, list, isFirstWhere);
	}
	
	
	private static void addValeToPvList(List<PreparedValue> list, Object value) {
		PreparedValue preparedValue = new PreparedValue();
		preparedValue.setValue(value);
		if (value == null)
			preparedValue.setType(Object.class.getName());
		else
			preparedValue.setType(value.getClass().getName());
		list.add(preparedValue);
	}
	
	
//	public static void main(String[] args) {
//		Condition condition=BeeFactory.getHoneyFactory().getCondition();
//		condition.op("abc", Op.eq, 1);
//		condition.op("inField", Op.in, 2);
//		
////		WhereConditionWrap wrap=processWhereCondition(condition, true, null);
//		WhereConditionWrap wrap=processWhereCondition(condition);
//		
//		System.out.println(wrap.getSqlBuffer().toString());  // where abc=? and in_field in (?)
//		
//	}
	
}

class ConditionWrap{
	
	private StringBuffer sqlBuffer;
	private List<?> pvList;
	private boolean isFirst; //where or updateSet
	
	public ConditionWrap() {}
	
	public ConditionWrap(StringBuffer sqlBuffer, List<?> pvList, boolean isFirst) {
		super();
		this.sqlBuffer = sqlBuffer;
		this.pvList = pvList;
		this.isFirst = isFirst;
	}



	public StringBuffer getSqlBuffer() {
		return sqlBuffer;
	}

	public void setSqlBuffer(StringBuffer sqlBuffer) {
		this.sqlBuffer = sqlBuffer;
	}

	public List<?> getPvList() {
		return pvList;
	}

	public void setPvList(List<?> pvList) {
		this.pvList = pvList;
	}

	public boolean isFirst() {
		return isFirst;
	}

	public void setFirst(boolean isFirst) {
		this.isFirst = isFirst;
	}
}

class WhereConditionWrap extends ConditionWrap {
	public WhereConditionWrap(StringBuffer sqlBuffer, List<?> pvList, boolean isFirst) {
		super(sqlBuffer, pvList, isFirst);
	}
}

class UpdateSetConditionWrap extends ConditionWrap {
	public UpdateSetConditionWrap(StringBuffer sqlBuffer, List<?> pvList, boolean isFirst) {
		super(sqlBuffer, pvList, isFirst);
	}
}
