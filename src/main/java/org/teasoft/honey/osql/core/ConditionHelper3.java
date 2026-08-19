package org.teasoft.honey.osql.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.BeeException;
import org.teasoft.bee.osql.OneMethod1;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.osql.exception.BeeErrorGrammarException;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.core.ConditionImpl.FunExpress;
import org.teasoft.honey.sharding.ShardingReg;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.util.ObjectUtils;

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

		String selectFields[] = conditionImpl.getSelectField();
		if (selectFields == null) return null;

		int oldLen = selectFields.length;

		// fieldName -> columnName
		for (int i = 0; i < selectFields.length; i++) {
			String old = selectFields[i];
			selectFields[i] = MoreTableHelper.fieldName2ColumnName(selectFields[i], mainTableAlias, mainClass,
					moreTableStructMap);
			if (selectFields[i] == null) {
				throw new BeeException(old + " would transfer be null");
			}
		}

		String selectField[] = ConditionHelper.addedOrderByFieldIfNeed(selectFields, conditionImpl);
		int newLen = selectField.length;

		List<String> addedColumn = null;
		if (newLen - oldLen > 0) {
			addedColumn = new ArrayList<>(newLen - oldLen);
		}

		for (int k = oldLen; k < newLen; k++) {
			String old = selectField[k];
			selectField[k] = MoreTableHelper.fieldName2ColumnName(selectField[k], mainTableAlias, mainClass,
					moreTableStructMap);
			if (selectField[k] == null) {
				throw new BeeException(old + " would transfer be null");
			} else {
				addedColumn.add(selectField[k]);
			}
		}
		if (newLen - oldLen > 0) {
			ShardingReg.regAddedOrderByColumn(addedColumn);
		}

		return HoneyUtil.checkAndProcessSelectFieldViaString(columnNames, subDulFieldMap, selectField);
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
