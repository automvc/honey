/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.BeeException;
import org.teasoft.bee.osql.MapSqlKey;
import org.teasoft.bee.osql.MapSqlSetting;
import org.teasoft.bee.osql.api.MapSql;
import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.osql.exception.BeeErrorGrammarException;
import org.teasoft.bee.osql.exception.BeeIllegalBusinessException;
import org.teasoft.bee.osql.exception.BeeIllegalSQLException;
import org.teasoft.honey.distribution.GenIdFactory;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.dialect.sqlserver.SqlServerPagingStruct;
import org.teasoft.honey.osql.util.NameCheckUtil;
import org.teasoft.honey.util.ObjectUtils;
import org.teasoft.honey.util.StringUtils;

/**
 * MapSql处理器.MapSql Processor.
 * 本类默认不支持Sharding分片功能.
 * @author Kingstar
 * @since  1.9
 */
public class MapSqlProcessor {

	private static final String TABLE = "Table";
	private static final String KEY_NAME_IS = "The Key name is ";

	private MapSqlProcessor() {}

	private static DbFeature getDbFeature() {
		return BeeFactory.getHoneyFactory().getDbFeature();
	}

	public static String toSelectSqlByMap(MapSql mapSql) {

		MapSqlImpl mapSqlImpl = (MapSqlImpl) mapSql;
		Map<MapSqlKey, String> sqlkeyMap = mapSqlImpl.getSqlkeyMap();
		Map<String, Object> whereConditonMap = mapSqlImpl.getKvMap();
		Map<MapSqlSetting, Boolean> sqlSettingMap = mapSqlImpl.getSqlSettingMap();

		String tableName = sqlkeyMap.get(MapSqlKey.Table);
		checkTable(tableName);
		String selectColumns = sqlkeyMap.get(MapSqlKey.SelectColumns);

		Boolean isTransfer = sqlSettingMap.get(MapSqlSetting.IsNamingTransfer);
		if (isTransfer == null) isTransfer = false;
		if (isTransfer) {
			selectColumns = _toColumnName(selectColumns); // just for select
			OneTimeParameter.setTrueForKey(StringConst.DoNotCheckAnnotation);// map sql do not check notation
			tableName = _toTableName(tableName);
		}

		StringBuffer sqlBuffer = new StringBuffer();

		sqlBuffer.append(K.select).append(K.space).append(selectColumns).append(K.space).append(K.from).append(K.space)
				.append(tableName);

		// where
		List<PreparedValue> list = new ArrayList<>();
		boolean firstWhere = true;

		if (ObjectUtils.isNotEmpty(whereConditonMap)) {
			Boolean isBooleanTransfer = sqlSettingMap.get(MapSqlSetting.IsTransferTrueFalseStringToBooleanType);
			parseBoolean(whereConditonMap, isBooleanTransfer); // V1.11
			firstWhere = where(whereConditonMap, list, sqlBuffer, isTransfer, getIncludeType(sqlSettingMap));
		}

		// 2.4.0
		WhereConditionWrap wrap = ConditionHelper.processWhereCondition(mapSqlImpl.getWhereCondition(), firstWhere, null);
		if (wrap != null) {
			sqlBuffer.append(wrap.getSqlBuffer());
			list.addAll((List) wrap.getPvList());
		}

		// group by
		String groupByField = sqlkeyMap.get(MapSqlKey.GroupBy);
		if (StringUtils.isNotBlank(groupByField)) {
			checkExpression(groupByField);
			sqlBuffer.append(K.space).append(K.groupBy).append(K.space).append(groupByField);
		}
		// having
		String havingStr = sqlkeyMap.get(MapSqlKey.Having);
		if (StringUtils.isNotBlank(havingStr)) {
			checkExpression(havingStr);
			sqlBuffer.append(K.space).append(K.having).append(K.space).append(havingStr);
		}

		SqlServerPagingStruct struct = new SqlServerPagingStruct();

		// order by
		String orderByStr = sqlkeyMap.get(MapSqlKey.OrderBy);
		if (StringUtils.isNotBlank(orderByStr)) {
			checkExpression(orderByStr);
			sqlBuffer.append(K.space).append(K.orderBy).append(K.space).append(orderByStr);
			struct.setHasOrderBy(true);
		}

		Integer start = mapSqlImpl.getStart();
		Integer size = mapSqlImpl.getSize();

		String sql = sqlBuffer.toString();

		String pkName = sqlkeyMap.get(MapSqlKey.PrimaryKey);
		if (StringUtils.isNotBlank(pkName)) {
			struct.setOrderColumn(pkName);
		}
		HoneyContext.setSqlServerPagingStruct(sql, struct);

		if (start != null && size != null) {
			sql = getDbFeature().toPageSql(sql, start, size);
		} else if (size != null) {
			sql = getDbFeature().toPageSql(sql, size);
		}

		setContext(sql, list, tableName);

		return sql;
	}

