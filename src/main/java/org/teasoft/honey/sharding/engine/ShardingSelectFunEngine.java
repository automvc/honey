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
import org.teasoft.bee.osql.FunctionType;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.ShardingLogReg;
import org.teasoft.honey.osql.core.SimpleRewriteSql;
import org.teasoft.honey.osql.core.StringConst;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.util.StringUtils;
import org.teasoft.honey.util.currency.CurrencyArithmetic;

/**
 * 分片的select操作
 * 主要是对函数操作和selectJson的分片.
 * @author AiTeaSoft
 * @since  2.0
 */
public class ShardingSelectFunEngine {

	/**
	 * @param sql
	 * @param beeSql
	 * @param opType  Fun=1;JsonType=2;
	 * @param entityClass
	 * @return
	 */
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

		ExecutorService executor = Executors.newCachedThreadPool();
//		ExecutorService executor = Executors.newWorkStealingPool(3);  //jdk 1.8 
		CompletionService<String> completionService = new ExecutorCompletionService<>(executor);
		final List<Callable<String>> tasks = new ArrayList<>(); // 构造任务

		for (int i = 0; sqls != null && i < sqls.length; i++) {
			tasks.add(new ShardingBeeSQLFunExecutorEngine(sqls[i], i + 1, beeSql, dsArray[i]));
		}

//		Logger.logSQL("========= Do sharding , the size of sub operation is :"+sqls.length);
		if (sqls != null) ShardingLogReg.log(sqls.length);

		int size=tasks.size();
		for (int i = 0; tasks != null && i < size; i++) {
			completionService.submit(tasks.get(i));
		}

		List<String> rsList = ResultMergeEngine.mergeFunResult(completionService, size);

		executor.shutdown();

		return funResultEngine(rsList);
	}

	private String funResultEngine(List<String> rsList) {
		String funType = HoneyContext.getSysCommStrLocal(StringConst.FunType);
		Double temp = 0D;
		int position = -1;
		if (FunctionType.MAX.getName().equalsIgnoreCase(funType)) {
			boolean first = true;
			for (int i = 0; i < rsList.size(); i++) {

				if (StringUtils.isNotBlank(rsList.get(i))) {
					double d = Double.parseDouble(rsList.get(i));
					if (first) {
						temp = d;
						first = false;
						position = i;
					} else if (d > temp) {
						temp = d;
						position = i;
					}
				}
			}
		} else if (FunctionType.MIN.getName().equalsIgnoreCase(funType)) {
			boolean first = true;
			for (int i = 0; i < rsList.size(); i++) {
				if (StringUtils.isNotBlank(rsList.get(i))) {
					double d = Double.parseDouble(rsList.get(i));
					if (first) {
						temp = d;
						first = false;
						position = i;
					} else if (d < temp) {
						temp = d;
						position = i;
					}
				}
			}
		} else if (FunctionType.SUM.getName().equalsIgnoreCase(funType)) {
			boolean first = true;
			String sum = "0";
			for (int i = 0; i < rsList.size(); i++) {
				if (StringUtils.isNotBlank(rsList.get(i))) {
					sum = CurrencyArithmetic.add(sum, rsList.get(i));
					if (first) {
						first = false;
					}
				}
			}
			if (!first) return sum;
		} else if (FunctionType.COUNT.getName().equalsIgnoreCase(funType)) {
			long c = 0;
			for (int i = 0; i < rsList.size(); i++) {
				if (StringUtils.isNotBlank(rsList.get(i))) {
					long r = Long.parseLong(rsList.get(i));
					c += r;
				}
			}
			return c + "";
		}

		if (FunctionType.MAX.getName().equalsIgnoreCase(funType)
				|| FunctionType.MIN.getName().equalsIgnoreCase(funType)) {
			if (position >= 0) return rsList.get(position); // 直接返回原来的元素,防止转换过程,精度有变化
		}

		return "";
	}
	
//	Return String 
	private class ShardingBeeSQLFunExecutorEngine
			extends ShardingAbstractBeeSQLExecutorEngine<String> {

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