/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.mongodb;

import java.util.concurrent.Callable;

import org.teasoft.bee.mongodb.MongodbBeeSql;
import org.teasoft.bee.osql.Serializer;
import org.teasoft.honey.osql.core.JdkSerializer;
import org.teasoft.honey.osql.core.Logger;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public abstract class ShardingAbstractMongoBeeSQLExecutorEngine<T> extends ShardingMongodbTemplate<T>
		implements Callable<T> {

	protected MongodbBeeSql mongodbBeeSql;

	public ShardingAbstractMongoBeeSQLExecutorEngine(String tab, int index, MongodbBeeSql mongodbBeeSql,
			String ds) {
		super.tab = tab;
		this.mongodbBeeSql = copy(mongodbBeeSql);

		super.index = index;
		super.ds = ds;
	}

	@Override
	public T call() throws Exception {
		return doSharding();
	}
	
	private MongodbBeeSql copy(MongodbBeeSql mongodbBeeSql) {
		try {
			Serializer jdks = new JdkSerializer();
			return (MongodbBeeSql) jdks.unserialize(jdks.serialize(mongodbBeeSql));
		} catch (Exception e) {
			Logger.debug(e.getMessage(), e);
		}
		return mongodbBeeSql; //有异常返回原来的
	}
}
