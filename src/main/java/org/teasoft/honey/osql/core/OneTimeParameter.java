/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内部使用的一次性参数设置类.OneTime Parameter class.
 * 设置后,一定要使用,否则可能被其它地方获取产生错误.
 * @author Kingstar
 * @since  1.8
 */
class OneTimeParameter {
	
	private static ThreadLocal<Map<String, Object>> local= new ThreadLocal<>();
	
	private OneTimeParameter() {}

	static Object getAttribute(String key) {
		
		Map<String, Object> map = local.get();
		if (null == map) return null;

		Object obj = map.get(key);
		map.remove(key);  //取后即删
		return obj;
	}

	static void setAttribute(String key, Object obj) {
		if (obj == null) return;
		Map<String, Object> map = local.get();
		if (null == map) map = new ConcurrentHashMap<>();
		map.put(key, obj); 
		local.set(map);
	}
	
	static void setTrueForKey(String key) {
		setAttribute(key, StringConst.tRue);
	}
	
	public static boolean isTrue(String key) {
		Object value = getAttribute(key);
		return StringConst.tRue.equals(value) ? true : false;
	}
	
}
