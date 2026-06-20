package org.teasoft.honey.osql.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.teasoft.bee.osql.FunctionType;
import org.teasoft.bee.osql.Op;
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
import org.teasoft.honey.osql.util.NameCheckUtil;
import org.teasoft.honey.sharding.ShardingReg;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.util.StringUtils;

public class ConditionHelper3 {

	private static final String ONE_SPACE = " ";

	private ConditionHelper3() {
	}

	private static DbFeature getDbFeature() {
		return BeeFactory.getHoneyFactory().getDbFeature();
	}

	//可以和1的合回? TODO
	static QueryConditionWrap processQueryCondition(Condition condition, boolean firstWhere,
			String mainTableAlias, Class<?> mainClass, Map<String, MoreTableStruct3> moreTableStructMap) {

//		Class entityClass = (Class) OneTimeParameter.getAttribute(StringConst.Column_EC); // 要消费了 V3多表，不用这个了。
		if (condition == null) return null;

		StringBuffer sqlBuffer = new StringBuffer();
		List<PreparedValue> list = new Vector<>();

//		没有初始化路由，是否有影响？
		// condition里要设置操作类型

		boolean isNeedAnd = true;
		boolean isFirstWhere = firstWhere; // v1.7.2 return for control whether allow to delete/update whole records in
											// one table
		ConditionImpl conditionImpl = (ConditionImpl) condition;
		Integer start = conditionImpl.getStart();
		Integer size = conditionImpl.getSize();

		if (start != null && SuidType.SELECT != conditionImpl.getSuidType()) {
			throw new BeeErrorGrammarException(conditionImpl.getSuidType() + " do not support paging with start !");
		}

		List<Expression> expList = conditionImpl.getExpList();
		OpType opType;
		Op op;
		Object value;
		String columnName = "";
		String fieldName = "";
		String operator = null;
//		int dotIndex;
//		String tablePart;
//		String namePart;
		for (Expression expression : expList) {
			opType = expression.getOpType();
			op = expression.getOp();
			value = expression.getValue();
//			columnName = _toColumnName(expression.getFieldName(), useSubTableNames, entityClass);

			fieldName = expression.getFieldName().trim();
			
			// MoreTableHelper.fieldName2ColumnName
			columnName = MoreTableHelper.fieldName2ColumnName(fieldName, mainTableAlias, mainClass, moreTableStructMap);

			// fieldName->columnName start
//			dotIndex = fieldName.indexOf('.');
//			if (dotIndex < 0) {
//				columnName = mainTableAlias + "." + _toColumnName(fieldName, mainClass);
//			} else {
//				tablePart = fieldName.substring(0, dotIndex);
//				namePart = fieldName.substring(dotIndex + 1);
//				if (tablePart.equals(mainTableAlias)) {
//					columnName = tablePart + "." + _toColumnName(namePart, mainClass);
//				} else {
//					MoreTableStruct3 moreTableStruct = moreTableStructMap.get(tablePart);
//					if (moreTableStruct == null) {
//						Logger.warn("Can not found the MoreTableStruct with " + tablePart
//								+ " , please check the Condition you use!");
//						continue;
//					}
//					columnName = tablePart + "." + _toColumnName(namePart, moreTableStruct.subClass);
//				}
//			}
//			// fieldName->columnName end

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
				isNeedAnd = adjustAnd(sqlBuffer, isNeedAnd); // fix bug V2.5.2.7
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
				isNeedAnd = adjustAnd(sqlBuffer, isNeedAnd);
				sqlBuffer.append(columnName);
				sqlBuffer.append(operator);
				sqlBuffer.append(value);
				isNeedAnd = true;
			} else if (OpType.OP2_TO_DATE == opType) {
				isNeedAnd = adjustAnd(sqlBuffer, isNeedAnd);
				sqlBuffer.append(columnName);
				sqlBuffer.append(operator);

				// eg:condition.op("mydatetime", Op.ge, new TO_DATE("2024-07-02",
				// "YYYY-MM-DD"));
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
							"BeeErrorGrammarException: " + conditionImpl.getSuidType()
									+ " do not support 'group by' !");
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

//		return new QueryConditionWrap(sqlBuffer, list, isFirstWhere, condition.getGroupByFields(),
//				condition.getOrderBy(), start, size, conditionImpl.getForUpdate());

		return new QueryConditionWrap(sqlBuffer, list, isFirstWhere, condition.getGroupByFields(),
				condition.getOrderBy());
	}
	
