/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.decorate;

import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.sharding.ShardingPageStruct;
import org.teasoft.honey.osql.core.BeeFactory;
import org.teasoft.honey.osql.core.HoneyContext;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class PagingSqlDecorator {

	public static String addPaging(String sql) {
		ShardingPageStruct shardingPage = HoneyContext.getCurrentShardingPage();
		if (shardingPage == null) return sql; //2.4.2
		
		int start = shardingPage.getStart();
		int size = shardingPage.getSize();
		if (start == -1)
			sql = getDbFeature().toPageSql(sql, size);
		else
			sql = getDbFeature().toPageSql(sql, start, size);

		return sql;
	}

	private static DbFeature getDbFeature() {
		return BeeFactory.getHoneyFactory().getDbFeature();
	}

}
