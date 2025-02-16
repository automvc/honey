/*
 * Copyright 2016-2024 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.algorithm;

//import java.util.HashMap;
//import java.util.Map;

import org.teasoft.bee.sharding.algorithm.Calculate;

/**
 * As example for Custom Calculate.
 * @author Kingstar
 * @since  2.4.0
 */
public class CustomCalculate implements Calculate {

	/**
	 * just example here.
	 */
	@Override
	public String process(String rule, String shardingValue) {

//		String t = shardingValue.substring(0, 7).replace("-", "");
////			if("202004".equals(t)) return "a";
////			else return "b";
//
//		// 直接定义映射关系；不一定是日期字段，其它字段值也可以；只是一个例子
//		Map<String, String> map = new HashMap<>();
//		map.put("202003", "a");
//		map.put("202004", "b");
//
//		String r = map.get(t);
//		if (r != null)
//			return r;
//		else
//			return "Default-Value"; 
//
//		// shardingValue为null时，不会流到这； MultiTenancy注解可以用默认值;但Sharding注解，ShardingBean会走全域查所有

		return ""; // just example here.

	}

}
