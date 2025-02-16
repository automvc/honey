/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine;

import java.util.List;

import org.teasoft.bee.osql.FunctionType;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.StringConst;
import org.teasoft.honey.util.StringUtils;
import org.teasoft.honey.util.currency.CurrencyArithmetic;

/**
 * @author Kingstar
 * @since  2.0
 */
public class ShardingFunResultEngine {

	public static String funResultEngine(List<String> rsList) {
		String funType = HoneyContext.getSysCommStrInheritableLocal(StringConst.FunType);
		Double temp = 0D;
		int position = -1;
		if (FunctionType.MAX.getName().equalsIgnoreCase(funType)) {
			boolean first = true;
			for (int i = 0; i < rsList.size(); i++) {

				if (StringUtils.isNotBlank(rsList.get(i))) {
					double d = Double.parseDouble(rsList.get(i));
					if (first) {
						temp = d;
						first = false;
						position = i;
					} else if (d > temp) {
						temp = d;
						position = i;
					}
				}
			}
		} else if (FunctionType.MIN.getName().equalsIgnoreCase(funType)) {
			boolean first = true;
			for (int i = 0; i < rsList.size(); i++) {
				if (StringUtils.isNotBlank(rsList.get(i))) {
					double d = Double.parseDouble(rsList.get(i));
					if (first) {
						temp = d;
						first = false;
						position = i;
					} else if (d < temp) {
						temp = d;
						position = i;
					}
				}
			}
		} else if (FunctionType.SUM.getName().equalsIgnoreCase(funType)) {
			boolean first = true;
			String sum = "0";
			for (int i = 0; i < rsList.size(); i++) {
				if (StringUtils.isNotBlank(rsList.get(i))) {
					sum = CurrencyArithmetic.add(sum, rsList.get(i));
					if (first) {
						first = false;
					}
				}
			}
			if (!first) return sum;
		} else if (FunctionType.COUNT.getName().equalsIgnoreCase(funType)) {
			long c = 0;
			for (int i = 0; i < rsList.size(); i++) {
				if (StringUtils.isNotBlank(rsList.get(i))) {
					long r = Long.parseLong(rsList.get(i));
					c += r;
				}
			}
			return c + "";
		}

		if (FunctionType.MAX.getName().equalsIgnoreCase(funType)
				|| FunctionType.MIN.getName().equalsIgnoreCase(funType)) {
			if (position >= 0) return rsList.get(position); // 直接返回原来的元素,防止转换过程,精度有变化
		}

		return "";
	}

}
