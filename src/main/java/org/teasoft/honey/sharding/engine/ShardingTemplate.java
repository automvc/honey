/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine;

import org.teasoft.honey.osql.core.HoneyContext;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public abstract class ShardingTemplate<T> {

	protected int index;
	protected String ds;

	public abstract T shardingWork();

	public T doSharding() {
		try {
			HoneyContext.setSqlIndexLocal(index);
			HoneyContext.setAppointDS(ds);

			return shardingWork();

		} finally {
			HoneyContext.removeAppointDS();
			HoneyContext.removeSqlIndexLocal();
		}
	}
}
