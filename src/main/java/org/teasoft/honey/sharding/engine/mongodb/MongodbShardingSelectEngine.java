/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.mongodb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.teasoft.bee.mongodb.MongoSqlStruct;
import org.teasoft.bee.mongodb.MongodbBeeSql;
import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.osql.core.ShardingLogReg;
import org.teasoft.honey.osql.core.StringConst;
import org.teasoft.honey.sharding.config.ShardingRegistry;
import org.teasoft.honey.sharding.engine.ResultMergeEngine;
import org.teasoft.honey.sharding.engine.decorate.ResultPagingDecorator;
import org.teasoft.honey.sharding.engine.decorate.SortListDecorator;
import org.teasoft.honey.util.StringUtils;

/**
 * 分片的select操作
 * 返回类型是List<T>
 * @author AiTeaSoft
 * @since  2.0
 */
public class MongodbShardingSelectEngine {
	
	
	//找表排列对应的ds排列
	static String[] _createSql(List<String> tabSuffixList, List<String> tabNameList,
			Map<String, String> tab2DsMap) {
		String dsArray[] = new String[tabSuffixList.size()];
		for (int i = 0; i < tabSuffixList.size(); i++) {
			String dsName = tab2DsMap.get(tabSuffixList.get(i)); // 只在使用注解时, 分库与分表同属于一个分片键,才有用. TODO
			if (StringUtils.isBlank(dsName)) {
				dsName = ShardingRegistry.getDsByTab(tabNameList.get(i));
			}
			dsArray[i] = dsName;
//			System.err.println(">>>>>>>>>>>>>>>>>>:"+dsArray[i]);
		}
		return dsArray;
	}
	
	private boolean showShardingSQL = getShowShardingSQL();
	
	private boolean getShowShardingSQL() {
		return HoneyConfig.getHoneyConfig().showSQL && HoneyConfig.getHoneyConfig().showShardingSQL;
	}
	
	public <T> List<T> asynProcess(Class<T> entityClass, MongodbBeeSql mongodbBeeSql,MongoSqlStruct struct) {

		List<String> tabNameList = HoneyContext.getListLocal(StringConst.TabNameListLocal);
		List<String> tabSuffixList = HoneyContext.getListLocal(StringConst.TabSuffixListLocal);
		Map<String, String> tab2DsMap = HoneyContext.getCustomMapLocal(StringConst.ShardingTab2DsMap);
		
		String dsArray[]=_createSql(tabSuffixList, tabNameList, tab2DsMap);
		

//		全域查询 或某些DS的某表全查询		
		//找到所有的表名,及对应的ds  TODO
		//分页 
		//ShardingSortStruct 收集排序的信息

		ExecutorService executor = Executors.newCachedThreadPool();
//		ExecutorService executor = Executors.newWorkStealingPool(3);  //jdk 1.8 
		CompletionService<List<T>> completionService = new ExecutorCompletionService<>(executor);
		final List<Callable<List<T>>> tasks = new ArrayList<>(); // 构造任务

		for (int i = 0; dsArray != null && i < dsArray.length; i++) {
			tasks.add(new ShardingBeeSQLExecutorEngine<T>(tabNameList.get(i), i + 1, mongodbBeeSql, dsArray[i], entityClass,struct));
		}

//		Logger.logSQL("========= Do sharding , the size of sub operation is :" + sqls.length);
		if(dsArray!=null) ShardingLogReg.log(dsArray.length);
		
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

	private class ShardingBeeSQLExecutorEngine<T>
			extends ShardingAbstractMongoBeeSQLExecutorEngine<List<T>> {

		private Class<T> entityClass;
		private MongoSqlStruct struct;

		public ShardingBeeSQLExecutorEngine(String tab, int index, MongodbBeeSql beeSql, String ds,
				Class<T> entityClass,MongoSqlStruct struct) {
			super(tab, index, beeSql, ds);
			this.entityClass = entityClass;
			this.struct=struct.copy();
			this.struct.tableName=tab;
		}

		public List<T> shardingWork() {
//			ShardingLogReg.regShardingSqlLog("select SQL", index, tab);
			return mongodbBeeSql.select(struct,entityClass);
		}
	}

}
