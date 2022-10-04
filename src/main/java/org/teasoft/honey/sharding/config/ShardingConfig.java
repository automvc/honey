/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.config;

import java.util.List;

import org.teasoft.bee.sharding.ShardingBean;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class ShardingConfig {


	public static void addShardingBean(Class<?> entity, ShardingBean shardingBean) {
		ShardingRegistry.register(entity, shardingBean);
	}
	
//	public static void addShardingBean(Class<?> entity, List<ShardingBean> shardingBeanList) {
//		ShardingRegistry.register(entity, shardingBeanList);
//	}


	public static void addBroadcastTabList(List<String> broadcastTabList) {
		ShardingRegistry.addBroadcastTabList(broadcastTabList);
	}

}
