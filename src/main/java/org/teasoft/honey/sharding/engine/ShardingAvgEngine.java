/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine;

import java.util.List;

import org.teasoft.honey.util.StringUtils;
import org.teasoft.honey.util.currency.CurrencyArithmetic;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class ShardingAvgEngine {

	// 改写为AVG分片用的SQL
	public static String rewriteAvgSql(String sql) {
		sql = sql.trim();
		String lowerSql = sql.toLowerCase();
		int startIndex = lowerSql.indexOf("select avg(");
		startIndex = startIndex + 11;
		int rightIndex = lowerSql.indexOf(')', startIndex);
		String funFieldName = sql.substring(startIndex, rightIndex);// 字段名

		String newFunStr = "sum(" + funFieldName + "),count(" + funFieldName + ") ";

		int rightIndex2 = lowerSql.indexOf("from ", startIndex);
		StringBuffer sb = new StringBuffer(sql);
		sb.replace(startIndex - 4, rightIndex2, newFunStr);// 要替换的

		return sb.toString();
	}

	// 处理并合并结果
	public static String avgResultEngine(List<String[]> listStrArray) {
		boolean first = true;
		String sum = "0";
		String count = "0";

		for (String str[] : listStrArray) {
			if (StringUtils.isNotBlank(str[0])) {
				if (first) {
					first = false;
				}
				sum = CurrencyArithmetic.add(sum, str[0]);
				count = CurrencyArithmetic.add(count, str[1]);
			}
		}

		if (!first) {
			return CurrencyArithmetic.divide(sum, count);
		} else {
			return "";
		}
	}
}
