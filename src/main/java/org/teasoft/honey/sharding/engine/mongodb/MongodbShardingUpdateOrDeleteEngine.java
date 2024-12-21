/*
 * Copyright 2020-2025 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.mongodb;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;

import org.teasoft.bee.mongodb.MongoSqlStruct;
import org.teasoft.bee.mongodb.MongodbBeeSql;
import org.teasoft.honey.osql.core.ShardingLogReg;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.sharding.engine.ResultMergeEngine;
import org.teasoft.honey.sharding.engine.ThreadPoolUtil;

/**
 * Update or Delete Sharing Engine for MongoDB.
 * @author Kingstar
 * @since  2.5.0
 */
public class MongodbShardingUpdateOrDeleteEngine {

	@SuppressWarnings("rawtypes")
	public Integer asynProcess(MongodbBeeSql mongodbBeeSql, MongoSqlStruct struct) {

		List<String[]> list;
		String dsArray[];
		String tabArray[];

		Class entityClass = struct.getEntityClass();

		if (ShardingUtil.hadShardingFullSelect()) {// 全域查询 或某些DS的某表全查询
			list = MongodbShardingRouter._findDsTabForFull(entityClass);
		} else {
			list = MongodbShardingRouter._findDsTab(); // 涉及部分分片
		}
		dsArray = list.get(0);
		tabArray = list.get(1);

		final List<Callable<Integer>> tasks = new ArrayList<>();

		for (int i = 0; dsArray != null && i < dsArray.length; i++) {
			tasks.add(
					new ShardingBeeSQLExecutorEngine(tabArray[i], i + 1, mongodbBeeSql, dsArray[i], entityClass, struct));
		}

		if (dsArray != null) ShardingLogReg.log(dsArray.length);

		int size = tasks.size();
		if (size == 0) return null;

//		Bee SQL Executor Engine
		ExecutorService executor = ThreadPoolUtil.getThreadPool(size);
		CompletionService<Integer> completionService = new ExecutorCompletionService<>(executor);
		for (int i = 0; tasks != null && i < size; i++) {
			completionService.submit(tasks.get(i));
		}

		// Merge Result
		int r = ResultMergeEngine.mergeInteger(completionService, size);

		executor.shutdown();

		return r;
	}

	private class ShardingBeeSQLExecutorEngine<T> extends ShardingAbstractMongoBeeSQLExecutorEngine<Integer> {

		private Class entityClass;
		private MongoSqlStruct struct;

		public ShardingBeeSQLExecutorEngine(String tab, int index, MongodbBeeSql mongodbBeeSql, String ds,
				Class entityClass, MongoSqlStruct struct) {
			super(tab, index, mongodbBeeSql, ds);
			this.entityClass = entityClass;
			this.struct = struct.copy();
			this.struct.setTableName(tab);
		}

		public Integer shardingWork() {
			ShardingLogReg.regShardingSqlLog("modify SQL", index, tab);
			return mongodbBeeSql.updateOrDelete(struct);
		}
	}

}
