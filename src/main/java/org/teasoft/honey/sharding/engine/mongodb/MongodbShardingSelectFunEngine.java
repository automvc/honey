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
import org.teasoft.bee.osql.FunctionType;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.ShardingLogReg;
import org.teasoft.honey.osql.core.StringConst;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.sharding.engine.ResultMergeEngine;
import org.teasoft.honey.sharding.engine.ShardingFunResultEngine;

/**
 * 分片的select操作
 * 主要是对函数操作的分片.
 * @author AiTeaSoft
 * @since  2.0
 */
public class MongodbShardingSelectFunEngine {

	public <T> String asynProcess(Class<T> entityClass, MongodbBeeSql mongodbBeeSql,
			MongoSqlStruct struct) {

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
			tasks.add(new ShardingBeeSQLFunExecutorEngine(tabArray[i], i + 1, mongodbBeeSql,
					dsArray[i], entityClass, struct));
		}

		if (dsArray != null) ShardingLogReg.log(dsArray.length);

		int size = tasks.size();
		for (int i = 0; tasks != null && i < size; i++) {
			completionService.submit(tasks.get(i));
		}

		List<String> rsList = ResultMergeEngine.mergeFunResult(completionService, size);

		executor.shutdown();

		return ShardingFunResultEngine.funResultEngine(rsList);
	}

//	Return String 		
	private class ShardingBeeSQLFunExecutorEngine
			extends ShardingAbstractMongoBeeSQLExecutorEngine<String> {
		private Class entityClass;
		private MongoSqlStruct struct;

		public ShardingBeeSQLFunExecutorEngine(String tab, int index,
				MongodbBeeSql mongodbBeeSql, String ds, Class entityClass,
				MongoSqlStruct struct) {
			super(tab, index, mongodbBeeSql, ds);
			this.entityClass = entityClass;
			this.struct = struct.copy();
			this.struct.setTableName(tab);
		}

		public String shardingWork() {
			ShardingLogReg.regShardingSqlLog("select fun SQL", index, tab);
			String rsStr = "";
			String typeTag = "";
			typeTag = "select fun";

			String funType = HoneyContext.getSysCommStrLocal(StringConst.FunType);
			if (FunctionType.COUNT.getName().equalsIgnoreCase(funType)) {
				rsStr = mongodbBeeSql.count(struct, entityClass);
			} else {
				rsStr = mongodbBeeSql.selectWithFun(struct, entityClass);
			}
			
			ShardingLogReg.regShardingSqlLog("    | <--  " + typeTag, index, rsStr);

			return rsStr;
		}
	}

}