/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.DatabaseConst;
import org.teasoft.bee.osql.OrderType;
import org.teasoft.bee.sharding.ShardingSortStruct;
import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.StringConst;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class ShardingUtil {

	private static Map<String, Integer> firstIndexMap;
	private static final int ZERO;
	private static final int ONE;

	static {
		ZERO = 0;
		ONE = 1;
		firstIndexMap = new HashMap<>();
		firstIndexMap.put(DatabaseConst.MYSQL.toLowerCase(), ZERO);
		firstIndexMap.put(DatabaseConst.MariaDB.toLowerCase(), ZERO);
		firstIndexMap.put(DatabaseConst.ORACLE.toLowerCase(), ONE);
	}

	public static int firstRecordIndex() {
		String dbName = HoneyConfig.getHoneyConfig().getDbName();
		Integer i = firstIndexMap.get(dbName.toLowerCase());
		if (i != null) return i;

		return 0;
	}
	
	public static String[] list2Array(List<String> entityList) {
		int len = entityList.size();
		String entity[] = new String[len];

		for (int i = 0; i < len; i++) {
			entity[i] = entityList.get(i);
		}
		return entity;
	}
	
	public static boolean isSharding() {
		return HoneyContext.isMultiDs() && HoneyConfig.getHoneyConfig().multiDS_sharding;
	}
	
	public static boolean hadSharding() {//要分片,且有分片
		return isSharding() && isTrueInSysCommStrLocal(StringConst.HadSharding);
	}
	
	public static boolean hadShardingFullSelect() {//要分片,且要全域查询
		return isSharding() && isTrueInSysCommStrLocal(StringConst.ShardingFullSelect);
	}
	
	public static boolean hadShardingSomeDsFullSelect() {//分片值只计算得数据源名称,应该查其下的所有表.
		return isSharding() && isTrueInSysCommStrLocal(StringConst.ShardingSomeDsFullSelect);
	}
	
	public static boolean isMoreTableQuery() {
//		return StringConst.tRue.equals(HoneyContext.getSysCommStrLocal(StringConst.MoreTableQuery));
		return isTrueInSysCommStrLocal(StringConst.MoreTableQuery);
	}
	
	public static void setTrueInSysCommStrLocal(String key) {
		HoneyContext.setSysCommStrLocal(key, StringConst.tRue);
	}
	
	public static boolean isTrueInSysCommStrLocal(String key) {
		return HoneyContext.isTrueInSysCommStrLocal(key);
	}
	
	public static boolean isShardingBatchInsertDoing() {
		return isTrueInSysCommStrLocal(StringConst.ShardingBatchInsertDoing);
	}
	
	public static int hashInt(String str) {
		if (str == null) return 0;
		int a = str.hashCode();
		return a < 0 ? -a : a;
	}
	
	public static ShardingSortStruct parseOrderByMap(Map<String, String> orderByMap) {
		String orderFields[] = new String[orderByMap.size()];
		OrderType[] orderTypes = new OrderType[orderByMap.size()];
		int lenA = orderFields.length;
		String orderBy = "";
		int i = 0;
		for (Map.Entry<String, String> entry : orderByMap.entrySet()) {
			String fName = entry.getKey();
			String orderType = entry.getValue();
			orderFields[i] = fName;
			if (OrderType.DESC.getName().equals(orderType))
				orderTypes[i] = OrderType.DESC;
			else
				orderTypes[i] = OrderType.ASC;
			orderBy += fName + " " + orderType;
			if (i < lenA - 1) orderBy += ",";
			i++;
		}
		
		ShardingSortStruct struct = new ShardingSortStruct(orderBy, orderFields, orderTypes);
		return struct;
	}

}
