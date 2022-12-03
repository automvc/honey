/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.decorate;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.teasoft.bee.sharding.ShardingSortStruct;
import org.teasoft.honey.osql.core.HoneyContext;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class SortStringArrayListDecorator {

	public static void sort(List<String[]> list) {

		Collections.sort(list, new Comparator<String[]>() {
			@Override
			public int compare(String[] a, String[] b) {
				ShardingSortStruct struct = HoneyContext.getCurrentShardingSort();
				if (struct == null) return 0;
				int indexArray[] = struct.getIndex();
				if (indexArray != null) {
					int i = 0;
					for (int index : indexArray) {
						int result = CompareUtil.compareTo(a[index], b[index], struct, i);
						if (0 != result) {
							return result;
						}
						i++;
					}
				}
				return 0;
			}
		});
	}

}
