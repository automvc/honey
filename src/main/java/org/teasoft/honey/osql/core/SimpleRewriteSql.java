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

import org.teasoft.bee.osql.exception.ShardingErrorException;
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
		for (int i = 0; i < tabSuffixList.size(); i++) { //有tabSuffixList， sql可以不用加前缀 2.2; 但用并行流parallelStream()会出问题,还是加上前缀好; 
			sqls[i] = sql.replace(StringConst.ShardingTableIndexStr, tabSuffixList.get(i)); // eg: [$#(index)#$]替换成下标等
			sqls[i]=HoneyUtil.getRandomPrefix()+sqls[i]; //2.2  //因parallelStream()+InheritableThreadLocal会有问题,但分片又必要要用InheritableThreadLocal,所以分片时,不支持parallelStream并行操作
			HoneyContext.setPreparedValue(sqls[i], listValue);
//			String dsName = tab2DsMap.get(tabSuffixList.get(i)); // 只在使用注解时, 分库与分表同属于一个分片键,才有用.
//			if (StringUtils.isBlank(dsName)) {
//				dsName = ShardingRegistry.getDsByTab(tabNameList.get(i));
//			}
			String dsName=ShardingUtil.findDs(tab2DsMap, tabSuffixList.get(i), tabNameList.get(i));
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

	static void _createSqlsForFull(List<String[]> list, String sql, List<PreparedValue> listValue, String baseTableName) {
		List<String> sqlList = new ArrayList<>();
		List<String> dsList = new ArrayList<>();

		Map<String, Set<String>> map = ShardingRegistry.getFullNodes(baseTableName);
		String tempSql;

		boolean justSomeDs = ShardingUtil.hadShardingSomeDsFullSelect();
		List<String> dsNameList = HoneyContext.getListLocal(StringConst.DsNameListLocal);

		//fixed bug 2.1
		if(map==null || map.size()<=0) throw new ShardingErrorException("Can not find the FullNodes by baseTableName:"+baseTableName);
			
		for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
			String dsName = entry.getKey();
			if (justSomeDs && !isContain(dsNameList, dsName)) continue; // 只执行部分DS
			Set<String> tabIndexSet = entry.getValue();
			for (String tabIndex : tabIndexSet) {
//			    tempSql = sql.replace(tableName, tab); // eg: orders##(index)##替换成orders1等
				tempSql = sql.replace(StringConst.ShardingTableIndexStr, tabIndex); // 将下标占位符改为具体下标
				tempSql=HoneyUtil.getRandomPrefix()+tempSql; //2.2
				sqlList.add(tempSql);
				dsList.add(dsName);
				HoneyContext.setPreparedValue(tempSql, listValue);
			}
		}

		String sqls[] = StringUtils.listToArray(sqlList);
		String dsArray[] = StringUtils.listToArray(dsList);
		list.add(sqls);
		list.add(dsArray);
	}

	private static boolean isContain(List<String> dsNameList, String check) {
		if(dsNameList==null) return false; //v2.1.5.1
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
