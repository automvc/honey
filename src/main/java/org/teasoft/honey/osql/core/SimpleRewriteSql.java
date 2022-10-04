/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.sharding.config.ShardingRegistry;
import org.teasoft.honey.util.StringUtils;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class SimpleRewriteSql {

	public static List<String[]> createSqlsAndInit(String sql) {

		List<String[]> list = new ArrayList<>();
		List<String> tabNameList = HoneyContext.getListLocal(StringConst.TabNameListLocal);
		List<String> tabSuffixList = HoneyContext.getListLocal(StringConst.TabSuffixListLocal);
		Map<String, String> tab2DsMap = HoneyContext.getCustomMapLocal(StringConst.ShardingTab2DsMap);
		List<PreparedValue> listValue = HoneyContext.justGetPreparedValue(sql);
		_createSql(list, tabSuffixList, sql, listValue, tabNameList, tab2DsMap);

		return list;
	}

	static void _createSql(List<String[]> list, List<String> tabSuffixList, String sql,
			List<PreparedValue> listValue, List<String> tabNameList,
			Map<String, String> tab2DsMap) {
		String sqls[] = new String[tabSuffixList.size()];
		String dsArray[] = new String[tabSuffixList.size()];
		for (int i = 0; i < tabSuffixList.size(); i++) {
			sqls[i] = sql.replace(StringConst.ShardingTableIndexStr, tabSuffixList.get(i)); // eg: #$(index)$#替换成下标等
			HoneyContext.setPreparedValue(sqls[i], listValue);
			String dsName = tab2DsMap.get(tabSuffixList.get(i)); // 只在使用注解时, 分库与分表同属于一个分片键,才有用. TODO
			if (StringUtils.isBlank(dsName)) {
				dsName = ShardingRegistry.getDsByTab(tabNameList.get(i));
			}
			dsArray[i] = dsName;
		}
		list.add(sqls);
		list.add(dsArray);
	}

	@SuppressWarnings("rawtypes")
	public static List<String[]> createSqlsForFull(String sql, Class entityClass) {

		List<String[]> list = new ArrayList<>();
		String tableName = _toTableName(entityClass); // orders[$#(index)#$]
		String baseTableName = tableName.replace(StringConst.ShardingTableIndexStr, "");
		List<PreparedValue> listValue = HoneyContext.justGetPreparedValue(sql);

		_createSqlsForFull(list, sql, listValue, baseTableName);

		return list;
	}

	static void _createSqlsForFull(List<String[]> list, String sql,
			List<PreparedValue> listValue, String baseTableName) {
		List<String> sqlList = new ArrayList<>();
		List<String> dsList = new ArrayList<>();

		Map<String, Set<String>> map = ShardingRegistry.getActualDataNodes(baseTableName);
		String tempSql;

		boolean justSomeDs = ShardingUtil.hadShardingSomeDsFullSelect();
		List<String> dsNameList = HoneyContext.getListLocal(StringConst.DsNameListLocal);

		for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
			String dsName = entry.getKey();
			if (justSomeDs && !isContain(dsNameList, dsName)) continue; // 只执行部分DS
			Set<String> tabIndexSet = entry.getValue();
			for (String tabIndex : tabIndexSet) {
//			    tempSql = sql.replace(tableName, tab); // eg: orders##(index)##替换成orders1等
				tempSql = sql.replace(StringConst.ShardingTableIndexStr, tabIndex); // 将下标占位符改为具体下标
				sqlList.add(tempSql);
				dsList.add(dsName);
				HoneyContext.setPreparedValue(tempSql, listValue);
			}
		}

		String sqls[] = ShardingUtil.list2Array(sqlList);
		String dsArray[] = ShardingUtil.list2Array(dsList);
		list.add(sqls);
		list.add(dsArray);
	}

	private static boolean isContain(List<String> dsNameList, String check) {
		for (int i = 0; i < dsNameList.size(); i++) {
			if (check.equalsIgnoreCase(dsNameList.get(i))) return true;
		}
		return false;
	}

	@SuppressWarnings("rawtypes")
	private static String _toTableName(Class entityClass) {
		return NameTranslateHandle.toTableName(entityClass.getName());
	}

}
