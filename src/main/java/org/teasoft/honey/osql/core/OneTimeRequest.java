/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Kingstar
 * @since  1.7.3
 */
public class OneTimeRequest {
	
	private static ThreadLocal<Map<String, Object>> local= new ThreadLocal<>();

	private OneTimeRequest() {}

	public static Object getAttribute(String key) {
		
		Map map = local.get();
		if (null == map) return null;

		Object obj = map.get(key);
		map.remove(key);  //取后即删
		return obj;
	}

	public static void setAttribute(String key, Object obj) {
		if (obj == null) return;
		Map<String, Object> map = local.get();
		if (null == map) map = new ConcurrentHashMap<>();
		map.put(key, obj); 
		local.set(map);
	}

}
