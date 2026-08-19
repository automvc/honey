/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.decorate;

import org.teasoft.bee.sharding.ShardingSortStruct;
import org.teasoft.honey.osql.core.HoneyContext1;
import org.teasoft.honey.osql.core.K;
import org.teasoft.honey.osql.core.StringConst;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class OrderBySqlDecorator {

	public static String addOrderBy(String sql) {
//		ShardingSortStruct shardingSort = HoneyContext.getCurrentShardingSort();
		ShardingSortStruct shardingSort = (ShardingSortStruct)HoneyContext1.getExtForSharding(StringConst.ShardingSortStruct);
		if (shardingSort != null) {
			sql += K.space + K.orderBy + K.space + shardingSort.getOrderSql();
		}
		return sql;
	}

}
