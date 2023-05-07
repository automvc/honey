/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.teasoft.bee.osql.BeeException;
import org.teasoft.bee.osql.Registry;
import org.teasoft.bee.sharding.ShardingBean;
import org.teasoft.honey.osql.core.Logger;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class ShardingRegistry implements Registry {
	
	private static final Map<Class<?>, ShardingBean> shardingMap = new ConcurrentHashMap<>();  
	private static final Map<String, ShardingBean> shardingTableKeyMap = new ConcurrentHashMap<>();  
	private static Map<String, Map<String, Set<String>>> fullNodes = new HashMap<>();// 1
	private static Map<String, String> tabToDsMap = new LinkedHashMap<>(); // 2
	private static Map<String, Integer> tabSizeMap = new HashMap<>(); // 3
	
	private static final Byte ONE=1;
	private static Map<String, Byte> broadcastTabMap = new HashMap<>(); // 4
	
	public static ShardingBean getShardingBean(Class<?> entity) {
		return shardingMap.get(entity);
	}
	
	public static ShardingBean getShardingBean(String baseTableName) {
		return shardingTableKeyMap.get(baseTableName.toLowerCase());
	}

	
	public static String getDsShardingField(Class<?> entity) {
		ShardingBean bean = shardingMap.get(entity);
		if (bean != null) return bean.getDsField();
		return null;
	}

	public static String getTabShardingField(Class<?> entity) {
		ShardingBean bean = shardingMap.get(entity);
		if (bean != null) return bean.getTabField();
		return null;
	}

	/**
	 * 通过表名,获取数据源名称; 只分库时,只会返回最后一个数据源.
	 * @param tabName
	 * @return
	 */
	public static String getDsByTab(String tabName) {
		// 考虑ds0,ds1里的表名都是:orders0,orders1,orders2时,如何区分?? 不在此解析.获取不到,上游再判断??
		return tabToDsMap.get(tabName);
	}
	
	public static Integer getTabSize(String tabBaseName) {
		return tabSizeMap.get(tabBaseName);
	}

	/**
	 * 根据基本表名,找到与其关联的每个库所具有的表的下标.
	 * @param baseTableName
	 * @return
	 */
	public static Map<String, Set<String>> getFullNodes(String baseTableName) {
//		"orders":"ds0"->[orders0,orders1,orders2],"ds1"->[orders3,orders4,orders5];
//		return:  "ds0"->[orders0,orders1,orders2],"ds1"->[orders3,orders4,orders5]
		return fullNodes.get(baseTableName.toLowerCase());
	}
	
	private final static Random RAND=new Random();
	
	public static String getRandDs(String baseTableName) {
		Map<String, Set<String>> map = getFullNodes(baseTableName);
		if (map == null || map.size() < 1) return null;

		int size = map.size();
		int rand = RAND.nextInt(size);

		Set<String> keySet = map.keySet();
		int i = 0;
		for (String ds : keySet) {
			if (i == rand) return ds;
			i++;
		}

		return null;
	}
	
	public static List<String> getAllDs(String baseTableName) {
		Map<String, Set<String>> map = getFullNodes(baseTableName);
		if (map == null || map.size() < 1) return null;

		List<String> allDs = new ArrayList<>();
		Set<String> keysSet = map.keySet();
		for (String ds : keysSet) {
			allDs.add(ds);
		}
		return allDs;
	}
	
	public static boolean isBroadcastTab(String tabName) {
		return ONE.equals(broadcastTabMap.get(tabName.toLowerCase()));
	}
	
	
	static void register(Class<?> entity, List<ShardingBean> shardingBeanList) {
		for (ShardingBean shardingBean : shardingBeanList) {
			register(entity, shardingBean);
		}
	}

	static void register(Class<?> entity, ShardingBean shardingBean) {
		if (entity == null || shardingBean == null) return;
		shardingMap.put(entity, shardingBean);

		parseFullNodesStringAndregister(shardingBean.getFullNodes(),shardingBean.getTabAssignType(), entity);
	}
	
	static void register(String baseTableName, ShardingBean shardingBean) {
		if (baseTableName == null || shardingBean == null) return;
		shardingTableKeyMap.put(baseTableName.toLowerCase(), shardingBean);

		parseFullNodesStringAndregister(shardingBean.getFullNodes(),shardingBean.getTabAssignType(), null);
	}

	static void addTabToDsMap(Map<String, String> someTabToDsMap) {
		tabToDsMap.putAll(someTabToDsMap);
	}

	static void addFullNodes(Map<String, Map<String, Set<String>>> someNodes) {
		fullNodes.putAll(someNodes);
	}

	static void parseFullNodesStringAndregister(String fullNodes) {
		parseFullNodesStringAndregister(fullNodes,0, null);
	}

	private static void parseFullNodesStringAndregister(String fullNodes,int tabAssignType,
			Class<?> entity) {
		ShardingConfigParse t = new ShardingConfigParse();
		ShardingConfigMeta shardingConfigMeta = t.parseForSharding(fullNodes);
		if (shardingConfigMeta != null) {
			addFullNodes(shardingConfigMeta.getFullNodes());
			addTabToDsMap(shardingConfigMeta.getTabToDsMap());
			tabSizeMap.put(shardingConfigMeta.getTabBaseName(), shardingConfigMeta.getTabSize());
		} else {
			String msg = "Can not parse the fullNodes:" + fullNodes;
			if (entity != null) msg += "! Its entity name:" + entity.getName();
			Logger.warn(msg, new BeeException());
		}
	}
	
	static void addBroadcastTabList(List<String> broadTabList) {
		for (String tab : broadTabList) {
			broadcastTabMap.put(tab.toLowerCase(), ONE);
		}
	}
	
	static void addBroadcastTabList(String broadTab) {
		broadcastTabMap.put(broadTab.toLowerCase(), ONE);
	}

}