	private static int getIncludeType(Map<MapSqlSetting, Boolean> sqlSettingMap) {
		int includeType = -1;
		boolean f1 = false, f2 = false;
		Boolean isIncludeNull = sqlSettingMap.get(MapSqlSetting.IsIncludeNull);
		Boolean isIncludeEmptyString = sqlSettingMap.get(MapSqlSetting.IsIncludeEmptyString);

		if (isIncludeNull != null && isIncludeNull) {
			includeType = 0;
			f1 = true;
		}
		if (isIncludeEmptyString != null && isIncludeEmptyString) {
			includeType = 1;
			f2 = true;
		}
		if (f1 && f2) includeType = 2;

		return includeType;
	}

	public static String toDeleteSqlByMap(MapSql mapSql) {

		MapSqlImpl suidMapImpl = (MapSqlImpl) mapSql;
		Map<MapSqlKey, String> sqlkeyMap = suidMapImpl.getSqlkeyMap();
		Map<String, Object> whereConditonMap = suidMapImpl.getKvMap();

		Map<MapSqlSetting, Boolean> sqlSettingMap = suidMapImpl.getSqlSettingMap();

		String tableName = sqlkeyMap.get(MapSqlKey.Table);
		checkTable(tableName);

		Boolean isTransfer = sqlSettingMap.get(MapSqlSetting.IsNamingTransfer);
		if (isTransfer == null) isTransfer = false;
		if (isTransfer) {
			OneTimeParameter.setTrueForKey(StringConst.DoNotCheckAnnotation);// map sql do not check notation
			tableName = _toTableName(tableName);
		}

		StringBuffer sqlBuffer = new StringBuffer();

		sqlBuffer.append(K.delete).append(K.space).append(K.from).append(K.space).append(tableName);

		// where
		List<PreparedValue> list = new ArrayList<>();
		boolean firstWhere = false;
		if (ObjectUtils.isNotEmpty(whereConditonMap)) {
			Boolean isBooleanTransfer = sqlSettingMap.get(MapSqlSetting.IsTransferTrueFalseStringToBooleanType);
			parseBoolean(whereConditonMap, isBooleanTransfer); // V1.11
			firstWhere = where(whereConditonMap, list, sqlBuffer, isTransfer, getIncludeType(sqlSettingMap));
		}

		// 2.4.0
		WhereConditionWrap wrap = ConditionHelper.processWhereCondition(suidMapImpl.getWhereCondition(), firstWhere,
				null);
		if (wrap != null) {
			sqlBuffer.append(wrap.getSqlBuffer());
			list.addAll((List) wrap.getPvList());
			firstWhere = wrap.isFirst();
		}

		String sql = sqlBuffer.toString();

		// 不允许删整张表
		// 只支持是否带where检测 v1.7.2
		if (firstWhere) {
			boolean notDeleteWholeRecords = HoneyConfig.getHoneyConfig().notDeleteWholeRecords;
			if (notDeleteWholeRecords) {
				Logger.logSQL(LogSqlParse.parseSql("In MapSuid, delete SQL: ", sql));
				throw new BeeIllegalBusinessException(
						"BeeIllegalBusinessException: It is not allowed delete whole records in one table.");
			}
		}
		setContext(sql, list, tableName);

		return sql;

	}

