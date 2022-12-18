/*
 * Copyright 2016-2022 the original author.All rights reserved.
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

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.spi.JsonTransform;
import org.teasoft.honey.osql.core.JsonResultWrap;
import org.teasoft.honey.osql.core.OrderByPagingRewriteSql;
import org.teasoft.honey.osql.core.ShardingLogReg;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.sharding.engine.ResultMergeEngine;
import org.teasoft.honey.sharding.engine.ShardingAbstractBeeSQLExecutorEngine;
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
	public JsonResultWrap asynProcess(String sql, BeeSql beeSql, int opType,
			Class entityClass) {

		List<String[]> list;
		String sqls[] = null;
		String dsArray[] = null;

		if (ShardingUtil.hadShardingFullSelect()) {// 全域查询
			list = OrderByPagingRewriteSql.createSqlsForFullSelect(sql, entityClass);
		} else {
			list = OrderByPagingRewriteSql.createSqlsAndInit(sql); // 涉及部分分片
		}

		sqls = list.get(0);
		dsArray = list.get(1);

		ExecutorService executor = Executors.newCachedThreadPool();
//		ExecutorService executor = Executors.newWorkStealingPool(3);  //jdk 1.8 
		CompletionService<String> completionService = new ExecutorCompletionService<>(executor);
		final List<Callable<String>> tasks = new ArrayList<>(); // 构造任务

		for (int i = 0; sqls != null && i < sqls.length; i++) {
			tasks.add(new ShardingBeeSQLJsonExecutorEngine(sqls[i], i + 1, beeSql, dsArray[i]));
		}

		if (sqls != null) ShardingLogReg.log(sqls.length);

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

			// 排序后,要将数据放缓存. TODO

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
			extends ShardingAbstractBeeSQLExecutorEngine<String> {

		public ShardingBeeSQLJsonExecutorEngine(String sql, int index, BeeSql beeSql, String ds) {
			super(sql, index, beeSql, ds);
		}

		public String shardingWork() {
			ShardingLogReg.regShardingSqlLog("selectJson SQL", index, sql);
			return beeSql.selectJson(this.sql);
		}
	}

}
