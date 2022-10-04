/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.decorate;

import java.util.ArrayList;
import java.util.List;

import org.teasoft.bee.sharding.ShardingPageStruct;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.sharding.ShardingUtil;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class ResultPagingDecorator {
	
	public static <T> void pagingList(List<T> rsList) {
		//分页装饰
		//获取指定的一页数据
		ShardingPageStruct shardingPage=HoneyContext.getCurrentShardingPage();
		if(shardingPage!=null) {
			int type=shardingPage.getPagingType();
			if(type==2 || type==3) {
				int start = shardingPage.getStart();
				int size = shardingPage.getSize();
				int first=ShardingUtil.firstRecordIndex();
				
				int from=start;
				if(start==-1) {
//					if(first==1) from=0;
					from=0; //List都是从0开始取首条
				}else {
					if(first==1) from=from-1;//往前调整一条
				}
				
				int to=from+size;
				
				List<T> onePageList = new ArrayList<>(size);
				for (int i = from; i<to && i < rsList.size(); i++) {  // [from,to)
					onePageList.add(rsList.get(i));
				}
//				rsList=onePageList;  // 确认是否可以返回?  不行
				rsList.clear();
				rsList.addAll(onePageList);
			}
			
		}
	}

}
