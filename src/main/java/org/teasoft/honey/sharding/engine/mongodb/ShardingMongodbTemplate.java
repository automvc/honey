/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.mongodb;

import org.teasoft.honey.osql.core.HoneyContext;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public abstract class ShardingMongodbTemplate<T> {

	protected int index;
	protected String ds;
	protected String tab;

	public abstract T shardingWork();

	public T doSharding() {
		try {
			HoneyContext.setSqlIndexLocal(index);
			HoneyContext.setAppointDS(ds);
			HoneyContext.setAppointTab(tab);

			return shardingWork();
			
		} finally {
			HoneyContext.removeAppointTab();
			HoneyContext.removeAppointDS();
			HoneyContext.removeSqlIndexLocal();
		}

	}
}
