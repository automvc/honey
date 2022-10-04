/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.config;

import java.util.Map;
import java.util.Set;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class ShardingConfigMeta {
	public Map<String, Map<String, Set<String>>> actualDataNodes;// 1
	public Map<String, String> tabToDsMap; // 2
	public String tabBaseName;
	public Integer tabSize;
}
