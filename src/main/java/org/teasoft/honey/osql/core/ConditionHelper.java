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
import org.teasoft.bee.osql.TO_DATE;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.osql.exception.BeeErrorGrammarException;
import org.teasoft.bee.osql.exception.BeeIllegalSQLException;
import org.teasoft.bee.sharding.FunStruct;
import org.teasoft.bee.sharding.GroupFunStruct;
import org.teasoft.honey.logging.Logger;
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

	private ConditionHelper() {}

	private static DbFeature getDbFeature() {
		return BeeFactory.getHoneyFactory().getDbFeature();
	}

	// ForUpdate
	static boolean processConditionForUpdateSet(StringBuffer sqlBuffer, List<PreparedValue> list, Condition condition) {
		return processUpdateSetCondition(sqlBuffer, list, condition, true).isFirst(); // just return isFirst
	}

	// 2.4.0
	static UpdateSetConditionWrap processUpdateSetCondition(Condition condition) {
		return processUpdateSetCondition(new StringBuffer(), new ArrayList<PreparedValue>(), condition, true);
	}

	static UpdateSetConditionWrap processUpdateSetCondition(StringBuffer sqlBuffer, List<PreparedValue> list,
			Condition condition, boolean firstSet) {

		Class entityClass = (Class) OneTimeParameter.getAttribute(StringConst.Column_EC);
		if (condition == null) return null;

		ConditionImpl conditionImpl = (ConditionImpl) condition;
		List<Expression> updateSetList = conditionImpl.getUpdateExpList();

		if (updateSetList != null && updateSetList.size() > 0) {
			if (SuidType.UPDATE != conditionImpl.getSuidType()) {
				throw new BeeErrorGrammarException(
						conditionImpl.getSuidType() + " do not support the method set ,setAdd or setMultiply!");
			}
		}

		OpType opType;
		String columnName;
		Object value;

		for (Expression expression : updateSetList) {
			opType = expression.getOpType();
			value = expression.getValue();

			if (firstSet) {
				firstSet = false;
			} else {
				sqlBuffer.append(",");
			}

			columnName = _toColumnName(expression.getFieldName(), null, entityClass);
			sqlBuffer.append(columnName);
			sqlBuffer.append("=");
			if (opType == OpType.SET) {
				sqlBuffer.append("?");
				addValeToPvList(list, value);

			} else if (opType == OpType.SET_ADD) {// salary = salary + 1000
				sqlBuffer.append(columnName);
				sqlBuffer.append(" + ");
				sqlBuffer.append("?");
				addValeToPvList(list, value);

			} else if (opType == OpType.SET_MULTIPLY) {// salary = salary * 1.2
				sqlBuffer.append(columnName);
				sqlBuffer.append(" * ");
				sqlBuffer.append("?");
				addValeToPvList(list, value);

			} else if (opType == OpType.SET_ADD_FIELD) {// salary = salary + bonus;
				sqlBuffer.append(columnName);
				sqlBuffer.append(" + ");
				sqlBuffer.append(value); // another field

			} else if (opType == OpType.SET_MULTIPLY_FIELD) {// salary = salary * rate;
				sqlBuffer.append(columnName);
				sqlBuffer.append(" * ");
				sqlBuffer.append(value); // another field

			} else if (opType == OpType.SET_WITH_FIELD) { // field1 = field2
				sqlBuffer.append(value); // another field
			}

//			update orders set total=total+0.5;
//			mysql is ok. as below:
//			update orders set total=total+?   [values]: -0.1  
		}

		return new UpdateSetConditionWrap(sqlBuffer, list, firstSet);
	}

	static boolean processCondition(StringBuffer sqlBuffer, List<PreparedValue> list, Condition condition,
			boolean firstWhere) {
		return processCondition(sqlBuffer, list, condition, firstWhere, null);
	}

	// v1.7.2 add return value for delete/update control
	static boolean processCondition(StringBuffer sqlBuffer, List<PreparedValue> list, Condition condition,
			boolean firstWhere, String useSubTableNames[]) {

		Class entityClass = (Class) OneTimeParameter.getAttribute(StringConst.Column_EC);

		WhereConditionWrap wrap = processWhereCondition(condition, firstWhere, useSubTableNames);
		if (wrap == null) return firstWhere;

		sqlBuffer.append(wrap.getSqlBuffer());
		list.addAll((List) wrap.getPvList());
		boolean isFirstWhere = wrap.isFirst();

		ConditionImpl conditionImpl = (ConditionImpl) condition;
		Integer start = conditionImpl.getStart();

		// >>>>>>>>>>>>>>>>>>>paging start
		if (SuidType.SELECT == conditionImpl.getSuidType()) {
			if (!OneTimeParameter.isTrue(StringConst.Select_Fun)) {
				Integer size = conditionImpl.getSize();
				String sql = "";
				if (start != null && start != -1 && size != null) {
					HoneyUtil.regPagePlaceholder();

					// V1.17 sql server paging
					Map<String, String> orderByMap = conditionImpl.getOrderBy();
					adjustSqlServerPagingIfNeed(sqlBuffer, orderByMap, start, entityClass, useSubTableNames);

					sql = getDbFeature().toPageSql(sqlBuffer.toString(), start, size);
					ShardingReg.regShadingPage(sqlBuffer.toString(), sql, start, size);// 2.0
//			        sqlBuffer=new StringBuffer(sql); //new 之后不是原来的sqlBuffer,不能带回去.
					sqlBuffer.delete(0, sqlBuffer.length());
					sqlBuffer.append(sql);
					HoneyUtil.setPageNum(list);

				} else if (size != null) {
					HoneyUtil.regPagePlaceholder();

					// V1.17 sql server paging
					Map<String, String> orderByMap = conditionImpl.getOrderBy();
					adjustSqlServerPagingIfNeed(sqlBuffer, orderByMap, 0, entityClass, useSubTableNames); // start=0,只用于2012的offset语法

					sql = getDbFeature().toPageSql(sqlBuffer.toString(), size);
					ShardingReg.regShadingPage(sqlBuffer.toString(), sql, null, size); // 2.0
					sqlBuffer.delete(0, sqlBuffer.length());
					sqlBuffer.append(sql);
					HoneyUtil.setPageNum(list);
				}
			}

			// 2.0 reg sort
			ShardingReg.regShardingSort(conditionImpl.getOrderBy());
		}
		// >>>>>>>>>>>>>>>>>>>paging end

		// >>>>>>>>>>>>>>>>>>>forUpdate
		// 仅用于SQL的单个表select
		if (useSubTableNames == null && SuidType.SELECT == conditionImpl.getSuidType()) {

			Boolean isForUpdate = conditionImpl.getForUpdate();
			if (isForUpdate != null && isForUpdate.booleanValue()) {
				sqlBuffer.append(" " + K.forUpdate + " ");
			}
		}
		// >>>>>>>>>>>>>>>>>>>forUpdate

		// check
		if (SuidType.SELECT == conditionImpl.getSuidType()) {
			List<Expression> updateSetList = conditionImpl.getUpdateExpList();
			if (updateSetList != null && updateSetList.size() > 0) {
				Logger.warn(
						"Use Condition's set method(s) in SELECT type, but it just effect in UPDATE type! Involved field(s): "
								+ conditionImpl.getUpdatefields());
			}
		}

		return isFirstWhere;
	}

	public static List<PreparedValue> processIn(Object v) {
		List<PreparedValue> inList = new ArrayList<>();
		if (List.class.isAssignableFrom(v.getClass()) || Set.class.isAssignableFrom(v.getClass())) { // List,Set
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
	private static void adjustSqlServerPagingIfNeed(StringBuffer sqlBuffer, Map<String, String> orderByMap, Integer start,
			Class entityClass, String useSubTableNames[]) {

		if (!HoneyUtil.isSqlServer()) return;

		SqlServerPagingStruct struct = new SqlServerPagingStruct();

		boolean needAdjust = false;
		boolean justChangePk = false;
		String pkName = "id";
		int majorVersion = HoneyConfig.getHoneyConfig().getDatabaseMajorVersion();
		// 要是参数没有condition,或condition为null,则使用默认排序.
		if (HoneyUtil.isSqlServer()) {
			if (orderByMap.size() > 0) { // 2012版之前的复杂分页语法需要判断. 之后的语法有order by即可.
				struct.setHasOrderBy(true);
//				orderByMap有值时,offset语法,只需要将默认order by id删除.
				if (majorVersion >= 11) {
					needAdjust = true;
				} else if (start > 1) {//// 2012版之前的复杂分页语法,两个参数,要是有主键倒序,则要调整
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
								} else {
									justChangePk = true; // 只要更改主键名
									struct.setJustChangeOrderColumn(true);
								}
							}
						}
					}
				}
			} else {// 检测是否要更改主键名
				String pkName0 = HoneyUtil.getPkFieldNameByClass(entityClass);
				if (!"".equals(pkName0)) {
					pkName = pkName0.split(",")[0]; // 有多个,只取第一个
					justChangePk = true;
					struct.setJustChangeOrderColumn(true);
				}
			}
		}

		pkName = _toColumnName(pkName, useSubTableNames, entityClass);
		if (pkName.contains(".")) {
			justChangePk = true;
			struct.setJustChangeOrderColumn(true);
		}

		struct.setOrderColumn(pkName);
		// 保存struct
		HoneyContext.setSqlServerPagingStruct(sqlBuffer.toString(), struct); // 作为key的sql不是最终sql;因此处理后,一般就要先分页
	}

	static String processSelectField(String columnNames, Condition condition) {
		return processSelectField(columnNames, condition, null);
	}

	static String processSelectField(String columnNames, Condition condition, Map<String, String> subDulFieldMap) {

		if (condition == null) return null;

		ConditionImpl conditionImpl = (ConditionImpl) condition;
		if (SuidType.SELECT != conditionImpl.getSuidType()) {
			throw new BeeErrorGrammarException(conditionImpl.getSuidType()
					+ " do not support specifying partial fields by method selectField(String) !");
		}
		String selectField[] = conditionImpl.getSelectField();

		if (selectField == null) return null;

		return HoneyUtil.checkAndProcessSelectFieldViaString(columnNames, subDulFieldMap, selectField);
	}

	public static String processFunction(String columnNames, Condition condition) {
//		if(condition==null) return null;

		boolean get_FunStructForSharding = OneTimeParameter.isTrue(StringConst.Get_GroupFunStruct); // V2.0

		ConditionImpl conditionImpl = (ConditionImpl) condition;
		List<FunExpress> funExpList = conditionImpl.getFunExpList();
		String columnName;
		String funStr = "";
		boolean isFirst = true;
		String alias;

		int size = funExpList.size();
//		FunStruct funStructs[]=null;
		List<FunStruct> funStructs = null;

		String funUseName = "";

		get_FunStructForSharding = get_FunStructForSharding && size > 0 && ShardingUtil.hadSharding();

//		String sumStr="";
//		String countStr="";

		if (get_FunStructForSharding) {
			funStructs = new ArrayList<>(size);
//			sumStr="_sum_";
//			countStr="_count_";
//			if (HoneyUtil.isSqlKeyWordUpper()) {
//				sumStr=sumStr.toUpperCase();
//				countStr=countStr.toUpperCase();
//			}

		}

		boolean hasAvg = false;
//		int adjust=0;
		for (int i = 0; i < funExpList.size(); i++) {

			if ("*".equals(funExpList.get(i).getField())) {
				columnName = "*";
			} else { // todo //不校验字段
//				//聚合函数,支持复合写法,eg:"DISTINCT(school_id)", 不用检测
				columnName = HoneyUtil.checkAndProcessSelectFieldViaString(columnNames, null, false,
						funExpList.get(i).getField());
			}
			if (isFirst) {
				isFirst = false;
			} else {
				funStr += ",";
			}
//			funStr+=funExpList.get(i).getFunctionType().getName()+"("+columnName+")"; // funType要能转大小写风格
//			String functionTypeName=funExpList.get(i).getFunctionType().getName();
			String functionTypeName = funExpList.get(i).getFunctionType();
			functionTypeName = FunAndOrderTypeMap.transfer(functionTypeName);

			funStr += functionTypeName + "(" + columnName + ")";

			alias = funExpList.get(i).getAlias();
			if (StringUtils.isNotBlank(alias)) {
				funStr += " " + K.as + " " + alias;
				funUseName = alias;
			} else {
				funUseName = columnName;
			}

			if (get_FunStructForSharding) { // sharding
//				funStructs[i+adjust] = new FunStruct(funUseName, functionTypeName);
				funStructs.add(new FunStruct(funUseName, functionTypeName));
				if (!hasAvg && FunctionType.AVG.getName().equalsIgnoreCase(functionTypeName)) {
					hasAvg = true;
//					adjust++;
//					funStructs[i+adjust] = new FunStruct(funUseName+"_sum_", FunctionType.SUM.getName());
					funStructs.add(new FunStruct(funUseName + "_sum_", FunctionType.SUM.getName()));
//					adjust++;
//					funStructs[i+adjust] = new FunStruct(funUseName+"_count_", FunctionType.COUNT.getName());
					funStructs.add(new FunStruct(funUseName + "_count_", FunctionType.COUNT.getName()));

					funStr += ", sum(" + columnName + ") " + K.as + " " + funUseName + "_sum_ , count(" + columnName
							+ ") " + K.as + " " + funUseName + "_count_ "; // 改写
				}
			}
		}

		if (get_FunStructForSharding) {
			GroupFunStruct struct = new GroupFunStruct();
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

		List<PreparedValue> list2 = new ArrayList<>();

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
			onExpBuffer.append(_toColumnName(exp.getFieldName(), entityClass));
			onExpBuffer.append(K.space);
			onExpBuffer.append(exp.getOp().getOperator());
			onExpBuffer.append("?");

			if (moreTableStruct[0].joinTableNum == 2) {
				String fieldName = exp.getFieldName();
				if (fieldName.startsWith(moreTableStruct[2].tableName + ".")
						|| (moreTableStruct[2].hasSubAlias && fieldName.startsWith(moreTableStruct[2].subAlias + "."))) { // 第2个从表
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

	private static void addValeToList(List<PreparedValue> list, Expression exp) {
		PreparedValue preparedValue = new PreparedValue();
		preparedValue.setType(exp.getValue().getClass().getName());
		preparedValue.setValue(exp.getValue());
		list.add(preparedValue);
	}

	private static String _toColumnName(String fieldName, Class entityClass) {
		return NameTranslateHandle.toColumnName(fieldName, entityClass);
	}

	private static String _toColumnName(String fieldName, String useSubTableNames[], Class entityClass) {
		if (StringUtils.isBlank(fieldName)) return fieldName;
		if (!fieldName.contains(",")) return _toColumnName0(fieldName, useSubTableNames, entityClass);

		String str[] = fieldName.split(",");
		String newFields = "";
		int len = str.length;
		for (int i = 0; i < len; i++) {
			newFields += _toColumnName0(str[i], useSubTableNames, entityClass);
			if (i != len - 1) newFields += ",";
		}
		return newFields;

	}

	private static String _toColumnName0(String fieldName, String useSubTableNames[], Class entityClass) {

		if (useSubTableNames == null) return NameTranslateHandle.toColumnName(fieldName, entityClass); // one table type

		String t_fieldName = "";
		String t_tableName = "";
//		String t_tableName_dot;
		String find_tableName = "";
		int index = fieldName.indexOf('.');
		if (index > -1) {
			t_fieldName = fieldName.substring(index + 1);
			t_tableName = fieldName.substring(0, index);
//			t_tableName_dot=fieldName.substring(0,index+1);
			// check whether is useSubTableName
			if (useSubTableNames[0] != null && useSubTableNames[0].equals(t_tableName)) { // fixed bug 2.4.0
				find_tableName = t_tableName;
			} else if (useSubTableNames[1] != null && useSubTableNames[1].equals(t_tableName)) {// fixed bug 2.4.0
				find_tableName = t_tableName;
			} else {
				OneTimeParameter.setTrueForKey(StringConst.DoNotCheckAnnotation);// adjust for @Table
				find_tableName = NameTranslateHandle.toTableName(t_tableName);
			}

			return find_tableName + "." + NameTranslateHandle.toColumnName(t_fieldName, entityClass);
		} else {
			fieldName = useSubTableNames[2] + "." + fieldName;
		}
		return NameTranslateHandle.toColumnName(fieldName, entityClass);
	}

	private static boolean adjustAnd(StringBuffer sqlBuffer, boolean isNeedAnd) {
		if (isNeedAnd) {
			sqlBuffer.append(" " + K.and + " ");
			isNeedAnd = false;
		}
		return isNeedAnd;
	}

	public static Integer getPageSize(Condition condition) {
		if (condition == null) return null;
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
					throw new BeeIllegalSQLException("Like has SQL injection risk! " + " like '" + v + "'");
				}
			}
		} else {
			Logger.warn("the parameter value in like is null !", new BeeIllegalSQLException());
		}

		return v;
	}

	static WhereConditionWrap processWhereCondition(Condition condition) {
		return processWhereCondition(condition, true, null);
	}

	static WhereConditionWrap processWhereCondition(Condition condition, boolean firstWhere, String useSubTableNames[]) {

		Class entityClass = (Class) OneTimeParameter.getAttribute(StringConst.Column_EC); // 要消费了
		if (condition == null) return null;

		StringBuffer sqlBuffer = new StringBuffer();
		List<PreparedValue> list = new Vector<>();

//		没有初始化路由，是否有影响？
		// condition里要设置操作类型

		boolean isNeedAnd = true;
		boolean isFirstWhere = firstWhere; // v1.7.2 return for control whether allow to delete/update whole records in one table
		ConditionImpl conditionImpl = (ConditionImpl) condition;
		Integer start = conditionImpl.getStart();

		if (start != null && SuidType.SELECT != conditionImpl.getSuidType()) {
			throw new BeeErrorGrammarException(conditionImpl.getSuidType() + " do not support paging with start !");
		}

		List<Expression> expList = conditionImpl.getExpList();
		OpType opType;
		Op op;
		Object value;
		String columnName = "";
		String operator = null;
		for (Expression expression : expList) {
			opType = expression.getOpType();
			op = expression.getOp();
			value = expression.getValue();
			columnName = _toColumnName(expression.getFieldName(), useSubTableNames, entityClass);

			if (OpType.GROUP_BY == opType || OpType.HAVING == opType) {
				if (SuidType.SELECT != conditionImpl.getSuidType()) {
					throw new BeeErrorGrammarException(
							conditionImpl.getSuidType() + " do not support the opType: " + opType + "!");
				}
			}
			// mysql's delete,update can use order by.

			if (firstWhere) {
				if (OpType.GROUP_BY == opType || OpType.HAVING == opType || OpType.ORDER_BY2 == opType
						|| OpType.ORDER_BY3 == opType || OpType.ORDER_BY4 == opType) {
					firstWhere = false;
				} else {
					sqlBuffer.append(" ").append(K.where).append(" ");
					firstWhere = false;
					isNeedAnd = false;
					isFirstWhere = false; // for return. where过滤条件
				}
			}

			if (OpType.OP2 == opType) {
				if (expression.getValue() != null && expression.getValue().getClass() == TO_DATE.class) // 2.4.0
					opType = OpType.OP2_TO_DATE;
			}

			operator = null;
			if (op != null) {
				if (HoneyUtil.isSqlKeyWordUpper())
					operator = op.getOperator().toUpperCase();
				else
					operator = op.getOperator();
			}

			// begin process
			if (OpType.IN == opType) {
				isNeedAnd = adjustAnd(sqlBuffer, isNeedAnd);
				sqlBuffer.append(columnName);
				sqlBuffer.append(operator);
				processIn(sqlBuffer, list, value); // 2.4.0

				isNeedAnd = true;
			} else if (OpType.LIKE == opType) {
				isNeedAnd = adjustAnd(sqlBuffer, isNeedAnd);
				sqlBuffer.append(columnName);
				sqlBuffer.append(operator);
				sqlBuffer.append("?");

				String v = (String) value;
				v = processLike(expression.getOp(), v); // V2.4.0
				addValeToPvList(list, v);

				isNeedAnd = true;
			} else if (OpType.OP2 == opType) {
				if (value == null) {
					sqlBuffer.append(columnName);
					if ("=".equals(operator)) {
						sqlBuffer.append(" " + K.isNull);
					} else {
						sqlBuffer.append(" " + K.isNotNull);
						if (!"!=".equals(operator)) {
							Logger.warn(columnName + operator + "null transfer to : " + columnName + " " + K.isNotNull);
						}
					}
				} else {
					sqlBuffer.append(columnName);
					sqlBuffer.append(operator);
					sqlBuffer.append("?");
					addValeToPvList(list, value);
				}
				isNeedAnd = true;
			} else if (OpType.OP_WITH_FIELD == opType) { // eg:field1=field2 //this could not use for having in mysql
				sqlBuffer.append(columnName);
				sqlBuffer.append(operator);
				sqlBuffer.append(value);
				isNeedAnd = true;
			} else if (OpType.OP2_TO_DATE == opType) {
				sqlBuffer.append(columnName);
				sqlBuffer.append(operator);

				// eg:condition.op("mydatetime", Op.ge, new TO_DATE("2024-07-02", "YYYY-MM-DD"));
				// mydatetime >= TO_DATE('2024-07-02', 'YYYY-MM-DD')
				TO_DATE to_date = (TO_DATE) value;
				String formatter = to_date.getFormatter();
				if (NameCheckUtil.isContainCommentString(formatter)) {
					throw new BeeIllegalSQLException("formatter :" + formatter + " , have sql comment character");
				}
				if (!HoneyUtil.isOracle()) {
					Logger.warn("Make sure the Database support TO_DATE() function!");
				}

				sqlBuffer.append(K.to_date + "(?, '" + formatter + "')");
				addValeToPvList(list, to_date.getDatetimeValue());

				isNeedAnd = true;
			} else if (OpType.BETWEEN == opType || OpType.NOT_BETWEEN == opType) {
				isNeedAnd = adjustAnd(sqlBuffer, isNeedAnd);
				sqlBuffer.append(columnName);
				if (OpType.BETWEEN == opType)
					sqlBuffer.append(" " + K.between + " ");
				else
					sqlBuffer.append(" " + K.notBetween + " ");
				sqlBuffer.append("?");
				sqlBuffer.append(" " + K.and + " ");
				sqlBuffer.append("?");

				addValeToPvList(list, value);
				addValeToPvList(list, expression.getValue2());

				isNeedAnd = true;

			} else if (OpType.L_PARENTHESES == opType) { // (
				isNeedAnd = adjustAnd(sqlBuffer, isNeedAnd);
				sqlBuffer.append(value);
			} else if (OpType.R_PARENTHESES == opType) {// )
				sqlBuffer.append(value);
				isNeedAnd = true;
			} else if (OpType.ONE == opType) { // or, and, not (2.1.10)
				if ("!".equals(value)) { // V2.1.10
					isNeedAnd = adjustAnd(sqlBuffer, isNeedAnd);
					sqlBuffer.append(value);
				} else {
					sqlBuffer.append(" ");
					sqlBuffer.append(value);
					sqlBuffer.append(" ");
				}
				isNeedAnd = false;
			} else if (OpType.GROUP_BY == opType) {
				if (SuidType.SELECT != conditionImpl.getSuidType()) {
					throw new BeeErrorGrammarException(
							"BeeErrorGrammarException: " + conditionImpl.getSuidType() + " do not support 'group by' !");
				}

				sqlBuffer.append(value);// group by或者,
				sqlBuffer.append(columnName);

			} else if (OpType.HAVING == opType) {
				if (SuidType.SELECT != conditionImpl.getSuidType()) {
					throw new BeeErrorGrammarException(conditionImpl.getSuidType() + " do not support 'having' !");
				}
//					if (5 == expression.getOpNum()) { // having(FunctionType.MIN, "field", Op.ge, 60)
				sqlBuffer.append(value);// having 或者 and
				sqlBuffer.append(FunAndOrderTypeMap.transfer(expression.getValue3().toString())); // fun
				sqlBuffer.append("(");
				if (FunctionType.COUNT.getName().equals(expression.getValue3())
						&& "*".equals(expression.getFieldName().trim())) {
					sqlBuffer.append("*");
				} else {
					sqlBuffer.append(columnName);
				}

				sqlBuffer.append(")");
				sqlBuffer.append(expression.getValue4()); // Op
				sqlBuffer.append("?");

				addValeToPvList(list, expression.getValue2());
			} else if (OpType.ORDER_BY2 == opType || OpType.ORDER_BY3 == opType || OpType.ORDER_BY4 == opType) {

				if (SuidType.SELECT != conditionImpl.getSuidType()) {
					throw new BeeErrorGrammarException(conditionImpl.getSuidType() + " do not support 'order by' !");
				}

				sqlBuffer.append(value);// order by或者,
				if (OpType.ORDER_BY4 == opType) { // order by max(total)
					sqlBuffer.append(FunAndOrderTypeMap.transfer(expression.getValue3().toString()));
					sqlBuffer.append("(");
					sqlBuffer.append(columnName);
					sqlBuffer.append(")");
				} else {
					sqlBuffer.append(columnName);
				}

				if (OpType.ORDER_BY3 == opType || OpType.ORDER_BY4 == opType) { // 指定 desc,asc
					sqlBuffer.append(ONE_SPACE);
					sqlBuffer.append(FunAndOrderTypeMap.transfer(expression.getValue2().toString()));
				}
			} // end orderBy

		} // end expList for

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

	public static void main(String[] args) {
		WhereConditionWrap wrap =null;
		
		Condition condition=BeeFactory.getHoneyFactory().getCondition();
		condition.op("abc", Op.eq, 1);
		condition.op("inField", Op.in, 2);
		wrap = processWhereCondition(condition);
		System.out.println(wrap);
//		  where abc=? and in_field in (?)
		
		condition=BeeFactory.getHoneyFactory().getCondition();
		condition.op("abc", Op.eq, 1);
//		condition.op("inField", Op.in, new String[] { "1", "2" });
		condition.op("inField", Op.in, "1,2");//多个可以用逗号隔开. 
		wrap = processWhereCondition(condition);
		System.out.println(wrap);
//		where abc=? and in_field in (?,?)
		
		condition=BeeFactory.getHoneyFactory().getCondition();
		condition.op("abc", Op.eq, 1);
		condition.op("likeField", Op.likeLeft, "bee"); //自动在左边加%
		wrap = processWhereCondition(condition);
		System.out.println(wrap);
//		sql:  where abc=? and like_field like ?
//				pvList:  1  %bee  
		
		condition=BeeFactory.getHoneyFactory().getCondition();
		condition.op("abc", Op.eq, 1);
		condition.op("likeField", Op.likeLeft, "%"); //自动在左边加%
		wrap = processWhereCondition(condition);
		System.out.println(wrap);
		
		condition=BeeFactory.getHoneyFactory().getCondition();
		condition.op("abc", Op.eq, 1);
		condition.op("likeField", Op.like, "%"); 
		wrap = processWhereCondition(condition);
		System.out.println(wrap);
		
		condition=BeeFactory.getHoneyFactory().getCondition();
		condition.op("abc", Op.eq, 1);
		condition.between("betweenField", 10, 20);
		wrap = processWhereCondition(condition);
		System.out.println(wrap);
//		sql:  where abc=? and between_field between ? and ?
//				pvList:  1  10  20  
		
//		WhereConditionWrap wrap=processWhereCondition(condition, true, null);
//		wrap = processWhereCondition(condition);
		
		
//		System.out.println(wrap.getSqlBuffer().toString()); 
//		System.out.println(wrap.getPvList().toString()); 
		
	}

}

class ConditionWrap {

	private StringBuffer sqlBuffer;
	private List<?> pvList;
	private boolean isFirst; // where or updateSet

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
	
	public String toString() {
		StringBuffer sbu=new StringBuffer();
		sbu.append("sql: ").append(sqlBuffer.toString()).append("\n");
		sbu.append("pvList:  ");
		for (Object pv : pvList) {
			sbu.append( ((PreparedValue)pv).getValue().toString()).append("  ");
		}
		return sbu.toString();
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
