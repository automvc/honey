/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine;

import java.security.SecureRandom;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.sharding.ShardingSortStruct;
import org.teasoft.honey.distribution.GenIdFactory;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.OrderByPagingRewriteSql;
import org.teasoft.honey.osql.core.ShardingLogReg;
import org.teasoft.honey.osql.core.ShardingSortReg;
import org.teasoft.honey.osql.core.StringConst;
import org.teasoft.honey.osql.core.TransformResultSet;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.sharding.engine.decorate.CompareResult;
import org.teasoft.honey.sharding.engine.decorate.OrderByStreamResult;

/**
 * 分片的select操作
 * 返回类型是List<T>
 * @author AiTeaSoft
 * @since  2.0
 */
public class ShardingSelectRsEngine {

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

		if (sqls == null || sqls.length == 0) return null;
		ExecutorService executor = ThreadPoolUtil.getThreadPool(sqls.length);
		CompletionService<ResultSet> completionService = new ExecutorCompletionService<>(executor);
		final List<Callable<ResultSet>> tasks = new ArrayList<>();

		// fixed bug
		String threadFlag = getSelectRsThreadFlag();
		ShardingUtil.regSelectRsThreadFlag(threadFlag);

		for (int i = 0; sqls != null && i < sqls.length; i++) {
			tasks.add(new ShardingBeeSQLExecutorEngine<T>(sqls[i], i + 1, beeSql, dsArray[i])); // 获取RS
		}

		if (sqls != null) ShardingLogReg.log(sqls.length);

//		Bee SQL Executor Engine
		int size = tasks.size();
		for (int i = 0; tasks != null && i < size; i++) {
			completionService.submit(tasks.get(i));
		}

		ResultSet rs = null;
		// Result Merge
		ShardingSortStruct struct = null;
		Queue<CompareResult> queue = new PriorityQueue<>(size);
		for (int i = 0; i < size; i++) {
			try {
				rs = completionService.take().get(); // 先于getCurrentShardingSort获取
				if (size == 1) break; // 只有一个rs,直接转化

				if (i == 0) {
					ShardingSortReg.regSort(rs.getMetaData());
					struct = HoneyContext.getCurrentShardingSort();
				}
				queue.offer(new CompareResult(rs, struct));
			} catch (InterruptedException e) {
				Logger.warn(e.getMessage(), e);
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				Logger.warn(e.getMessage(), e);
			}
		}
		executor.shutdown();

		List<T> rsList = null;
		if (size == 1) {// v2.4.0 只有一个rs,直接转化
			rsList = TransformResultSet.rsToListEntity(rs, entityClass);
		} else {
			// 放入优先队列后,就转换出需要的数据. 要传入需要多少数据? 在内部处理. 有取中间几条的吗? 有
			rsList = new OrderByStreamResult<>(queue, entityClass).getOnePageList();
		}

		for (int i = 0; i < size; i++) { // fixed bug
			HoneyContext.clearConnForSelectRs(threadFlag + (i + 1));
		}

		return rsList;
	}

	private class ShardingBeeSQLExecutorEngine<T> extends ShardingAbstractBeeSQLExecutorEngine<ResultSet> {

		public ShardingBeeSQLExecutorEngine(String sql, int index, BeeSql beeSql, String ds) {
			super(sql, index, beeSql, ds);
		}

		public ResultSet shardingWork() {
			ShardingLogReg.regShardingSqlLog("select SQL", index, sql);
			return beeSql.selectRs(this.sql);
		}
	}

	private SecureRandom sr = new SecureRandom();

	private String getSelectRsThreadFlag() {
		long gid = GenIdFactory.get(StringConst.ShardingSelectRs_ThreadFlag, GenIdFactory.GenType_OneTimeSnowflakeId);
		String threadFlag = gid + "" + sr.nextDouble();
		return threadFlag;
	}

}
