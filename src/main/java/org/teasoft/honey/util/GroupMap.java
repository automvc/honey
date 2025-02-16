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

	Map<String, Map<String, String>> map = new HashMap<>();

	public GroupMap() {}

	public GroupMap(Map<String, Map<String, String>> initMap) {
		this.map.putAll(initMap);
	}

	/**
	 * @param tag  group tag
	 * @param key  map key
	 * @param value map value
	 */
	public void add(String tag, String key, String value) {
		Map<String, String> gMap = map.get(tag);
		if (gMap == null) gMap = new HashMap<>();
		gMap.put(key, value);
		map.put(tag, gMap);
	}

	/**
	 * 将tag排序后,转为list
	 * @return
	 */
	public List<Map<String, String>> toList() {
		String tagsArray[] = new String[map.size()];
		List<Map<String, String>> list = new ArrayList<>();
		if (tagsArray.length == 0) return list;

		map.keySet().toArray(tagsArray);
		Arrays.sort(tagsArray);

		for (int i = 0; i < tagsArray.length; i++) {
			list.add(map.get(tagsArray[i]));
		}

		return list;
	}

	/**
	 * @return
	 * @since 2.1.10
	 */
	public Map<String, Map<String, String>> getMap() {
		return map;
	}

	/**
	 * 
	 * @return boolean value whether GroupMap instance is empty.
	 * @since 2.1.10
	 */
	public boolean isEmpty() {
		return map.isEmpty();
	}

//	public static void main(String[] args) {
//		GroupMap a =new GroupMap();
//		
//		String tagsArray[] = new String[0];
//		System.out.println(tagsArray.length);
//		System.out.println(a.toList());
//	}

}
