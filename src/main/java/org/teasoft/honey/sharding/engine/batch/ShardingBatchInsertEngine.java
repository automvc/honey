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

import org.teasoft.bee.osql.Serializer;
import org.teasoft.bee.osql.api.SuidRich;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.JdkSerializer;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.osql.core.NameTranslateHandle;
import org.teasoft.honey.osql.core.ShardingLogReg;
import org.teasoft.honey.osql.core.StringConst;
import org.teasoft.honey.osql.name.NameUtil;
import org.teasoft.honey.sharding.ShardingReg;
import org.teasoft.honey.sharding.config.ShardingRegistry;
import org.teasoft.honey.sharding.engine.ResultMergeEngine;
import org.teasoft.honey.sharding.engine.ThreadPoolUtil;
import org.teasoft.honey.util.StringUtils;

/**
 * 批量插入分片(使用CompletionService)
 * 在SuidRich切入.
 * @author AiTeaSoft
 * @since  2.0
 */
public class ShardingBatchInsertEngine<T> {

	@SuppressWarnings("unchecked")
	public int batchInsert(T entity[], int batchSize, String excludeFields, List<String> tabNameListForBatch,
			SuidRich suidRich) {

		ShardingReg.regShardingBatchInsertDoing();

//		Logger.debug("" + (tabNameListForBatch.size() == entity.length));

		List<String> taskDs = new ArrayList<>();
		List<String> taskTab = new ArrayList<>();

		List<String> dsNameListForBatch = HoneyContext.getListLocal(StringConst.DsNameListForBatchLocal);

		boolean isBroadcastTabBatchInsert = false;
		String tableName = _toTableName(entity[0]);
		if (ShardingRegistry.isBroadcastTab(tableName)) isBroadcastTabBatchInsert = true;

		int time = 0;

		final List<Callable<Integer>> tasks = new ArrayList<>();

		if (!isBroadcastTabBatchInsert) {
			// 要求的数据
			List<Object[]> newEntityArrayList = new ArrayList<>();
			Map<String, List<Integer>> tabMap = groupElement(tabNameListForBatch);

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

			for (int i = 0; i < newEntityArrayList.size(); i++) {
				tasks.add(new ShardingBeeSQLBatchInsertExecutorEngine(newEntityArrayList.get(i), batchSize, excludeFields,
						taskDs, taskTab, suidRich, i));
			}

			time = newEntityArrayList.size();

		} else { // BroadcastTab insert

			time = dsNameListForBatch.size();

			taskTab = HoneyContext.getListLocal(StringConst.TabNameListForBatchLocal); // 广播表,一库一表
			for (int i = 0; i < time; i++) {
				tasks.add(new ShardingBeeSQLBatchInsertExecutorEngine(entity, batchSize, excludeFields,
						dsNameListForBatch, taskTab, suidRich, i));
			}
		}

		ShardingLogReg.log(time);

		int size = tasks.size();
		if (size == 0) return 0;

		ExecutorService executor = ThreadPoolUtil.getThreadPool(size);
		CompletionService<Integer> completionService = new ExecutorCompletionService<>(executor);
		for (int i = 0; tasks != null && i < size; i++) {
			completionService.submit(tasks.get(i));
		}

		// Merge Result
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

	private String _toTableName(Object entity) {
		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
	}

	private class ShardingBeeSQLBatchInsertExecutorEngine extends ShardingBatchInsertTemplate<Integer>
			implements Callable<Integer> {

		private int batchSize;
		private String excludeFields;
		private SuidRich suidRich;
//		private List<Object[]> newEntityArrayList = new ArrayList<>();
		private Object[] newEntityArray;

//		public ShardingBeeSQLBatchInsertExecutorEngine(List<Object[]> newEntityArrayList,
		public ShardingBeeSQLBatchInsertExecutorEngine(Object[] newEntityArray, int batchSize, String excludeFields,
				List<String> taskDs, List<String> taskTab, SuidRich suidRich, int index) {

			this.batchSize = batchSize;
			this.excludeFields = excludeFields;
			this.suidRich = suidRich;
			this.newEntityArray = newEntityArray;

			super.taskDs = taskDs;
			super.taskTab = taskTab;
			super.index = index;

		}

		@Override
		public Integer shardingWork() {
			int b = copy(suidRich).insert(newEntityArray, batchSize, excludeFields);
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
			return suidRich; // 没有序列化(有异常)返回原来的
		}
	}

}
