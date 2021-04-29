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
import org.teasoft.bee.osql.MapSql;
import org.teasoft.bee.osql.MapSqlKey;
import org.teasoft.bee.osql.MapSqlSetting;
import org.teasoft.bee.osql.exception.BeeIllegalBusinessException;
import org.teasoft.honey.distribution.GenIdFactory;
import org.teasoft.honey.util.ObjectUtils;
import org.teasoft.honey.util.StringUtils;

/**
 * @author Kingstar
 * @since  1.9
 */
public class MapSqlProcessor {

	public static String toSelectSqlByMap(MapSql mapSql) {

		MapSqlImpl suidMapImpl = (MapSqlImpl) mapSql;
		Map<MapSqlKey, String> sqlkeyMap = suidMapImpl.getSqlkeyMap();
		Map<String, Object> whereConditonMap = suidMapImpl.getKvMap();
		Map<MapSqlSetting, Boolean> sqlSettingMap = suidMapImpl.getSqlSettingMap();

		String tableName = sqlkeyMap.get(MapSqlKey.Table);
		checkTable(tableName);
		String selectColumns = sqlkeyMap.get(MapSqlKey.SelectColumns);

		Boolean isTransfer = sqlSettingMap.get(MapSqlSetting.IsNamingTransfer);
		if (isTransfer == null) isTransfer = false;
		if (isTransfer) {
			selectColumns = _toColumnName(selectColumns);
			OneTimeParameter.setTrueForKey(StringConst.DoNotCheckAnnotation);//map sql do not check notation
			tableName = _toTableName(tableName);
		}

		StringBuffer sqlBuffer = new StringBuffer();

		sqlBuffer.append(K.select).append(K.space).append(selectColumns).append(K.space).append(K.from)
				.append(K.space).append(tableName);

		//where
		List<PreparedValue> list = new ArrayList<>();

		if (ObjectUtils.isNotEmpty(whereConditonMap)) {
			where(whereConditonMap, list, sqlBuffer, isTransfer, getIncludeType(sqlSettingMap));
		}

		//group by
		String groupByField = sqlkeyMap.get(MapSqlKey.GroupBy);
		if (StringUtils.isNotBlank(groupByField)) {
			sqlBuffer.append(K.space).append(K.groupBy).append(K.space).append(groupByField);
		}
		//having
		String havingStr = sqlkeyMap.get(MapSqlKey.Having);
		if (StringUtils.isNotBlank(havingStr)) {
			sqlBuffer.append(K.space).append(K.having).append(K.space).append(havingStr);
		}
		//order by
		String orderByStr = sqlkeyMap.get(MapSqlKey.OrderBy);
		if (StringUtils.isNotBlank(orderByStr)) {
			sqlBuffer.append(K.space).append(K.orderBy).append(K.space).append(orderByStr);
		}

		String sql = sqlBuffer.toString();
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
			OneTimeParameter.setTrueForKey(StringConst.DoNotCheckAnnotation);//map sql do not check notation
			tableName = _toTableName(tableName);
		}

		StringBuffer sqlBuffer = new StringBuffer();

		sqlBuffer.append(K.delete).append(K.space).append(K.from).append(K.space).append(tableName);

		//where
		List<PreparedValue> list = new ArrayList<>();
		boolean firstWhere = false;
		if (ObjectUtils.isNotEmpty(whereConditonMap)) {
			firstWhere = where(whereConditonMap, list, sqlBuffer, isTransfer,
					getIncludeType(sqlSettingMap));
		}

		String sql = sqlBuffer.toString();

		//不允许删整张表
		//只支持是否带where检测   v1.7.2 
		if (firstWhere) {
			boolean notDeleteWholeRecords = HoneyConfig.getHoneyConfig().notDeleteWholeRecords;
			if (notDeleteWholeRecords) {
				Logger.logSQL("In MapSuid, delete SQL: ", sql);
				throw new BeeIllegalBusinessException(
						"BeeIllegalBusinessException: It is not allowed delete whole records in one table.");
			}
		}
		setContext(sql, list, tableName);

