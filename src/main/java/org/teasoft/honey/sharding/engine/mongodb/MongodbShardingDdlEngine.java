/*
 * Copyright 2016-2023 the original author.All rights reserved.
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

import org.teasoft.bee.mongodb.MongodbBeeSql;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.core.ShardingLogReg;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.sharding.engine.ThreadPoolUtil;

/**
 * DDL创建表
 * @author AiTeaSoft
 * @since  2.0
 */
public class MongodbShardingDdlEngine {

	public <T> boolean asynProcess(Class<T> entityClass, MongodbBeeSql mongodbBeeSql, boolean isDropExistTable) {

		List<String[]> list;
		String dsArray[];
		String tabArray[];

		if (ShardingUtil.hadShardingFullSelect()) {// 全域查询 或某些DS的某表全查询
			list = MongodbShardingRouter._findDsTabForFull(entityClass);
		} else {
			list = MongodbShardingRouter._findDsTab(); // 涉及部分分片
		}
		dsArray = list.get(0);
		tabArray = list.get(1);

		final List<Callable<Boolean>> tasks = new ArrayList<>();

		for (int i = 0; dsArray != null && i < dsArray.length; i++) {
			tasks.add(new ShardingBeeSQLExecutorEngine<T>(tabArray[i], i + 1, mongodbBeeSql, dsArray[i], entityClass,
					isDropExistTable));
		}

		if (dsArray != null) ShardingLogReg.log(dsArray.length);

		int size = tasks.size();
		if (size == 0) return false;

//		Bee SQL Executor Engine
		ExecutorService executor = ThreadPoolUtil.getThreadPool(dsArray.length);
		CompletionService<Boolean> completionService = new ExecutorCompletionService<>(executor);
		for (int i = 0; tasks != null && i < size; i++) {
			completionService.submit(tasks.get(i));
		}

		// Result Merge
		boolean f = false;
		for (int i = 0; i < size; i++) {
			try {
				boolean t = completionService.take().get();
				if (i == 0) f = t;
				else f = f && t;
			} catch (Exception e) {
				Logger.warn(e.getMessage(), e);
				Thread.currentThread().interrupt();
			}
		}
		executor.shutdown();

		return f;
	}

	private class ShardingBeeSQLExecutorEngine<T> extends ShardingAbstractMongoBeeSQLExecutorEngine<Boolean> {

		private Class<T> entityClass;
		private boolean isDropExistTable;

		public ShardingBeeSQLExecutorEngine(String tab, int index, MongodbBeeSql mongodbBeeSql, String ds,
				Class<T> entityClass, boolean isDropExistTable) {
			super(tab, index, mongodbBeeSql, ds);
			this.entityClass = entityClass;
			this.isDropExistTable = isDropExistTable;
		}

		public Boolean shardingWork() {
			ShardingLogReg.regShardingSqlLog("DDL SQL", index, tab);
			return mongodbBeeSql.createTable(entityClass, isDropExistTable);
		}
	}

}
