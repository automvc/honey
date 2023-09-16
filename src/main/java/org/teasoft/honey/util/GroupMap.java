/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kingstar
 * @since  2.1
 */
public class GroupMap {
	
	Map<String,Map<String,Object>> map=new HashMap<>(); 
	
	public void add(String tag,String key, Object value) {
		Map<String,Object> gMap=map.get(tag);
		if(gMap==null) gMap=new HashMap<>();
		gMap.put(key, value);
		map.put(tag, gMap);
	}
	
	/**
	 * 将tag排序后,转为list
	 * @return
	 */
	public List<Map<String, Object>> toList() {
		String tagsArray[] = new String[map.size()];
		map.keySet().toArray(tagsArray);
		Arrays.sort(tagsArray);

		List<Map<String, Object>> list = new ArrayList<>();

		for (int i = 0; i < tagsArray.length; i++) {
			list.add(map.get(tagsArray[i]));
		}

		return list;
	}

}
