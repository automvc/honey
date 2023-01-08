/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.osql.core.OrderByPagingRewriteSql;
import org.teasoft.honey.osql.core.ShardingLogReg;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.sharding.engine.decorate.ResultPagingDecorator;
import org.teasoft.honey.sharding.engine.decorate.ShardingGroupByDecorator;
import org.teasoft.honey.sharding.engine.decorate.SortListDecorator;

/**
 * 分片的select操作
 * 返回类型是List<T>
 * @author AiTeaSoft
 * @since  2.0
 */
public class ShardingSelectEngine {
	
	private boolean showShardingSQL = getShowShardingSQL();
	
	private boolean getShowShardingSQL() {
		return HoneyConfig.getHoneyConfig().showSQL && HoneyConfig.getHoneyConfig().showShardingSQL;
	}
	
	public <T> List<T> asynProcess(String sql, Class<T> entityClass, BeeSql beeSql) {

		List<String[]> list;
		String sqls[] = null;
		String dsArray[] = null;

		if (ShardingUtil.hadShardingFullSelect()) {// 全域查询 或某些DS的某表全查询
			list = OrderByPagingRewriteSql.createSqlsForFullSelect(sql, entityClass);
		} else {
			list = OrderByPagingRewriteSql.createSqlsAndInit(sql); // 涉及部分分片
		}
		sqls = list.get(0);
		dsArray = list.get(1);

		ExecutorService executor = Executors.newCachedThreadPool();
		CompletionService<List<T>> completionService = new ExecutorCompletionService<>(executor);
		final List<Callable<List<T>>> tasks = new ArrayList<>();

		for (int i = 0; sqls != null && i < sqls.length; i++) {
			tasks.add(new ShardingBeeSQLExecutorEngine<T>(sqls[i], i + 1, beeSql, dsArray[i], entityClass));
		}

//		Logger.logSQL("========= Do sharding , the size of sub operation is :" + sqls.length);
		if(sqls!=null) ShardingLogReg.log(sqls.length);
		
//		Bee SQL Executor Engine
//		tasks.forEach(completionService::submit);
		int size=tasks.size();
		for (int i = 0; tasks != null && i < size; i++) {
			completionService.submit(tasks.get(i));
		}

		//Result Merge
		List<T> rsList = ResultMergeEngine.merge(completionService, size);
		
		executor.shutdown();
		
		//group and aggregate Entity,if necessary
		ShardingGroupByDecorator.groupAndAggregateEntity(rsList);

		// 排序装饰
		SortListDecorator.sort(rsList);

		// 排序后,要将数据放缓存. TODO

		if(showShardingSQL) Logger.debug("before ResultPagingDecorator, rows: "+rsList.size());
		
		// 分页装饰
		// 获取指定的一页数据
		ResultPagingDecorator.pagingList(rsList);
		
		if(showShardingSQL) Logger.debug("after  ResultPagingDecorator, rows: "+rsList.size());

		return rsList;
	}

	private class ShardingBeeSQLExecutorEngine<T> extends ShardingAbstractBeeSQLExecutorEngine<List<T>> {

		private Class<T> entityClass;

		public ShardingBeeSQLExecutorEngine(String sql, int index, BeeSql beeSql, String ds,
				Class<T> entityClass) {
			super(sql, index, beeSql, ds);
			this.entityClass = entityClass;
		}

		public List<T> shardingWork() {
			ShardingLogReg.regShardingSqlLog("select SQL", index, sql);
			return beeSql.selectSomeField(this.sql, entityClass); // 都是传同一个beeSql,是否会有线程问题?????
		}

	}

}
