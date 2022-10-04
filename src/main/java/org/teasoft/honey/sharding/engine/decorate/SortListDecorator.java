/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.decorate;

import java.util.ArrayList;
import java.util.List;

import org.teasoft.bee.osql.OrderType;
import org.teasoft.bee.sharding.ShardingPageStruct;
import org.teasoft.bee.sharding.ShardingSortStruct;
import org.teasoft.bee.spi.entity.SortStruct;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.util.EntityUtil;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class SortListDecorator {
	
	public static void sort(List<?> rsList) {
		//排序装饰
		ShardingPageStruct shardingPage=HoneyContext.getCurrentShardingPage();
		if (rsList != null && rsList.size() > 1) {
			ShardingSortStruct shardingSort = HoneyContext.getCurrentShardingSort();
			
			if (shardingSort != null && shardingPage.getPagingType()!=1) { //分片分页的类型为1的,使用union all 已在DB进行排序
				List<SortStruct> sortBeanList = new ArrayList<>();

//			   sortBeanList.add(new SortStruct("id"));
//			   sortBeanList.add(new SortStruct("orderid",true));

				String orderFields[] = shardingSort.getOrderFields();
				OrderType orderTypes[] = shardingSort.getOrderTypes();

				for (int i = 0; i < orderFields.length; i++) {
					if (orderTypes == null || OrderType.DESC == orderTypes[i]) {
						sortBeanList.add(new SortStruct(orderFields[i], true));
					} else {
						sortBeanList.add(new SortStruct(orderFields[i]));
					}
				}

				EntityUtil.sort(rsList, sortBeanList);
			}
		}
	}

}