		return sql;

	}

	public static String toInsertSqlByMap(MapSql mapSql) {

		MapSqlImpl suidMapImpl = (MapSqlImpl) mapSql;
		Map<MapSqlKey, String> sqlkeyMap = suidMapImpl.getSqlkeyMap();
		Map<String, Object> insertKvMap = suidMapImpl.getKvMap();

		Map<MapSqlSetting, Boolean> sqlSettingMap = suidMapImpl.getSqlSettingMap();

		String tableName = sqlkeyMap.get(MapSqlKey.Table);
		checkTable(tableName);

		Boolean isTransfer = sqlSettingMap.get(MapSqlSetting.IsNamingTransfer);
		if (isTransfer == null) isTransfer = false;
		if (isTransfer) {
			OneTimeParameter.setTrueForKey(StringConst.DoNotCheckAnnotation);//map sql do not check notation
			tableName = _toTableName(tableName);
		}

		StringBuffer sqlBuffer = new StringBuffer();
		sqlBuffer.append(K.insert).append(K.space).append(K.into).append(K.space).append(tableName);

		Object oldId=null;
		List<PreparedValue> list = new ArrayList<>();
		if (ObjectUtils.isNotEmpty(insertKvMap)) {
			oldId=processId(insertKvMap,tableName);
			toInsertSql(insertKvMap, list, sqlBuffer, isTransfer, getIncludeType(sqlSettingMap));
		}else {
			throw new BeeException("Must set the insert vlaue with MapSql.put(String fieldName, Object value) !");
		}

		String sql = sqlBuffer.toString();
		setContext(sql, list, tableName);
		
		revertId(insertKvMap,oldId);

		return sql;
	}
	
	private static void checkTable(String tableName) {
		if (StringUtils.isBlank(tableName)) {
			throw new BeeException("The Map which key is SqlMapKey.Table must define!");
		}
	}
	
	private static Object processId(Map<String, Object> insertKvMap,String tableName) {
		Object id=insertKvMap.get("id");
		boolean isUpper=false;
		if(id==null) {
			id=insertKvMap.get("ID");
			isUpper=true;
		}
		
//		Long replaceId=null;
		
		boolean genAll = HoneyConfig.getHoneyConfig().genid_forAllTableLongId;
		boolean replaceOldValue = HoneyConfig.getHoneyConfig().genid_replaceOldId;
		if(id!=null) {
			if(genAll && replaceOldValue) {
				long newId = GenIdFactory.get(tableName);
				if(isUpper) insertKvMap.put("ID", newId);
				else insertKvMap.put("id", newId);
				OneTimeParameter.setAttribute("_SYS_Bee_MapSuid_Insert_Has_ID", newId);
//				replaceId=newId;
			}else {
				OneTimeParameter.setAttribute("_SYS_Bee_MapSuid_Insert_Has_ID", id);
			}
		}else {
			if(genAll) {
				long newId = GenIdFactory.get(tableName);
				insertKvMap.put("id", newId);
				OneTimeParameter.setAttribute("_SYS_Bee_MapSuid_Insert_Has_ID", newId);
//				replaceId=newId;
			}
		}
		
		return id;
	}
	
	private static void revertId(Map<String, Object> insertKvMap, Object oldId) {
		Object id = insertKvMap.get("id");
		if (id != null) {
			if (oldId == null)
				insertKvMap.remove("id");
			else
				insertKvMap.put("id", oldId);
		} else {
			id = insertKvMap.get("ID");
			if (id != null) {
				if (oldId == null)
					insertKvMap.remove("ID");
				else
					insertKvMap.put("ID", oldId);
			}
		}
	}

	private static boolean where(Map<String, Object> whereConditonMap, List<PreparedValue> list,
			StringBuffer sqlBuffer, boolean isTransfer, int includeType) {
		boolean firstWhere = true;

		PreparedValue preparedValue = null;
		for (Map.Entry<String, Object> entry : whereConditonMap.entrySet()) {

			Object value = entry.getValue();

			if (HoneyUtil.isContinue(includeType, value, null)) continue;

			if (firstWhere) {
				sqlBuffer.append(K.space).append(K.where).append(K.space); //where 
				firstWhere = false;
			} else {
				sqlBuffer.append(K.space).append(K.and).append(K.space); //and
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

	private static void toInsertSql(Map<String, Object> insertKvMap, List<PreparedValue> list,
			StringBuffer sqlBuffer, boolean isTransfer, int includeType) {
		StringBuffer sqlValue = new StringBuffer(" (");
		boolean isFirst = true;
		sqlBuffer.append(" (");

		PreparedValue preparedValue = null;
		for (Map.Entry<String, Object> entry : insertKvMap.entrySet()) {

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
			if (value == null)
				preparedValue.setType(Object.class.getSimpleName());
			else
				preparedValue.setType(value.getClass().getSimpleName());
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
		return NameTranslateHandle.toTableName(tableName);
	}

	private static String _toColumnName(String fieldName) {
		return NameTranslateHandle.toColumnName(fieldName);
	}

}