	public static String toUpdateSqlByMap(MapSql mapSql) {

		MapSqlImpl suidMapImpl = (MapSqlImpl) mapSql;
		Map<MapSqlKey, String> sqlkeyMap = suidMapImpl.getSqlkeyMap();
		Map<String, Object> whereMap = suidMapImpl.getKvMap();

		Map<MapSqlSetting, Boolean> sqlSettingMap = suidMapImpl.getSqlSettingMap();

		String tableName = sqlkeyMap.get(MapSqlKey.Table);
		checkTable(tableName);

		Boolean isTransfer = sqlSettingMap.get(MapSqlSetting.IsNamingTransfer);
		if (isTransfer == null) isTransfer = false;
		if (isTransfer) {
			OneTimeParameter.setTrueForKey(StringConst.DoNotCheckAnnotation);// map sql do not check notation
			tableName = _toTableName(tableName);
		}

		StringBuffer sqlBuffer = new StringBuffer();

		sqlBuffer.append(K.update).append(" ");
		sqlBuffer.append(tableName);

		List<PreparedValue> list = new ArrayList<>();

		boolean firstSet = true;

		Map<String, Object> newValueMap = suidMapImpl.getNewKvMap();
		if (ObjectUtils.isNotEmpty(newValueMap)) {
			Boolean isBooleanTransfer = sqlSettingMap.get(MapSqlSetting.IsTransferTrueFalseStringToBooleanType);
			parseBoolean(newValueMap, isBooleanTransfer); // V1.11
			firstSet = updateSet(newValueMap, list, sqlBuffer, isTransfer, getIncludeType(sqlSettingMap));
		}

		// 2.4.0 使用Condition设置update set
		UpdateSetConditionWrap updateSetWrap = ConditionHelper.processUpdateSetCondition(sqlBuffer, list,
				suidMapImpl.getUpdateSetCondition(), firstSet);
		if (updateSetWrap != null) {
//			sqlBuffer.append(updateSetWrap.getSqlBuffer());
//			list.addAll((List) updateSetWrap.getPvList()); //already pass list to processUpdateSetCondition
			firstSet = updateSetWrap.isFirst();
		}

		boolean firstWhere = true;
		if (ObjectUtils.isNotEmpty(whereMap)) {
			Boolean isBooleanTransfer = sqlSettingMap.get(MapSqlSetting.IsTransferTrueFalseStringToBooleanType);
			parseBoolean(whereMap, isBooleanTransfer); // V1.11
			firstWhere = where(whereMap, list, sqlBuffer, isTransfer, getIncludeType(sqlSettingMap));
		}

		// 2.4.0
		WhereConditionWrap wrap = ConditionHelper.processWhereCondition(suidMapImpl.getWhereCondition(), firstWhere,
				null); // do not pass list and sqlBuffer
		if (wrap != null) {
			sqlBuffer.append(wrap.getSqlBuffer());
			list.addAll((List) wrap.getPvList());
			firstWhere = wrap.isFirst();
		}

		String sql = sqlBuffer.toString();

		if (firstSet) {
			Logger.logSQL(LogSqlParse.parseSql("In MapSuid, update SQL: ", sql));
			throw new BeeErrorGrammarException("BeeErrorGrammarException: the SQL update set part is empty!");
		}

		// 不允许更新整张表
		// 只支持是否带where检测 v1.7.2
		if (firstWhere) {
			boolean notUpdateWholeRecords = HoneyConfig.getHoneyConfig().notUpdateWholeRecords;
			if (notUpdateWholeRecords) {
				Logger.logSQL(LogSqlParse.parseSql("In MapSuid, update SQL: ", sql));
				throw new BeeIllegalBusinessException(
						"BeeIllegalBusinessException: It is not allowed update whole records in one table.");
			}
		}

		setContext(sql, list, tableName);

		return sql;
	}

