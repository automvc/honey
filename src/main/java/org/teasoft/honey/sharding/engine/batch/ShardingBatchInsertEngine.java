/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.batch;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.teasoft.bee.osql.Serializer;
import org.teasoft.bee.osql.SuidRich;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.JdkSerializer;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.osql.core.ShardingLogReg;
import org.teasoft.honey.osql.core.StringConst;
import org.teasoft.honey.sharding.ShardingReg;
import org.teasoft.honey.sharding.config.ShardingRegistry;
import org.teasoft.honey.sharding.engine.ResultMergeEngine;
import org.teasoft.honey.util.StringUtils;

/**
 * 批量插入分片(使用CompletionService)
 * 在SuidRich切入.
 * @author AiTeaSoft
 * @since  2.0
 */
public class ShardingBatchInsertEngine<T> {

	@SuppressWarnings("unchecked")
	public int batchInsert(T entity[], int batchSize, String excludeFields,
			List<String> tabNameListForBatch, SuidRich suidRich) {
		
		ShardingReg.regShardingBatchInsertDoing();

//		 集合大小一致
        Logger.debug(""+(tabNameListForBatch.size() == entity.length));

		// 要求的数据
		List<Object[]> newEntityArrayList = new ArrayList<>();
		List<String> taskDs = new ArrayList<>();
		List<String> taskTab = new ArrayList<>();

		Map<String, List<Integer>> tabMap = groupElement(tabNameListForBatch);
		List<String> dsNameListForBatch = HoneyContext.getListLocal(StringConst.DsNameListForBatchLocal);

		for (Map.Entry<String, List<Integer>> entry : tabMap.entrySet()) {
			String tabName = entry.getKey();
			List<Integer> indexList = entry.getValue();

			T newEntity[] = (T[]) new Object[indexList.size()];
			for (int i = 0; i < indexList.size(); i++) {
				newEntity[i] = entity[indexList.get(i)];
			}
			newEntityArrayList.add(newEntity);

			String dsName = dsNameListForBatch.get(indexList.get(0)); // 表名一样,对应的ds也要是一样的.
			if (StringUtils.isBlank(dsName)) {
				dsName = ShardingRegistry.getDsByTab(tabName);
			}

			taskDs.add(dsName);
			taskTab.add(tabName);
		}

		ExecutorService executor = Executors.newCachedThreadPool();
		CompletionService<Integer> completionService = new ExecutorCompletionService<>(
				executor);
		final List<Callable<Integer>> tasks = new ArrayList<>(); // 构造任务

		for (int i = 0; i < newEntityArrayList.size(); i++) {
			tasks.add(new ShardingBeeSQLBatchInsertExecutorEngine(newEntityArrayList, batchSize, excludeFields,
					taskDs, taskTab, suidRich, i));
		}

		ShardingLogReg.log(newEntityArrayList.size());
		
		int size=tasks.size();
		for (int i = 0; tasks != null && i < size; i++) {
			completionService.submit(tasks.get(i));
		}

		//Merge Result
		int r = ResultMergeEngine.mergeInteger(completionService, size);
		
		executor.shutdown();
		
		return r;
	}

	// List<String>按元素值分组.
	private Map<String, List<Integer>> groupElement(List<String> tabNameListForBatch) {
		Map<String, List<Integer>> tabMap = new LinkedHashMap<>();

		for (int i = 0; i < tabNameListForBatch.size(); i++) {
			List<Integer> dsList = tabMap.get(tabNameListForBatch.get(i));
			if (dsList == null) dsList = new ArrayList<>();
			dsList.add(i);
			tabMap.put(tabNameListForBatch.get(i), dsList);
		}

		return tabMap;
	}

	private class ShardingBeeSQLBatchInsertExecutorEngine
			extends ShardingBatchInsertTemplate<Integer> implements Callable<Integer> {

		private int batchSize;
		private String excludeFields;
		private SuidRich suidRich;
		private List<Object[]> newEntityArrayList = new ArrayList<>();

		public ShardingBeeSQLBatchInsertExecutorEngine(List<Object[]> newEntityArrayList,
				int batchSize, String excludeFields, List<String> taskDs, List<String> taskTab,
				SuidRich suidRich, int index) {

			this.batchSize = batchSize;
			this.excludeFields = excludeFields;
			this.suidRich = suidRich;
			this.newEntityArrayList = newEntityArrayList;

			super.taskDs = taskDs;
			super.taskTab = taskTab;
			super.index = index;

		}

		public Integer shardingWork() {
		    int b = copy(suidRich).insert(newEntityArrayList.get(index), batchSize, excludeFields);
//			int b = suidRich.insert(newEntityArrayList.get(index), batchSize, excludeFields);
			return b;
		}

		@Override
		public Integer call() throws Exception {
			return doSharding();
		}
		
		private SuidRich copy(SuidRich suidRich) {
			try {
				Serializer jdks = new JdkSerializer();
				return (SuidRich) jdks.unserialize(jdks.serialize(suidRich));
			} catch (Exception e) {
				Logger.debug(e.getMessage(), e);
			}
			return suidRich; //没有序列化(有异常)返回原来的
		}
	}

}
