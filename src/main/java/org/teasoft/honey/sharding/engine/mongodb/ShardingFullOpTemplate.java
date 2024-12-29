/*
 * Copyright 2019-2025 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.mongodb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.teasoft.bee.osql.OneMethod;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.StringConst;
import org.teasoft.honey.sharding.config.ShardingRegistry;

/**
 * 在单线程环境下，对所有分片执行操作.
 * @author Kingstar
 * @since  2.5.2
 */
public class ShardingFullOpTemplate<T> {

	protected String ds;
	protected String tab;

	private String baseTableName;
	private OneMethod<T> oneMethod;

	public ShardingFullOpTemplate(String baseTableName, OneMethod<T> oneMethod) {
		if (baseTableName.endsWith(StringConst.ShardingTableIndexStr)) {
			baseTableName = baseTableName.replace(StringConst.ShardingTableIndexStr, "");
		}
		this.baseTableName = baseTableName;
		this.oneMethod = oneMethod;
	}

	public T doSharding() {
		try {
			HoneyContext.setAppointDS(ds);
			HoneyContext.setAppointTab(tab);

			return oneMethod.doOneMethod();

		} finally {
			HoneyContext.removeAppointTab();
			HoneyContext.removeAppointDS();
		}

	}

	public List<T> execute() {
		Map<String, Set<String>> map = ShardingRegistry.getFullNodes(this.baseTableName);
		List<T> list = new ArrayList<>();

		for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
			String dsName = entry.getKey();
			Set<String> tabIndexSet = entry.getValue();
			for (String tabIndex : tabIndexSet) {
				this.ds = dsName;
				this.tab = baseTableName + tabIndex;
				T t = doSharding();
				list.add(t);
			}
		}
		return list;
	}

}
