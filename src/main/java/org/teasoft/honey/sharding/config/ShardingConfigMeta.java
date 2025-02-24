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
	// 分隔符,只支持"_"; "-"很多数据库都不支持，不要用
	private String sepTab = ""; // separator between table and index, like orders_1; but recommand use orders1

	private Map<String, String> tabToBase; // eg: orders1->orders

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

	public String getSepTab() {
		return sepTab;
	}

	public void setSepTab(String sepTab) {
		this.sepTab = sepTab;
	}

	public Map<String, String> getTabToBase() {
		return tabToBase;
	}

	public void setTabToBase(Map<String, String> tabToBase) {
		this.tabToBase = tabToBase;
	}

}
