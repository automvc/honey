/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.batch;

import java.util.ArrayList;
import java.util.List;

import org.teasoft.honey.osql.core.HoneyContext;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public abstract class ShardingBatchInsertTemplate<T> {

	int index;
	List<String> taskDs = new ArrayList<>();
	List<String> taskTab = new ArrayList<>();

	public abstract T shardingWork();

	public T doSharding() {
		try {
			HoneyContext.setSqlIndexLocal(index);
			HoneyContext.setAppointTab(taskTab.get(index));
			HoneyContext.setAppointDS(taskDs.get(index));

			return shardingWork();
			
		} finally {
			HoneyContext.removeAppointDS();
			HoneyContext.removeAppointTab();
			HoneyContext.removeSqlIndexLocal();
		}
	}
}
