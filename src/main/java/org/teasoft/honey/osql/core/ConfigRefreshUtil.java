/*
 * Copyright 2020-2025 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.Map;

import org.teasoft.bee.sharding.ShardingBean;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.sharding.config.ShardingConfig;
import org.teasoft.honey.util.StringUtils;

/**
 * @author Kingstar
 * @since  2.5.2
 */
public class ConfigRefreshUtil {
	
	private static boolean isSharding() {
		return HoneyConfig.getHoneyConfig().multiDS_enable && HoneyConfig.getHoneyConfig().getMultiDsSharding();
	}
	
	static void prcessShardingRuleInProperties() {
		
//		System.err.println("----------prcessShardingRuleInProperties----------loggerType: "+HoneyConfig.getHoneyConfig().getLoggerType());
		
		Map<String, Map<String, String>> shardingMap = HoneyConfig.getHoneyConfig().getSharding();

		if (shardingMap == null || shardingMap.isEmpty()) return;
		if (!isSharding()) return;

		for (Map.Entry<String, Map<String, String>> entry : shardingMap.entrySet()) {

			Map<String, String> map = entry.getValue();
			if (map == null || map.size() == 0) continue;

			String baseTableName = map.get("baseTableName");
			String className = map.get("className");

			boolean baseTableNameEmpty = false;
			boolean classNameEmpty = false;
			if (StringUtils.isBlank(baseTableName)) {
				baseTableNameEmpty = true;
			}
			if (StringUtils.isBlank(className)) {
				classNameEmpty = true;
			}

			if (baseTableNameEmpty && classNameEmpty) {
				Logger.warn("bee.db.sharding[" + entry.getKey() + "]" + "must define baseTableName or className");
			} else {
				if (!baseTableNameEmpty) {
					ShardingConfig.addShardingBean(baseTableName, new ShardingBean(entry.getValue()));
				} else if (!classNameEmpty) {
					try {
						ShardingConfig.addShardingBean(Class.forName(className), new ShardingBean(entry.getValue()));
					} catch (Exception e) {
						Logger.warn("Can not find the class: " + className, e);
					}
				}
			}
		}
	}
}
