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
import org.teasoft.honey.osql.core.HoneyContext1;
import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.osql.core.StringConst;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class SortListDecorator {

	public static <T> void sort(List<T> rsList) {
//		ShardingSortStruct struct = HoneyContext.getCurrentShardingSort();
		ShardingSortStruct struct = (ShardingSortStruct)HoneyContext1.getExtForSharding(StringConst.ShardingSortStruct);

		Collections.sort(rsList, new Comparator<T>() {
			@Override
			public int compare(T a, T b) {
////				ShardingSortStruct struct = HoneyContext.getCurrentShardingSort();
//				ShardingSortStruct struct = (ShardingSortStruct)HoneyContext1.getExtForSharding(StringConst.ShardingSortStruct);
				if (struct == null) return 0;
				String orderFields[] = struct.getOrderFields();
				if (orderFields != null) {
					int i = 0;
					for (String orderField : orderFields) {
						int result = CompareUtil.compareTo(getValue(a, orderField), getValue(b, orderField), struct, i);
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
