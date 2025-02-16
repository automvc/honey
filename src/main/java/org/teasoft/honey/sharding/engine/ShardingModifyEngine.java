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
 * 分片的Modify(update,insert,delete)操作
 * @author AiTeaSoft
 * @since  2.0
 */
public class ShardingModifyEngine {

	@SuppressWarnings("rawtypes")
	public <T> int asynProcess(String sql, Class entityClass, BeeSql beeSql) {

		List<String[]> list;
		String sqls[] = null;
		String dsArray[] = null;

		if (ShardingUtil.hadShardingFullSelect()) {// 全域查询 , modify也是这个
			list = SimpleRewriteSql.createSqlsForFull(sql, entityClass);
		} else {
			list = SimpleRewriteSql.createSqlsAndInit(sql); // 涉及部分分片
		}
		sqls = list.get(0);
		dsArray = list.get(1);

		if (sqls == null || sqls.length == 0) return 0;
		ExecutorService executor = ThreadPoolUtil.getThreadPool(sqls.length);
		CompletionService<Integer> completionService = new ExecutorCompletionService<>(executor);
		final List<Callable<Integer>> tasks = new ArrayList<>();

		for (int i = 0; sqls != null && i < sqls.length; i++) {
			tasks.add(new ShardingBeeSQLModifyExecutorEngine(sqls[i], i + 1, beeSql, dsArray[i]));
		}

		if (sqls != null) ShardingLogReg.log(sqls.length);

		int size = tasks.size();
		for (int i = 0; tasks != null && i < size; i++) {
			completionService.submit(tasks.get(i));
		}

		// Merge Result
		int r = ResultMergeEngine.mergeInteger(completionService, size);

		executor.shutdown();

		return r;
	}

	private class ShardingBeeSQLModifyExecutorEngine extends ShardingAbstractBeeSQLExecutorEngine<Integer> {

		public ShardingBeeSQLModifyExecutorEngine(String sql, int index, BeeSql beeSql, String ds) {
			super(sql, index, beeSql, ds);
		}

		public Integer shardingWork() {
			ShardingLogReg.regShardingSqlLog("modify SQL", index, sql);
			return beeSql.modify(this.sql);
		}
	}

}
