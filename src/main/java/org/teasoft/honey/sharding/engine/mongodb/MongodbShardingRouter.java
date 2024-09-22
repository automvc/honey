/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.mongodb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.NameTranslateHandle;
import org.teasoft.honey.osql.core.StringConst;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.sharding.config.ShardingRegistry;
import org.teasoft.honey.util.StringUtils;

/**
 * @author Kingstar
 * @since  2.0
 */
public class MongodbShardingRouter {

	// 找表排列对应的ds排列
	static List<String[]> _findDsTab() {

		List<String> tabNameList = HoneyContext.getListLocal(StringConst.TabNameListLocal);
		List<String> tabSuffixList = HoneyContext.getListLocal(StringConst.TabSuffixListLocal);
		Map<String, String> tab2DsMap = HoneyContext
				.getCustomMapLocal(StringConst.ShardingTab2DsMap);

		String dsArray[] = new String[tabSuffixList.size()];
		for (int i = 0; i < tabSuffixList.size(); i++) {
//			String dsName = tab2DsMap.get(tabSuffixList.get(i)); // 只在使用注解时, 分库与分表同属于一个分片键,才有用. 
//			if (StringUtils.isBlank(dsName)) {
//				dsName = ShardingRegistry.getDsByTab(tabNameList.get(i));
//			}
			String dsName=ShardingUtil.findDs(tab2DsMap, tabSuffixList.get(i), tabNameList.get(i));
			dsArray[i] = dsName;
		}

		String tabArray[] = StringUtils.listToArray(tabNameList);
		List<String[]> list = new ArrayList<>();

		list.add(dsArray);
		list.add(tabArray);
		return list;
	}

	@SuppressWarnings("rawtypes")
	private static String _toTableName(Class entityClass) {
		return NameTranslateHandle.toTableName(entityClass.getName());
	}

	@SuppressWarnings("rawtypes")
	static List<String[]> _findDsTabForFull(Class entityClass) {

		String baseTableName = _toTableName(entityClass);
		
		List<String> dsList = new ArrayList<>();
		List<String> tabList = new ArrayList<>();

		Map<String, Set<String>> map = ShardingRegistry.getFullNodes(baseTableName);

		boolean justSomeDs = ShardingUtil.hadShardingSomeDsFullSelect();
		List<String> dsNameList = HoneyContext.getListLocal(StringConst.DsNameListLocal);

		for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
			String dsName = entry.getKey();
			if (justSomeDs && !isContain(dsNameList, dsName)) continue; // 只执行部分DS
			Set<String> tabIndexSet = entry.getValue();
			for (String tabIndex : tabIndexSet) {
				dsList.add(dsName);
				tabList.add(baseTableName + tabIndex);  //加分隔 ? ,不用另外加,  FullNodes的已加有
			}
		}

//		String sqls[] = ShardingUtil.list2Array(sqlList);
		String dsArray[] = StringUtils.listToArray(dsList);
		String tabArray[] = StringUtils.listToArray(tabList);

		List<String[]> list = new ArrayList<>();
		list.add(dsArray);
		list.add(tabArray);

		return list;
	}

	private static boolean isContain(List<String> dsNameList, String check) {
		for (int i = 0; i < dsNameList.size(); i++) {
			if (check.equalsIgnoreCase(dsNameList.get(i))) return true;
		}
		return false;
	}

}
