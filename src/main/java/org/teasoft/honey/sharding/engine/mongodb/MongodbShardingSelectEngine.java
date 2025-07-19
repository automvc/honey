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

import org.teasoft.bee.mongodb.MongoSqlStruct;
import org.teasoft.bee.mongodb.MongodbBeeSql;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.core.ShardingLogReg;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.sharding.engine.ResultMergeEngine;
import org.teasoft.honey.sharding.engine.ThreadPoolUtil;
import org.teasoft.honey.sharding.engine.decorate.ResultPagingDecorator;
import org.teasoft.honey.sharding.engine.decorate.SortListDecorator;

/**
 * 分片的select操作
 * 返回类型是List<T>
 * @author AiTeaSoft
 * @since  2.0
 */
public class MongodbShardingSelectEngine {

	private boolean showShardingSQL = getShowShardingSQL();

	private boolean getShowShardingSQL() {
		return HoneyConfig.getHoneyConfig().showSQL && HoneyConfig.getHoneyConfig().showShardingSQL;
	}

	public <T> List<T> asynProcess(Class<T> entityClass, MongodbBeeSql mongodbBeeSql, MongoSqlStruct struct) {

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

		// 分页
		// ShardingSortStruct 收集排序的信息

		final List<Callable<List<T>>> tasks = new ArrayList<>();

		for (int i = 0; dsArray != null && i < dsArray.length; i++) {
			tasks.add(new ShardingBeeSQLExecutorEngine<T>(tabArray[i], i + 1, mongodbBeeSql, dsArray[i], entityClass,
					struct));
		}

		if (dsArray != null) ShardingLogReg.log(dsArray.length);

		int size = tasks.size();
		if (size == 0) return null;

//		Bee SQL Executor Engine
		ExecutorService executor = ThreadPoolUtil.getThreadPool(size);
		CompletionService<List<T>> completionService = new ExecutorCompletionService<>(executor);
		for (int i = 0; tasks != null && i < size; i++) {
			completionService.submit(tasks.get(i));
		}

		// Result Merge
		List<T> rsList = ResultMergeEngine.merge(completionService, size);

		executor.shutdown();

		// 排序装饰
		SortListDecorator.sort(rsList);

		if (showShardingSQL) Logger.debug("before ResultPagingDecorator, rows: " + rsList.size());

		// 分页装饰
		// 获取指定的一页数据
		ResultPagingDecorator.pagingList(rsList);

		if (showShardingSQL) Logger.debug("after  ResultPagingDecorator, rows: " + rsList.size());

		return rsList;
	}

	private class ShardingBeeSQLExecutorEngine<T> extends ShardingAbstractMongoBeeSQLExecutorEngine<List<T>> {

		private Class<T> entityClass;
		private MongoSqlStruct struct;

		public ShardingBeeSQLExecutorEngine(String tab, int index, MongodbBeeSql mongodbBeeSql, String ds,
				Class<T> entityClass, MongoSqlStruct struct) {
			super(tab, index, mongodbBeeSql, ds);
			this.entityClass = entityClass;
			this.struct = struct.copy();
			this.struct.setTableName(tab); // reset tab to really tableName
		}

		public List<T> shardingWork() {
			ShardingLogReg.regShardingSqlLog("select SQL", index, tab);
			return mongodbBeeSql.select(struct, entityClass);
		}
	}

}
