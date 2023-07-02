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

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.honey.osql.core.OrderByPagingRewriteSql;
import org.teasoft.honey.osql.core.ShardingLogReg;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.sharding.engine.decorate.ResultPagingDecorator;
import org.teasoft.honey.sharding.engine.decorate.SortStringArrayListDecorator;

/**
 * 分片的select操作
 * 专门为处理返回值是List<String[]>的类型
 * @author AiTeaSoft
 * @since  2.0
 */
public class ShardingSelectListStringArrayEngine {

	@SuppressWarnings("rawtypes")
	public List<String[]> asynProcess(String sql, BeeSql beeSql,Class entityClass) {
		
//		Logger.info(" asyn(任务分发前) 当前线程id:  " + Thread.currentThread().getId());
		
		List<String[]> list;
		String sqls[]=null;
		String dsArray[]=null;
		
		if (ShardingUtil.hadShardingFullSelect()) {// 全域查询
			list = OrderByPagingRewriteSql.createSqlsForFullSelect(sql, entityClass);
		} else {
			list = OrderByPagingRewriteSql.createSqlsAndInit(sql); // 涉及部分分片
		}
		
		sqls=list.get(0);
		dsArray=list.get(1);

		if(sqls==null || sqls.length==0) return null;
		ExecutorService executor = ThreadPoolUtil.getThreadPool(sqls.length);
		CompletionService<List<String[]>> completionService = new ExecutorCompletionService<>(executor);
		final List<Callable<List<String[]>>> tasks = new ArrayList<>(); 

		for (int i = 0; sqls != null && i < sqls.length; i++) {
			tasks.add(new ShardingBeeSQLExecutorEngine(sqls[i], i + 1, beeSql, dsArray[i]));
		}
		
		if (sqls != null) ShardingLogReg.log(sqls.length);
		
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


//	Return ListStringArray
	private class ShardingBeeSQLExecutorEngine extends ShardingAbstractBeeSQLExecutorEngine<List<String[]>> {

		public ShardingBeeSQLExecutorEngine(String sql, int index, BeeSql beeSql, String ds) {
			super(sql, index, beeSql, ds);
		}

		public List<String[]> shardingWork() {
			ShardingLogReg.regShardingSqlLog("select SQL", index, sql);
			return beeSql.select(sql);
		}
	}

}