	public static String toInsertSqlByMap(MapSql mapSql) {
		return toInsertSqlByMap(mapSql, false);
	}

	public static String toInsertSqlByMap(MapSql mapSql, boolean returnId) {

		MapSqlImpl suidMapImpl = (MapSqlImpl) mapSql;
		Map<MapSqlKey, String> sqlkeyMap = suidMapImpl.getSqlkeyMap();
		Map<String, Object> insertKvMap = suidMapImpl.getKvMap();

		Map<MapSqlSetting, Boolean> sqlSettingMap = suidMapImpl.getSqlSettingMap();

		String tableName = sqlkeyMap.get(MapSqlKey.Table);
		checkTable(tableName);
		String pkName = sqlkeyMap.get(MapSqlKey.PrimaryKey);
		String orgi_tableName = tableName;

		Boolean isTransfer = sqlSettingMap.get(MapSqlSetting.IsNamingTransfer);
		if (isTransfer == null) isTransfer = false;
		if (isTransfer) {
			OneTimeParameter.setTrueForKey(StringConst.DoNotCheckAnnotation);// map sql do not check notation
			tableName = _toTableName(tableName);
		}

		StringBuffer sqlBuffer = new StringBuffer();
		sqlBuffer.append(K.insert).append(K.space).append(K.into).append(K.space).append(tableName);

		Object oldId = null;
		List<PreparedValue> list = new ArrayList<>();
		if (ObjectUtils.isNotEmpty(insertKvMap)) {
//			if(isNeedProcessId) 
			oldId = processId(insertKvMap, orgi_tableName, pkName, sqlSettingMap, returnId); // fixed bug,用于获取分布式id的表名要一致
			Boolean isBooleanTransfer = sqlSettingMap.get(MapSqlSetting.IsTransferTrueFalseStringToBooleanType);
			parseBoolean(insertKvMap, isBooleanTransfer); // V1.11
			toInsertSql(insertKvMap, list, sqlBuffer, isTransfer, getIncludeType(sqlSettingMap));
		} else {
			throw new BeeException("Must set the insert vlaue with MapSql.put(String fieldName, Object value) !");
		}

		String sql = sqlBuffer.toString();
		setContext(sql, list, tableName);

//        if(isNeedProcessId) 
		revertId(insertKvMap, oldId, pkName);

		return sql;
	}

	public static String toCountSqlByMap(MapSql mapSql) {
		MapSqlImpl t = (MapSqlImpl) mapSql;
//		MapSql newOne=copyForCount(t);
		MapSql newOne = t.copyForCount();
		newOne.put(MapSqlKey.SelectColumns, "count(*)");
		return toSelectSqlByMap(newOne);
	}

	private static Object processId(Map<String, Object> insertKvMap, String tableName, String customPkName,
			Map<MapSqlSetting, Boolean> sqlSettingMap, boolean returnId) {

		Boolean isGenId = sqlSettingMap.get(MapSqlSetting.IsGenId);
		isGenId = isGenId == null ? false : isGenId;

		Boolean isUseIntegerId = sqlSettingMap.get(MapSqlSetting.IsUseIntegerId);
		isUseIntegerId = isUseIntegerId == null ? false : isUseIntegerId;

		boolean isUpper = false;
		boolean isPrimaryKey = false;
		Object id = insertKvMap.get("id");
		String pkName = "id";

		if (StringUtils.isNotBlank(customPkName)) {
			isPrimaryKey = true;
			id = insertKvMap.get(customPkName);
			pkName = customPkName;
		} else if (id == null) {
			id = insertKvMap.get("ID");
			if (id != null) { // fixed bug V1.17
				isUpper = true;
				pkName = "ID";
			}
		}

//		Long replaceId=null;

		boolean genAll = HoneyConfig.getHoneyConfig().genid_forAllTableLongId;
		boolean replaceOldValue = HoneyConfig.getHoneyConfig().genid_replaceOldId;

		if (isGenId || (id != null && genAll && replaceOldValue) || (id == null && genAll)) { // 不为null,需要允许覆盖; 为null也要是genAll
			Object newId;
			if (isUseIntegerId) {
				newId = (int) GenIdFactory.get(tableName, GenIdFactory.GenType_IntSerialIdReturnLong);
			} else {
				newId = GenIdFactory.get(tableName);
			}

			if (isPrimaryKey) insertKvMap.put(customPkName, newId);
			else if (isUpper) insertKvMap.put("ID", newId);
			else insertKvMap.put("id", newId);
			if (returnId) OneTimeParameter.setAttribute(StringConst.MapSuid_Insert_Has_ID, newId);
		}

		if (id != null) {
			if (!(genAll && replaceOldValue))
				if (returnId) OneTimeParameter.setAttribute(StringConst.MapSuid_Insert_Has_ID, id);
		}

		if ("".equals(pkName) || pkName.contains(",")) pkName = "id";
		// V1.11 for SqlLib use column name
		OneTimeParameter.setAttribute(StringConst.PK_Column_For_ReturnId, HoneyUtil.toCloumnNameForPks(pkName, null));

		return id;
	}