	static String processSelectField(String columnNames, Condition condition, Map<String, String> subDulFieldMap,
			String mainTableAlias, Class<?> mainClass, Map<String, MoreTableStruct3> moreTableStructMap) {

		if (condition == null) return null;

		ConditionImpl conditionImpl = (ConditionImpl) condition;
		if (SuidType.SELECT != conditionImpl.getSuidType()) {
			throw new BeeErrorGrammarException(conditionImpl.getSuidType()
					+ " do not support specifying partial fields by method selectField(String) !");
		}
		String selectField[] = conditionImpl.getSelectField();

		if (selectField == null) return null;
		
		String selectFields[];

		if (selectField.length == 1) { // 变长参数,只有一个时,才允许用逗号隔开
			selectFields = selectField[0].split(",");
		} else {
			selectFields = selectField;
		}
		StringUtils.trim(selectFields);
		
		// fieldName -> columnName
		for (int i = 0; i < selectFields.length; i++) {
			selectFields[i] = MoreTableHelper.fieldName2ColumnName(selectFields[i], mainTableAlias, mainClass, moreTableStructMap);
		}

		return HoneyUtil.checkAndProcessSelectFieldViaString(columnNames, subDulFieldMap, selectFields);
	}
	
	public static String processFunction(String columnNames, Condition condition, String mainTableAlias,
			Class<?> mainClass, Map<String, MoreTableStruct3> moreTableStructMap
			) {

		if (condition == null) return null;

		boolean get_FunStructForSharding = OneTimeParameter.isTrue(StringConst.Get_GroupFunStruct); // V2.0

		ConditionImpl conditionImpl = (ConditionImpl) condition;
		List<FunExpress> funExpList = conditionImpl.getFunExpList();
		String columnName;
		String funStr = "";
		boolean isFirst = true;
		String nameAlias;
		boolean isSQLite=HoneyUtil.isSQLite();

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
		
		boolean isConfuseDuplicateFieldDB = HoneyUtil.isConfuseDuplicateFieldDB();
		boolean hasAvg = false;
//		int adjust=0;
		for (int i = 0; i < funExpList.size(); i++) {

			if ("*".equals(funExpList.get(i).getField())) {
				columnName = "*";
			} else { // todo //不校验字段
				
				//3.0
				String fieldName = funExpList.get(i).getField();
				columnName = MoreTableHelper.fieldName2ColumnName(fieldName, mainTableAlias, mainClass, moreTableStructMap);

//				//聚合函数,支持复合写法,eg:"DISTINCT(school_id)", 不用检测
				columnName = HoneyUtil.checkAndProcessSelectFieldViaString(columnNames, null, false, columnName);
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

			nameAlias = funExpList.get(i).getAlias();
			if (StringUtils.isNotBlank(nameAlias)) {
				funUseName = nameAlias;
				String newAlias = nameAlias;
				// isConfuseDuplicateFieldDB用原来的,取名时应该取不有重名的
				if (!isConfuseDuplicateFieldDB && StringUtils.isNotBlank(mainTableAlias) && !nameAlias.contains(".")) {
					newAlias = "'"+mainTableAlias + "." + nameAlias+"'"; //mysql ok
				}
				if (isSQLite) {
					funStr += " " + K.as + " " + newAlias;
				} else {
					funStr += " " + newAlias;
				}
			} else {
				funUseName = columnName;
			}

			if (get_FunStructForSharding) { // sharding
//				funStructs[i+adjust] = new FunStruct(funUseName, functionTypeName);
				funStructs.add(new FunStruct(funUseName, functionTypeName));
				if (!hasAvg && FunctionType.AVG.getName().equalsIgnoreCase(functionTypeName)) {
					hasAvg = true;
//					adjust++;
					//TODO 一样也要改
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
	
//	private static String _toColumnName(String fieldName, Class entityClass) {
//		return HoneyUtil.toColumnName(fieldName, entityClass);
//	}

	private static boolean adjustAnd(StringBuffer sqlBuffer, boolean isNeedAnd) {
		return ConditionHelper.adjustAnd(sqlBuffer, isNeedAnd);
	}

	public static List<PreparedValue> processIn(Object v) {
		return ConditionHelper.processIn(v);
	}

	static void addValeToPvList(List<PreparedValue> list, Object value) {
		ConditionHelper.addValeToPvList(list, value);
	}

	public static void processIn(StringBuffer sqlBuffer, List<PreparedValue> list, Object v) {
		ConditionHelper.processIn(sqlBuffer, list, v);
	}

	public static String processLike(Op op, String v) {
		return ConditionHelper.processLike(op, v);
	}

	private static void adjustSqlServerPagingIfNeed(StringBuffer sqlBuffer, Map<String, String> orderByMap,
			Integer start,
			Class entityClass, String useSubTableNames[]) {
		ConditionHelper.adjustSqlServerPagingIfNeed(sqlBuffer, orderByMap, start, entityClass, useSubTableNames);
	}
	
//	static void processPagingAndForUpdate0(StringBuffer sqlBuffer, List<PreparedValue> list, Condition condition,
//			boolean firstWhere, String useSubTableNames[]) {
//		//...省略其它代码
//		String sql = "";
//		//分页主要代码
//		if(ShardingUtil.hadSharding()) {
////			在首次生成sql前就先调整好分页；在分片引擎就不用再调用页码
//			sql = getDbFeature().toPageSql(sqlBuffer.toString(), firstRecordIndex(), start + size);
//		}else {
//			sql = getDbFeature().toPageSql(sqlBuffer.toString(), start, size);
//		}
//		//...省略其它代码
//	}

	// TODO useSubTableNames 要更新。 主要是加表名。
	static void processPagingAndForUpdate(StringBuffer sqlBuffer, List<PreparedValue> list, Condition condition,
			boolean firstWhere, String useSubTableNames[]) {

		Class entityClass = (Class) OneTimeParameter.getAttribute(StringConst.Column_EC);

		if (condition == null) return;

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
					
					//TODO ShardingUtil.hadSharding()
					if(ShardingUtil.hadSharding()) {
//						先调整好分页，后面就不用再调 //TODO
						//以下regShadingPage就不用再注册。
						//TODO copy condition??
//						conditionImpl.start(firstRecordIndex())
//						.size(start + size - 1);
//						firstRecordIndex(), start + size - 1
						sql = getDbFeature().toPageSql(sqlBuffer.toString(), firstRecordIndex(), start + size);
					}else {
						sql = getDbFeature().toPageSql(sqlBuffer.toString(), start, size);
					}

//					sql = getDbFeature().toPageSql(sqlBuffer.toString(), start, size);
//					ShardingReg.regShadingPage(sqlBuffer.toString(), sql, start, size);// 2.0
					
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
	}
	
	private static int firstRecordIndex() {
		return ShardingUtil.firstRecordIndex();
	}
	
	static boolean isNeedRewriteSqlByPage(Condition condition) {
		if (condition == null) return false;

		ConditionImpl conditionImpl = (ConditionImpl) condition;
		Integer start = conditionImpl.getStart();
		Integer size = conditionImpl.getSize();
		
//		return (start != null && start > 1 && size != null && size > 1); //size==1也有可能要改写??  是的
//      只传size也要改写
		
		return (start != null && start > 1) || (size != null && size > 0);
	}
	

}

class QueryConditionWrap extends ConditionWrap {
	//若是没用到这两个，可以不定义这个类。  TODO
	List<String> groupNameslist;
	Map<String, String> orderByMap;
//	Integer start = null;
//	Integer size = null;
//	boolean forUpdate;

//	public QueryConditionWrap(StringBuffer sqlBuffer, List<?> pvList, boolean isFirst, List<String> groupNameslist,
//			Map<String, String> orderByMap, Integer start, Integer size, boolean forUpdate) {
	public QueryConditionWrap(StringBuffer sqlBuffer, List<?> pvList, boolean isFirst, List<String> groupNameslist,
			Map<String, String> orderByMap) {
		super(sqlBuffer, pvList, isFirst);
		this.groupNameslist = groupNameslist;
		this.orderByMap = orderByMap;
//		this.start = start;
//		this.size = size;
//		this.forUpdate = forUpdate;
	}
}
