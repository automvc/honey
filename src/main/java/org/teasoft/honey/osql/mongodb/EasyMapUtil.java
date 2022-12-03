/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.mongodb;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Jade
 * @since  2.0
 */
public class EasyMapUtil{
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map createMap(Object key,Object value) {
		Map map=new LinkedHashMap();
		map.put(key, value);
		return map;
	}

}
