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
import java.util.concurrent.Executors;

import org.teasoft.bee.mongodb.MongoSqlStruct;
import org.teasoft.bee.mongodb.MongodbBeeSql;
import org.teasoft.honey.osql.core.ShardingLogReg;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.sharding.engine.ResultMergeEngine;
import org.teasoft.honey.sharding.engine.decorate.ResultPagingDecorator;
import org.teasoft.honey.sharding.engine.decorate.SortStringArrayListDecorator;

/**
 * 分片的select操作
 * 专门为处理返回值是List<String[]>的类型
 * @author AiTeaSoft
 * @since  2.0
 */
public class MongodbShardingSelectListStringArrayEngine {

//	@SuppressWarnings("rawtypes")
	public <T> List<String[]> asynProcess(Class<T> entityClass, MongodbBeeSql mongodbBeeSql,MongoSqlStruct struct) {
		
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

		ExecutorService executor = Executors.newCachedThreadPool();
		CompletionService<List<String[]>> completionService = new ExecutorCompletionService<>(executor);
		final List<Callable<List<String[]>>> tasks = new ArrayList<>(); 

//		for (int i = 0; sqls != null && i < sqls.length; i++) {
//			tasks.add(new ShardingBeeSQLExecutorEngine(sqls[i], i + 1, beeSql, dsArray[i]));
//		}
		
		for (int i = 0; dsArray != null && i < dsArray.length; i++) {
			tasks.add(new ShardingBeeSQLExecutorEngine<T>(tabArray[i], i + 1, mongodbBeeSql,
					dsArray[i], entityClass, struct));
		}
		
		if (dsArray != null) ShardingLogReg.log(dsArray.length);
		
//		Bee SQL Executor Engine
		int size=tasks.size();
		for (int i = 0; tasks != null && i < size; i++) {
			completionService.submit(tasks.get(i));
		}

		//Result Merge
		List<String[]> rsList = ResultMergeEngine.merge(completionService, size);

		executor.shutdown();
		
		// 排序装饰
		SortStringArrayListDecorator.sort(rsList);

		// 分页装饰
		// 获取指定的一页数据
		ResultPagingDecorator.pagingList(rsList);
		
		return rsList;
	}


	private class ShardingBeeSQLExecutorEngine<T>
			extends ShardingAbstractMongoBeeSQLExecutorEngine<List<String[]>> {

		private Class<T> entityClass;
		private MongoSqlStruct struct;

		public ShardingBeeSQLExecutorEngine(String tab, int index, MongodbBeeSql mongodbBeeSql,
				String ds, Class<T> entityClass, MongoSqlStruct struct) {
			super(tab, index, mongodbBeeSql, ds);
			this.entityClass = entityClass;
			this.struct = struct.copy();
			this.struct.setTableName(tab);
		}

		public List<String[]> shardingWork() {
			ShardingLogReg.regShardingSqlLog("select SQL", index, tab);
			return mongodbBeeSql.selectString(struct, entityClass);
		}
	}

}
