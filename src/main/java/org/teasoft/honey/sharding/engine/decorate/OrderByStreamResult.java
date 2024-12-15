/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.decorate;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.teasoft.bee.sharding.ShardingPageStruct;
import org.teasoft.honey.osql.core.ExceptionHelper;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.TransformResultSet;
import org.teasoft.honey.sharding.ShardingUtil;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class OrderByStreamResult<T> {

	private Queue<CompareResult> orderByValuesQueue;
//	T entity;
	Class<T> entityClass;

	public OrderByStreamResult(Queue<CompareResult> orderByValuesQueue, Class<T> entityClass) {
		this.orderByValuesQueue = orderByValuesQueue;
		this.entityClass = entityClass;
	}

//	取出队头的元素转成Javabean,然后又放入队列,继续取出,直到队列为空.
	public List<T> getOnePageList() {
		List<T> onePageList = null;
		int from=0;
		int to=0;
		
//		if (this.orderByValuesQueue != null && shardingPage != null) {
		if (this.orderByValuesQueue != null) {
			ShardingPageStruct shardingPage = HoneyContext.getCurrentShardingPage();//TODO shardingPage在select * from tablename时,为空
			if(shardingPage != null) {//有分页的,要跳过查多的记录
//			int type = shardingPage.getPagingType();
//			if (type == 2 || type == 3) {
//			if (type != 1) {  //type==1 只有一条sql,只有一个rs,在上层,已转化.
				int start = shardingPage.getStart();
				int size = shardingPage.getSize();
				int first = ShardingUtil.firstRecordIndex();

				from = start;
				if (start == -1) {
//					if(first==1) from=0;
					from = 0; // List都是从0开始取首条
				} else {
					if (first == 1) from = from - 1;// 往前调整一条
				}

				to = from + size;

				for (int i = 0; i < from; i++) { // skip
					CompareResult cr = orderByValuesQueue.poll();
					if (cr != null && cr.hasNext()) orderByValuesQueue
							.offer(new CompareResult(cr.getResultSet(), cr.getStruct()));
				}

				onePageList = new ArrayList<>(size);
			}else {
				onePageList = new ArrayList<>();
			}
				
			 try {
//				for (int i = from; i < to && orderByValuesQueue.size() > 0; i++) { // [from,to)
				for (int i = from; orderByValuesQueue.size() > 0;) { // [from,to) no: i++
					
					if (shardingPage != null && !(i < to)) break;
					
					CompareResult cr = orderByValuesQueue.poll();

					if (cr.hasNext()) {
						ResultSet rs = cr.getResultSet();
						if (orderByValuesQueue.size() == 0) { //原来一个,取出后,变成0
							onePageList.add(TransformResultSet.rowToEntity(rs, entityClass));
							i++; // 转换了,才算
							while ((shardingPage == null || (shardingPage != null && i < to)) && rs.next()) {
								onePageList.add(TransformResultSet.rowToEntity(rs, entityClass));
								i++; // 转换了,才算
							}
						} else {
							if (rs.isAfterLast()) continue;
							onePageList.add(TransformResultSet.rowToEntity(rs, entityClass));
							i++; // 转换了,才算
							this.orderByValuesQueue.offer(new CompareResult(rs, cr.getStruct()));
						}
					}
				}
				
			  } catch (Exception e) {
				 throw ExceptionHelper.convert(e);
			  }
//			}
		}

		if(onePageList==null) onePageList = new ArrayList<>(); //fixed bug
		return onePageList;
	}

}
