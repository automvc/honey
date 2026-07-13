package org.teasoft.honey.osql.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.BeeException;
import org.teasoft.bee.osql.FunctionType;
import org.teasoft.bee.osql.OneMethod1;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.osql.exception.BeeErrorGrammarException;
import org.teasoft.bee.sharding.FunStruct;
import org.teasoft.bee.sharding.GroupFunStruct;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.core.ConditionImpl.FunExpress;
import org.teasoft.honey.sharding.ShardingReg;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.util.ObjectUtils;
import org.teasoft.honey.util.StringUtils;

public class ConditionHelper3 {

	private ConditionHelper3() {}

	private static DbFeature getDbFeature() {
		return BeeFactory.getHoneyFactory().getDbFeature();
	}
	
	static WhereConditionWrap processWhereCondition3(Condition condition, boolean firstWhere, String mainTableAlias,
			Class<?> mainClass, Map<String, MoreTableStruct3> moreTableStructMap) {
		return ConditionHelper._processWhereCondition(condition, firstWhere, new OneMethod1<String>() {
			public String doOneMethod(String fieldName) {
				return MoreTableHelper.fieldName2ColumnName(fieldName, mainTableAlias, mainClass, moreTableStructMap);
			}
		});
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
			String old = selectFields[i];
			selectFields[i] = MoreTableHelper.fieldName2ColumnName(selectFields[i], mainTableAlias, mainClass,
					moreTableStructMap);
			if (selectFields[i] == null) {
				throw new BeeException(old + " would transfer be null");
			}
		}

		return HoneyUtil.checkAndProcessSelectFieldViaString(columnNames, subDulFieldMap, selectFields);
	}

	public static String processFunction(String columnNames, Condition condition, String mainTableAlias,
			Class<?> mainClass, Map<String, MoreTableStruct3> moreTableStructMap) {

		if (condition == null) return null;

		boolean get_FunStructForSharding = OneTimeParameter.isTrue(StringConst.Get_GroupFunStruct); // V2.0

		ConditionImpl conditionImpl = (ConditionImpl) condition;
		List<FunExpress> funExpList = conditionImpl.getFunExpList();
		String columnName;
		String funStr = "";
		boolean isFirst = true;
		String nameAlias;
		boolean isSQLite = HoneyUtil.isSQLite();

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

				// 3.0
				String fieldName = funExpList.get(i).getField();
				columnName = MoreTableHelper.fieldName2ColumnName(fieldName, mainTableAlias, mainClass,
						moreTableStructMap);

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
					newAlias = "'" + mainTableAlias + "." + nameAlias + "'"; // mysql ok
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
					// TODO 一样也要改
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

	private static void adjustSqlServerPagingIfNeed(StringBuffer sqlBuffer, Map<String, String> orderByMap, Integer start,
			Class entityClass, String useSubTableNames[]) {
		ConditionHelper.adjustSqlServerPagingIfNeed(sqlBuffer, orderByMap, start, entityClass, useSubTableNames);
	}

	// TODO useSubTableNames 要更新。 主要是加表名。
	static void processPaging(StringBuffer sqlBuffer, List<PreparedValue> list, Condition condition, boolean firstWhere,
			String useSubTableNames[]) {

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

					// TODO ShardingUtil.hadSharding()
					if (ShardingUtil.hadSharding()) {
//						先调整好分页，后面就不用再调 //TODO
						// 以下regShadingPage就不用再注册。
						// TODO copy condition??
//						conditionImpl.start(firstRecordIndex())
//						.size(start + size - 1);
//						firstRecordIndex(), start + size - 1
						sql = getDbFeature().toPageSql(sqlBuffer.toString(), firstRecordIndex(), start + size);
					} else {
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

	static void processForUpdate(StringBuffer sqlBuffer, Condition condition, String useSubTableNames[]) {
		if (condition == null) return;
		ConditionImpl conditionImpl = (ConditionImpl) condition;

		if (useSubTableNames == null && SuidType.SELECT == conditionImpl.getSuidType()) {

			Boolean isForUpdate = conditionImpl.getForUpdate();
			if (isForUpdate != null && isForUpdate.booleanValue()) {
				sqlBuffer.append(" " + K.forUpdate + " ");
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

	static boolean hasFun(Condition condition) {
		if (condition == null) return false;
		ConditionImpl conditionImpl = (ConditionImpl) condition;
		List<FunExpress> funExpList = null;
		if (conditionImpl != null) funExpList = conditionImpl.getFunExpList();
		return ObjectUtils.isNotEmpty(funExpList);
	}

}
