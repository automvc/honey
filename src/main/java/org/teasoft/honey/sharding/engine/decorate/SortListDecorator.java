/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.decorate;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.teasoft.bee.sharding.ShardingSortStruct;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.HoneyUtil;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class SortListDecorator {

	/*public static void sort(List<?> rsList) {
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
	}*/

	public static <T> void sort(List<T> rsList) {

		Collections.sort(rsList, new Comparator<T>() {
			@Override
			public int compare(T a, T b) {
				ShardingSortStruct struct = HoneyContext.getCurrentShardingSort();
				if (struct == null) return 0;
				String orderFields[] = struct.getOrderFields();
				if (orderFields != null) {
					int i = 0;
					for (String orderField : orderFields) {
						int result = CompareUtil.compareTo(getValue(a, orderField),
								getValue(b, orderField), struct, i);
						if (result != 0) {
							return result;
						}
						i++;
					}
				}
				return 0;
			}
		});
	}

	private static <T> String getValue(T t, String fieldName) {
		if (t == null) return null;
		try {
			Field field = t.getClass().getDeclaredField(fieldName);
			HoneyUtil.setAccessibleTrue(field);
			Object obj = field.get(t);
			if (obj != null) {
				return obj.toString();
			}
		} catch (IllegalAccessException | NoSuchFieldException e) {
			
		}
		return null;
	}

}
