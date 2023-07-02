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
import org.teasoft.honey.osql.core.ShardingLogReg;
import org.teasoft.honey.osql.core.SimpleRewriteSql;
import org.teasoft.honey.sharding.ShardingUtil;

/**
 * 分片的select操作
 * 主要是对函数操作和selectJson的分片.
 * @author AiTeaSoft
 * @since  2.0
 */
public class ShardingSelectFunEngine {

	@SuppressWarnings("rawtypes")
	public <T> String asynProcess(String sql, BeeSql beeSql, Class entityClass) {

		List<String[]> list;
		String sqls[] = null;
		String dsArray[] = null;

		if (ShardingUtil.hadShardingFullSelect()) {// 全域查询
			list = SimpleRewriteSql.createSqlsForFull(sql, entityClass);
		} else {
			list = SimpleRewriteSql.createSqlsAndInit(sql); // 部分分片
		}
		sqls = list.get(0);
		dsArray = list.get(1);

		if(sqls==null || sqls.length==0) return null;
		ExecutorService executor = ThreadPoolUtil.getThreadPool(sqls.length);
		CompletionService<String> completionService = new ExecutorCompletionService<>(executor);
		final List<Callable<String>> tasks = new ArrayList<>(); 

		for (int i = 0; sqls != null && i < sqls.length; i++) {
			tasks.add(new ShardingBeeSQLFunExecutorEngine(sqls[i], i + 1, beeSql, dsArray[i]));
		}

		if (sqls != null) ShardingLogReg.log(sqls.length);

		int size=tasks.size();
		for (int i = 0; tasks != null && i < size; i++) {
			completionService.submit(tasks.get(i));
		}

		List<String> rsList = ResultMergeEngine.mergeFunResult(completionService, size);

		executor.shutdown();

		return ShardingFunResultEngine.funResultEngine(rsList);
	}

	
//	Return String 
	private class ShardingBeeSQLFunExecutorEngine extends ShardingAbstractBeeSQLExecutorEngine<String> {

		public ShardingBeeSQLFunExecutorEngine(String sql, int index, BeeSql beeSql, String ds) {
			super(sql, index, beeSql, ds);
		}

		public String shardingWork() {
			ShardingLogReg.regShardingSqlLog("select fun SQL", index, sql);
			String rsStr = "";
			String typeTag = "";
			typeTag = "select fun";

			rsStr = beeSql.selectFun(this.sql);
			ShardingLogReg.regShardingSqlLog("    | <--  " + typeTag, index, rsStr);

			return rsStr;
		}
	}
}