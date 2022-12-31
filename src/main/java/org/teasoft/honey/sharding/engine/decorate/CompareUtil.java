/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.decorate;

import org.teasoft.bee.osql.OrderType;
import org.teasoft.bee.sharding.ShardingSortStruct;
import org.teasoft.honey.util.StringUtils;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class CompareUtil {
	
	private static final String EMPTY = "";

	/**
	 * 
	 * @param thisValue
	 * @param otherValue
	 * @param struct
	 * @param i element index
	 * @return
	 */
	public static int compareTo(final String thisValue, final String otherValue,
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
		int t = thisValue.compareTo(otherValue);
		return OrderType.ASC == orderType ? t : -t;
	}

	private static Integer compareNumber(String thisValue, String otherValue,
			final String type) {
		boolean oneIsZero=false;
		if (StringUtils.isBlank(thisValue)) {
			thisValue = "0";
			oneIsZero=true;
		}
		if (StringUtils.isBlank(otherValue)) {
			if(oneIsZero) return 0;
			otherValue = "0";
		}
		
		Integer number = null;
		if ("Integer".equalsIgnoreCase(type) || "int".equalsIgnoreCase(type)
				|| "Short".equalsIgnoreCase(type) || "Byte".equalsIgnoreCase(type)
//				|| "short".equalsIgnoreCase(type) || "byte".equalsIgnoreCase(type)
				) {
			number = Integer.compare(Integer.parseInt(thisValue), Integer.parseInt(otherValue));
		} else if ("Long".equalsIgnoreCase(type) ) {
			number = Long.compare(Long.parseLong(thisValue), Long.parseLong(otherValue));
		} else if ("Double".equalsIgnoreCase(type)) {
			number = Double.compare(Double.parseDouble(thisValue),
					Double.parseDouble(otherValue));
		} else if ("Float".equalsIgnoreCase(type)) {
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
