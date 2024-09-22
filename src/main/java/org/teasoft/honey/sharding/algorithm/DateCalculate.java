/*
 * Copyright 2016-2024 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.algorithm;

import org.teasoft.bee.sharding.algorithm.Calculate;

/**
 * @author Kingstar
 * @since  2.4.0
 */

public class DateCalculate implements Calculate{
 
	/**
	 * 默认获取日期的前6位字符.Get the first 6 characters of the date by default.
	 */
	@Override
	public String process(String rule, String dateString) {
		if (dateString == null) return "";
//		return dateString.substring(0, 7).replace("-", ""); //eg:"202004";
		return dateString.replace("-", "").replace("/", "").substring(0, dateString.length() >= 6 ? 6 : dateString.length());
		// shardingValue为null时，不会流到这； MultiTenancy注解可以用默认值;但Sharding注解，ShardingBean会走全域查所有
	}
}
