/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.sharding.GroupFunStruct;
import org.teasoft.bee.spi.JsonTransform;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.JsonResultWrap;
import org.teasoft.honey.osql.core.NameTranslateHandle;
import org.teasoft.honey.osql.core.OrderByPagingRewriteSql;
import org.teasoft.honey.osql.core.ShardingLogReg;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.sharding.engine.decorate.ResultPagingDecorator;
import org.teasoft.honey.sharding.engine.decorate.ShardingGroupByDecorator;
import org.teasoft.honey.sharding.engine.decorate.SortStringArrayListDecorator;
import org.teasoft.honey.spi.SpiInstanceFactory;
import org.teasoft.honey.util.ObjectCreatorFactory;

/**
 * 分片的select操作
 * 处理List<String[]>,List<T>,List<String> json 包含有AVG的分组分片
 * @author AiTeaSoft
 * @since  2.0
 */
public class ShardingGroupbyListStringArrayEngine {
	
	private final static int List_String_Array = 1;
	private final static int List_T = 2;
//	private final static int List_StringJson = 3;

//	@SuppressWarnings("rawtypes")
	public <T> Object asynProcess(String sql, BeeSql beeSql,Class<T> entityClass, int returnType) {
//	public Object asynProcess(String sql, BeeSql beeSql, Class entityClass) {
		
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

		ExecutorService executor = Executors.newCachedThreadPool();
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
		
		//group and aggregate List<String[]>,if necessary
		ShardingGroupByDecorator.groupAndAggregateStringArray(rsList);
		
		// 排序装饰
		SortStringArrayListDecorator.sort(rsList);

		// 分页装饰
		// 获取指定的一页数据
		ResultPagingDecorator.pagingList(rsList);
		
		if (returnType == List_String_Array) { // String[]
			// 要去掉多余的列 ,在groupAndAggregateStringArray已处理
			return (List<String[]>) rsList;
		} else {
//			要转化成List<T>或json的List即List<String>   
			List<T> entityList = new ArrayList<>(rsList.size());

			// 将List<String[]>转成List<T> 
			T targetObj = null;

			try {
			
				GroupFunStruct groupFunStruct = HoneyContext.getCurrentGroupFunStruct();
				for (int i = 0; i < rsList.size(); i++) {
					targetObj = (T) entityClass.newInstance();
					String[] current = rsList.get(i);

					for (Map.Entry<String, Integer> entry : groupFunStruct.getColumnIndexMap().entrySet()) {
						String columnName = entry.getKey();
						int index = entry.getValue();
						String value = current[index];

						Field field = null;
						String name = null;
						try {
							name = _toFieldName(columnName, entityClass);
							field = entityClass.getDeclaredField(name);// 可能会找不到Javabean的字段
						} catch (NoSuchFieldException e) {
							continue;
						}
						Object obj = ObjectCreatorFactory.create(value, field.getType());
						field.setAccessible(true);
						field.set(targetObj, obj);
					}//处理一行记录结束
					entityList.add(targetObj);
				}
			} catch (IllegalAccessException | InstantiationException e) {
				e.printStackTrace();
			}

			if (returnType == List_T) {
				return entityList;
//			}else if(returnType==List_StringJson) {
			} else {
				String json = "";
				int rowCount;
//			    if (rsList.size() == 1) {
//				  json = rsList.get(0);
//				  rowCount = 1;
//			    } else 
				if (rsList.size() == 0) {
					json = "[]";
					rowCount = 0;
				}
				JsonTransform jsonTransform = SpiInstanceFactory.getJsonTransform();
				json = jsonTransform.toJson(entityList);
				rowCount = entityList.size();

				JsonResultWrap wrap = new JsonResultWrap();
				wrap.setResultJson(json);
				wrap.setRowCount(rowCount);
				return wrap;
			}
		}
	}
		
	@SuppressWarnings("rawtypes")
	private static String _toFieldName(String columnName, Class entityClass) {
		return NameTranslateHandle.toFieldName(columnName, entityClass);
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
