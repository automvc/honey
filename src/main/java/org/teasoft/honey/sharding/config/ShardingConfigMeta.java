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

	private Map<String, Map<String, Set<String>>> fullNodes;// 1
	private Map<String, String> tabToDsMap; // 2
	private String tabBaseName;
	private Integer tabSize;

	public Map<String, Map<String, Set<String>>> getFullNodes() {
		return fullNodes;
	}

	public void setFullNodes(Map<String, Map<String, Set<String>>> fullNodes) {
		this.fullNodes = fullNodes;
	}

	public Map<String, String> getTabToDsMap() {
		return tabToDsMap;
	}

	public void setTabToDsMap(Map<String, String> tabToDsMap) {
		this.tabToDsMap = tabToDsMap;
	}

	public String getTabBaseName() {
		return tabBaseName;
	}

	public void setTabBaseName(String tabBaseName) {
		this.tabBaseName = tabBaseName;
	}

	public Integer getTabSize() {
		return tabSize;
	}

	public void setTabSize(Integer tabSize) {
		this.tabSize = tabSize;
	}
}
