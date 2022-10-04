/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.decorate;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.teasoft.bee.osql.OrderType;
import org.teasoft.bee.sharding.ShardingSortStruct;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.util.StringUtils;

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
						int result = compareTo(a[index], b[index],
								struct, i);
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

	private static final String EMPTY = "";

	/**
	 * 
	 * @param thisValue
	 * @param otherValue
	 * @param struct
	 * @param i element index
	 * @return
	 */
	private static int compareTo(final String thisValue, final String otherValue,
			ShardingSortStruct struct, int i) {

		if (struct == null) return 0;

		OrderType orderType = struct.getOrderTypes()[i];
		boolean nullFirst = struct.getNullFirst() != null ? struct.getNullFirst()[i] : false;
		boolean caseSensitive = struct.getCaseSensitive() != null ? struct.getCaseSensitive()[i]
				: false;

		if (null == thisValue && null == otherValue) {
			return 0;
		}
		if (null == thisValue) {
			return nullFirst ? 1 : -1;
		}
		if (null == otherValue) {
			return nullFirst ? -1 : 1;
		}

		// Number compare
		String type = struct.getType() != null ? struct.getType()[i] : EMPTY;
		Integer number = compareNumber(thisValue, otherValue, type);
		if (number != null) return OrderType.ASC == orderType ? number : -number;

		if (!caseSensitive && thisValue instanceof String && otherValue instanceof String) {
			return compareCaseInsensitiveString((String) thisValue, (String) otherValue,
					orderType);
		}
		return OrderType.ASC == orderType ? thisValue.compareTo(otherValue)
				: -thisValue.compareTo(otherValue);
	}

	private static Integer compareNumber(String thisValue, String otherValue,
			final String type) {
		
		if (StringUtils.isBlank(thisValue)) thisValue = "0";
		if (StringUtils.isBlank(otherValue)) otherValue = "0";
		
		Integer number = null;
		if ("Integer".equalsIgnoreCase(type) || "int".equalsIgnoreCase(type)
				|| "Short".equalsIgnoreCase(type) || "Byte".equalsIgnoreCase(type)
				|| "short".equalsIgnoreCase(type) || "byte".equalsIgnoreCase(type)) {
			number = Integer.compare(Integer.parseInt(thisValue), Integer.parseInt(otherValue));
		} else if ("Long".equalsIgnoreCase(type) || "long".equalsIgnoreCase(type)) {
			number = Long.compare(Long.parseLong(thisValue), Long.parseLong(otherValue));
		} else if ("Double".equalsIgnoreCase(type) || "double".equalsIgnoreCase(type)) {
			number = Double.compare(Double.parseDouble(thisValue),
					Double.parseDouble(otherValue));
		} else if ("Float".equalsIgnoreCase(type) || "float".equalsIgnoreCase(type)) {
			number = Float.compare(Float.parseFloat(thisValue), Float.parseFloat(otherValue));

		}
		return number;
	}

	private static int compareCaseInsensitiveString(final String thisValue,
			final String otherValue, final OrderType orderDirection) {
		int n = thisValue.toLowerCase().compareTo(otherValue.toLowerCase());
		return OrderType.ASC == orderDirection ? n : -n;
	}

}