	private static void revertId(Map<String, Object> insertKvMap, Object oldId, String customPkName) {
		Object id = insertKvMap.get("id");
		Object pkId = null;

		if (ObjectUtils.isNotEmpty(customPkName)) {
			pkId = insertKvMap.get(customPkName);
		}

		if (pkId != null) {
			if (oldId == null) insertKvMap.remove(customPkName);
			else insertKvMap.put(customPkName, oldId);
		} else if (id != null) {
			if (oldId == null) insertKvMap.remove("id");
			else insertKvMap.put("id", oldId);
		} else {
			id = insertKvMap.get("ID");
			if (id != null) {
				if (oldId == null) insertKvMap.remove("ID");
				else insertKvMap.put("ID", oldId);
			}
		}
	}

	private static boolean where(Map<String, Object> whereConditonMap, List<PreparedValue> list, StringBuffer sqlBuffer,
			boolean isTransfer, int includeType) {
		boolean firstWhere = true;

		PreparedValue preparedValue = null;
		for (Map.Entry<String, Object> entry : whereConditonMap.entrySet()) {

			if (TABLE.equalsIgnoreCase(entry.getKey())) {
				Logger.warn(KEY_NAME_IS + entry.getKey() + " , will be ignored in 'where' part!");
				continue;
			}

			checkName(entry.getKey()); // v1.9.8

			Object value = entry.getValue();

			if (HoneyUtil.isContinue(includeType, value, null)) continue;

			if (firstWhere) {
				sqlBuffer.append(K.space).append(K.where).append(K.space); // where
				firstWhere = false;
			} else {
				sqlBuffer.append(K.space).append(K.and).append(K.space); // and
			}

			if (isTransfer) {
				sqlBuffer.append(_toColumnName(entry.getKey()));
			} else {
				sqlBuffer.append(entry.getKey());
			}

			if (value == null) {
				sqlBuffer.append(" ").append(K.isNull);
			} else {
				sqlBuffer.append("=");
				sqlBuffer.append("?");

				preparedValue = new PreparedValue();
				preparedValue.setType(entry.getValue().getClass().getSimpleName());
				preparedValue.setValue(entry.getValue());
				list.add(preparedValue);
			}
		}
		return firstWhere;
	}

