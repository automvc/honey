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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import org.teasoft.bee.osql.api.SuidRich;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.ShardingLogReg;
import org.teasoft.honey.osql.core.StringConst;
import org.teasoft.honey.sharding.config.ShardingRegistry;
import org.teasoft.honey.util.StringUtils;

/**
 * 批量插入分片,使用ForkJoin
 * @author AiTeaSoft
 * @since  2.0
 */
public class ShardingForkJoinBatchInsertEngine<T> {

	@SuppressWarnings("unchecked")
	public int batchInsert(T entity[], int batchSize, String excludeFields, List<String> tabNameListForBatch,
			SuidRich suidRich) {

//	           集合大小一致
//		.println(tabNameListForBatch.size() == entity.length);

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

//		Logger.logSQL("========= Do sharding , the size of sub operation is :"+taskTab.size());
		ShardingLogReg.log(taskTab.size());

		return doTask(new ShardingRecursiveBatchInsert(newEntityArrayList, batchSize, excludeFields, taskDs, taskTab,
				suidRich));
	}

	private int doTask(ShardingRecursiveBatchInsert work) {

//		final ForkJoinPool forkJoinPool = new ForkJoinPool();
//		ForkJoinTask<Integer> task = forkJoinPool.submit(work);
//		int a = 0;
//		try {
//			Integer count = task.get();
//			if (count != null) a = count;
//			if(count!=null) a=count;
//		} catch (Exception e) {
//			Logger.error(e.getMessage(), e);
//		}
//		return a;

		ForkJoinPool pool = new ForkJoinPool();
		pool.invoke(work);
		return work.join();
	}

//List<String>按元素值分组.
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

	/**
	 * 批量插入分片,使用RecursiveTask,进行ForkJoin
	 * @author Kingstar
	 * @since  2.0
	 */
	private class ShardingRecursiveBatchInsert extends RecursiveTask<Integer> {

		private static final long serialVersionUID = 12345602L;

		private int start;
		private int end;
		private int batchSize;
		private String excludeFields;
		private SuidRich suidRich;
		private List<Object[]> newEntityArrayList = new ArrayList<>();
		private List<String> taskDs = new ArrayList<>();
		private List<String> taskTab = new ArrayList<>();

		public ShardingRecursiveBatchInsert(List<Object[]> newEntityArrayList, int batchSize, String excludeFields,
				List<String> taskDs, List<String> taskTab, SuidRich suidRich) {

			this.start = 0;
			this.end = newEntityArrayList.size() - 1;

			this.batchSize = batchSize;
			this.excludeFields = excludeFields;
			this.suidRich = suidRich;
			this.newEntityArrayList = newEntityArrayList;
			this.taskDs = taskDs;
			this.taskTab = taskTab;
		}

		public ShardingRecursiveBatchInsert(List<Object[]> newEntityArrayList, int batchSize, String excludeFields,
				List<String> taskDs, List<String> taskTab, SuidRich suidRich, int start, int end) {

			this(newEntityArrayList, batchSize, excludeFields, taskDs, taskTab, suidRich);

			this.start = start;
			this.end = end;
		}

		@Override
		protected Integer compute() {
			if (end == start) {
//			Logger.info(">>>>>>>>>>>do sharding "+start);
				return doOneTask(newEntityArrayList.get(start), batchSize, excludeFields, start);
			} else {
//			int mid = (end + start) / 2;
				ShardingRecursiveBatchInsert task1 = new ShardingRecursiveBatchInsert(newEntityArrayList, batchSize,
						excludeFields, taskDs, taskTab, suidRich,
//					start, mid);
						start, start); // 按顺序分派
				ShardingRecursiveBatchInsert task2 = new ShardingRecursiveBatchInsert(newEntityArrayList, batchSize,
						excludeFields, taskDs, taskTab, suidRich,
//					mid + 1, end);
						start + 1, end);

				invokeAll(task1, task2);
				return task1.join() + task2.join();
			}
		}

		private int doOneTask(Object entity[], int batchSize, String excludeFields, int index) {

			try {
				HoneyContext.setSqlIndexLocal(index);

				HoneyContext.setAppointTab(taskTab.get(index));
				HoneyContext.setAppointDS(taskDs.get(index));

//		        int b = copy(suidRich).insert(entity, batchSize, excludeFields);
				int b = suidRich.insert(entity, batchSize, excludeFields);
				return b;
			} finally {
				HoneyContext.removeAppointDS();
				HoneyContext.removeAppointTab();
				HoneyContext.removeSqlIndexLocal();
			}

		}
	}
}