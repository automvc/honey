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
import org.teasoft.bee.spi.JsonTransform;
import org.teasoft.honey.osql.core.JsonResultWrap;
import org.teasoft.honey.osql.core.ShardingLogReg;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.sharding.engine.ResultMergeEngine;
import org.teasoft.honey.sharding.engine.decorate.ResultPagingDecorator;
import org.teasoft.honey.sharding.engine.decorate.SortListDecorator;
import org.teasoft.honey.spi.SpiInstanceFactory;
import org.teasoft.honey.util.ObjectUtils;

/**
 * 分片的select操作
 * 主要是对函数操作和selectJson的分片.
 * @author AiTeaSoft
 * @since  2.0
 */
public class MongodbShardingSelectJsonEngine {

	/**
	 * @param sql
	 * @param beeSql
	 * @param entityClass
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JsonResultWrap asynProcess(Class entityClass, MongodbBeeSql mongodbBeeSql,MongoSqlStruct struct) {

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
		CompletionService<String> completionService = new ExecutorCompletionService<>(executor);
		final List<Callable<String>> tasks = new ArrayList<>(); 

		for (int i = 0; dsArray != null && i < dsArray.length; i++) {
			tasks.add(new ShardingBeeSQLJsonExecutorEngine(tabArray[i], i + 1, mongodbBeeSql,
					dsArray[i], entityClass, struct));
		}

		if (dsArray != null) ShardingLogReg.log(dsArray.length);

		int size=tasks.size();
		for (int i = 0; tasks != null && i < size; i++) {
			completionService.submit(tasks.get(i));
		}

		List<String> rsList =ResultMergeEngine.mergeJsonResult(completionService, size);

		executor.shutdown();

		String json = "";
		int rowCount;
		if (rsList.size() == 1) {
			json = rsList.get(0);
			rowCount = 1;
		} else if (rsList.size() == 0) {
			json = "[]";
			rowCount = 0;
		} else {
			JsonTransform jsonTransform = SpiInstanceFactory.getJsonTransform();

			List entityList = new ArrayList<>();
			for (int i = 0; i < rsList.size(); i++) {
				if (ObjectUtils.isNotEmpty(rsList.get(i)) && !"[]".equals(rsList.get(i))) {
					entityList.addAll(jsonTransform.toEntity(rsList.get(i), List.class, entityClass));
				}
			}

			// 排序装饰
			SortListDecorator.sort(entityList);

			// 分页装饰
			// 获取指定的一页数据
			ResultPagingDecorator.pagingList(entityList);

			json = jsonTransform.toJson(entityList);
			rowCount = entityList.size();
		}

		JsonResultWrap wrap = new JsonResultWrap();
		wrap.setResultJson(json.toString());
		wrap.setRowCount(rowCount);

		return wrap;
	}
	
//	Return String 
	private class ShardingBeeSQLJsonExecutorEngine
			extends ShardingAbstractMongoBeeSQLExecutorEngine<String> 
	{
		private Class entityClass;
		private MongoSqlStruct struct;

		public ShardingBeeSQLJsonExecutorEngine(String tab, int index, MongodbBeeSql mongodbBeeSql,
				String ds, Class entityClass, MongoSqlStruct struct) {
			super(tab, index, mongodbBeeSql, ds);
			this.entityClass = entityClass;
			this.struct = struct.copy();
			this.struct.setTableName(tab);
		}

		public String shardingWork() {
			ShardingLogReg.regShardingSqlLog("selectJson SQL", index, tab);
			return mongodbBeeSql.selectJson(struct,entityClass);
		}
	}

}
