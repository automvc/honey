/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.decorate;

import org.teasoft.bee.sharding.ShardingSortStruct;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.K;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class OrderBySqlDecorator {

	public static String addOrderBy(String sql) {
		ShardingSortStruct shardingSort = HoneyContext.getCurrentShardingSort();
		if (shardingSort != null) {
			sql += K.space + K.orderBy + K.space + shardingSort.getOrderSql();
		}
		return sql;
	}

}
