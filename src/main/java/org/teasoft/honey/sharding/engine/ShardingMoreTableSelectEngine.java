/*
 * Copyright 2016-2023 the original author.All rights reserved.
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
import org.teasoft.honey.sharding.engine.decorate.SortListDecorator;

/**
 * 分片的select操作
 * 返回类型是List<T>
 * @author AiTeaSoft
 * @since  2.0
 */
public class ShardingMoreTableSelectEngine {
	
	private boolean showShardingSQL = getShowShardingSQL();
	
	private boolean getShowShardingSQL() {
		return HoneyConfig.getHoneyConfig().showSQL && HoneyConfig.getHoneyConfig().showShardingSQL;
	}

	public <T> List<T> asynProcess(String sql, T entity, BeeSql beeSql) {

		List<String[]> list;
		String sqls[] = null;
		String dsArray[] = null;

		if (ShardingUtil.hadShardingFullSelect()) {// 全域查询 或某些DS的某表全查询
			list = OrderByPagingRewriteSql.createSqlsForFullSelect(sql, entity.getClass());
		} else {
			list = OrderByPagingRewriteSql.createSqlsAndInit(sql); // 涉及部分分片
		}
		sqls = list.get(0);
		dsArray = list.get(1);

		ExecutorService executor = Executors.newCachedThreadPool(); // TODO 池是否要调整
//		ExecutorService executor = Executors.newWorkStealingPool(3);  //jdk 1.8 
		CompletionService<List<T>> completionService = new ExecutorCompletionService<>(executor);
		final List<Callable<List<T>>> tasks = new ArrayList<>(); // 构造任务

		for (int i = 0; sqls != null && i < sqls.length; i++) {
			tasks.add(new ShardingBeeSQLExecutorEngine<T>(sqls[i], i + 1, beeSql, dsArray[i], entity));
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

//	private class ShardingBeeSQLExecutorEngine<T> extends ShardingTemplate<List<T>> implements Callable<List<T>> {

	private class ShardingBeeSQLExecutorEngine<T>
			extends ShardingAbstractBeeSQLExecutorEngine<List<T>> {

		private T entity;

		public ShardingBeeSQLExecutorEngine(String sql, int index, BeeSql beeSql, String ds,
				T entity) {
			super(sql, index, beeSql, ds);
			this.entity = entity;
		}

		public List<T> shardingWork() {
			ShardingLogReg.regShardingSqlLog("select SQL", index, sql);
			return beeSql.moreTableSelect(this.sql, this.entity); // 都是传同一个beeSql,是否会有线程问题?????
		}

	}

}
