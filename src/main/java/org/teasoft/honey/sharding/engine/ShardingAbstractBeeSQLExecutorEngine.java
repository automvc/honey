/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine;

import java.util.concurrent.Callable;

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.Serializer;
import org.teasoft.honey.osql.core.JdkSerializer;
import org.teasoft.honey.osql.core.Logger;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public abstract class ShardingAbstractBeeSQLExecutorEngine<T> extends ShardingTemplate<T> implements Callable<T> {

	protected String sql;
	protected BeeSql beeSql;

	public ShardingAbstractBeeSQLExecutorEngine(String sql, int index, BeeSql beeSql, String ds) {
		this.sql = sql;
//		this.beeSql = beeSql;
		this.beeSql = copy(beeSql);

		super.index = index;
		super.ds = ds;
	}

	@Override
	public T call() throws Exception {
		return doSharding();
	}

	private BeeSql copy(BeeSql beeSql) {
		try {
			Serializer jdks = new JdkSerializer();
			return (BeeSql) jdks.unserialize(jdks.serialize(beeSql));
		} catch (Exception e) {
			Logger.debug(e.getMessage(), e);
		}
		return beeSql; // 有异常返回原来的
	}
}
