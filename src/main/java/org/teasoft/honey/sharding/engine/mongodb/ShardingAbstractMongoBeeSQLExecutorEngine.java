/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.mongodb;

import java.util.concurrent.Callable;

import org.teasoft.bee.mongodb.MongodbBeeSql;
import org.teasoft.bee.osql.Serializer;
import org.teasoft.honey.osql.core.JdkSerializer;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.sharding.engine.ShardingTemplate;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public abstract class ShardingAbstractMongoBeeSQLExecutorEngine<T> extends ShardingTemplate<T>
		implements Callable<T> {

	protected String sql;
	protected MongodbBeeSql beeSql;

	public ShardingAbstractMongoBeeSQLExecutorEngine(String sql, int index, MongodbBeeSql beeSql,
			String ds) {
		this.sql = sql;
		this.beeSql = copy(beeSql);

		super.index = index;
		super.ds = ds;
	}

	@Override
	public T call() throws Exception {
		return doSharding();
	}
	
	private MongodbBeeSql copy(MongodbBeeSql beeSql) {
		try {
			Serializer jdks = new JdkSerializer();
			return (MongodbBeeSql) jdks.unserialize(jdks.serialize(beeSql));
		} catch (Exception e) {
			Logger.debug(e.getMessage(), e);
		}
		return beeSql; //有异常返回原来的
	}
}
