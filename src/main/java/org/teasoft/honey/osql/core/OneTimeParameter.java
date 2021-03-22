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
 * @since  1.8
 */
class OneTimeParameter {
	
	private static ThreadLocal<Map<String, Object>> local= new ThreadLocal<>();
	
//	private static final String twiceFix = "<:twiceFix:Bee>";
//	private static final String needTwiceKey = "<:needTwice:Key:Bee>"; 
	
	private OneTimeParameter() {}

	private static Object _getAttribute(String key) {
		
		Map<String, Object> map = local.get();
		if (null == map) return null;

		Object obj = map.get(key);
		map.remove(key);  //取后即删
		return obj;
	}
	
	public static Object getAttribute(String key) {
		Object value = _getAttribute(key);
//		if (value == null) value = _getAttribute(key + twiceFix);
		return value;
	}

	public static void setAttribute(String key, Object obj) {
		if (obj == null) return;
		Map<String, Object> map = local.get();
		if (null == map) map = new ConcurrentHashMap<>();   //TODO 使用弱引用???
		map.put(key, obj); 
		local.set(map);
	}
	
	/**
	 * @param key
	 * @param obj
	 * @since 1.9
	 */
//	public static void setAttributeTwice(String key, Object obj) {
//		if (obj == null) return;
//		Map<String, Object> map = local.get();
//		if (null == map) map = new ConcurrentHashMap<>();
//		map.put(key, obj); 
//		map.put(key+twiceFix, obj); 
//		local.set(map);
//	}
	
	public static void setTrueForKey(String key) {
		setAttribute(key, StringConst.tRue);
	}
	
	public static boolean isTrue(String key) {
		Object value = _getAttribute(key);
		return StringConst.tRue.equals(value) ? true : false;
	}
	
//	public static void needTwice() {
//		setAttribute(needTwiceKey,StringConst.tRue);
//	}
//	
//	public static boolean isNeedTwice() {
//		Object value = _getAttribute(needTwiceKey);
//	    return StringConst.tRue.equals(value) ? true : false;
//	}
	
//	public static void main(String[] args) {
//		
////		OneTimeParameter.setAttribute("AAA",111);
//		OneTimeParameter.setAttributeTwice("AAA",111);
//		System.out.println(getAttribute("AAA"));
//		System.out.println(getAttribute("AAA"));
//		System.out.println(getAttribute("AAA"));
//		
//		needTwice();
//		System.out.println(isNeedTwice());
//	}
}