	private static boolean updateSet(Map<String, Object> setConditonMap, List<PreparedValue> list, StringBuffer sqlBuffer,
			boolean isTransfer, int includeType) {
		boolean firstSet = true;

		PreparedValue preparedValue = null;
		for (Map.Entry<String, Object> entry : setConditonMap.entrySet()) {

			if (TABLE.equalsIgnoreCase(entry.getKey())) {
				Logger.warn(KEY_NAME_IS + entry.getKey() + " , will be ignored!");
				continue;
			}

			checkName(entry.getKey()); // v1.9.8

			Object value = entry.getValue();

			if (HoneyUtil.isContinue(includeType, value, null)) continue;

			if (firstSet) {
				sqlBuffer.append(K.space).append(K.set).append(K.space); // set
				firstSet = false;
			} else {
				sqlBuffer.append(K.space).append(",").append(K.space); // ,
			}

			if (isTransfer) {
				sqlBuffer.append(_toColumnName(entry.getKey()));
			} else {
				sqlBuffer.append(entry.getKey());
			}

			if (value == null) {
				sqlBuffer.append(" =").append(K.Null); // =
			} else {
				sqlBuffer.append("=");
				sqlBuffer.append("?");

				preparedValue = new PreparedValue();
				preparedValue.setType(entry.getValue().getClass().getSimpleName());
				preparedValue.setValue(entry.getValue());
				list.add(preparedValue);
			}
		}
		return firstSet;
	}

	private static void toInsertSql(Map<String, Object> insertKvMap, List<PreparedValue> list, StringBuffer sqlBuffer,
			boolean isTransfer, int includeType) {
		StringBuffer sqlValue = new StringBuffer(" (");
		boolean isFirst = true;
		sqlBuffer.append(" (");

		PreparedValue preparedValue = null;
		for (Map.Entry<String, Object> entry : insertKvMap.entrySet()) {

			if (TABLE.equalsIgnoreCase(entry.getKey())) {
				Logger.warn(KEY_NAME_IS + entry.getKey() + " , will be ignored!");
				continue;
			}

			checkName(entry.getKey()); // v1.9.8

			Object value = entry.getValue();
			if (HoneyUtil.isContinue(includeType, value, null)) continue;
			if (isFirst) {
				isFirst = false;
			} else {
				sqlBuffer.append(",");
				sqlValue.append(",");
			}

			if (isTransfer) {
				sqlBuffer.append(_toColumnName(entry.getKey()));
			} else {
				sqlBuffer.append(entry.getKey());
			}
			sqlValue.append("?");

			preparedValue = new PreparedValue();
			if (value == null) preparedValue.setType(Object.class.getSimpleName());
			else preparedValue.setType(value.getClass().getSimpleName());
			preparedValue.setValue(value);
			list.add(preparedValue);
		}
		sqlValue.append(")");

		sqlBuffer.append(") ").append(K.values);
		sqlBuffer.append(sqlValue);

	}

	private static void setContext(String sql, List<PreparedValue> list, String tableName) {
		HoneyContext.setContext(sql, list, tableName);
	}

	private static String _toTableName(String tableName) {
//		本类默认不支持Sharding分片功能.
		return NameTranslateHandle.toTableName(tableName);
	}

	private static String _toColumnName(String fieldName) {
		checkName(fieldName);
		return NameTranslateHandle.toColumnName(fieldName);
	}

	private static void checkTable(String tableName) {
		if (StringUtils.isBlank(tableName)) {
			throw new BeeException("The Map which key is SqlMapKey.Table must define!");
		}
		checkName(tableName);
	}

	private static void checkName(String tableName) {
		NameCheckUtil.checkName(tableName);
	}

	private static void checkExpression(String expression) {
		if (Check.isNotValidExpressionForJustFetch(expression)) {
			throw new BeeIllegalSQLException(" '" + expression + "' is invalid in MapSql!");
		}
	}

	private static void parseBoolean(Map<String, Object> map, Boolean isBooleanTransfer) {
		if (map == null) return;
		if (Boolean.FALSE.equals(isBooleanTransfer)) return;
//		isBooleanTransfer is null or true will be Transfer

		for (Map.Entry<String, Object> entry : map.entrySet()) {
			Object obj = entry.getValue();
			if (!(obj instanceof String)) continue;
			String v = (String) obj;
			if ("true".equalsIgnoreCase(v) || "false".equalsIgnoreCase(v)) {
				map.put(entry.getKey(), Boolean.parseBoolean(v));
			}
		}
	}

}
