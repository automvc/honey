/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Kingstar
 * @since  1.9
 */
public class FunAndOrderTypeMap {
	
	private static String max;
	private static String min;
	private static String sum;
	private static String avg;
	private static String count;
	
	private static String distinct;
	
	private static String desc;
	private static String asc;
	
	private static Map<String,String> typeMap=new HashMap<>();
	
	private FunAndOrderTypeMap() {}
	
	static {
		if (HoneyUtil.isSqlKeyWordUpper()) {
			max = "MAX";
			min = "MIN";
			sum = "SUM";
			avg = "AVG";
			count = "COUNT";
			
			distinct = "DISTINCT";
			
			desc="DESC";
			asc="ASC";
		} else {
			max = "max";
			min = "min";
			sum = "sum";
			avg = "avg";
			count = "count";
			
			distinct = "distinct";
			
			desc="desc";
			asc="asc";
		}
		
		typeMap.put("max", max);
		typeMap.put("min", min);
		typeMap.put("sum", sum);
		typeMap.put("avg", avg);
		typeMap.put("count", count);
		
		typeMap.put("distinct", distinct);
		
		typeMap.put("desc", desc);
		typeMap.put("asc", asc);
		
	}
	
	public static String transfer(String type) {
		String t = typeMap.get(type);
		if (t != null)
			return t;
		else
			return type;
	}
}
