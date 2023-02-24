/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Kingstar
 * @since  2.1
 */
public class Converter {

	/**
	 * Map to Properties
	 * @param map
	 * @return
	 */
	public static Properties map2Prop(Map<String, String> map) {
		if (map == null) return null;

		Properties p = new Properties();
		for (Map.Entry<String, String> entry : map.entrySet()) {
			p.setProperty(entry.getKey(), entry.getValue());
		}
		return p;
	}
	
	/**
	 * change the key of map, eg: driver-class-name -> driverClassName
	 * @param map
	 * @return
	 */
	public static Map<String, String> transferKey(Map<String, String> map) {
		if (map == null) return null;

		Map<String, String> map2 = new LinkedHashMap<>(map.size());
		for (Map.Entry<String, String> entry : map.entrySet()) {
			map2.put(transfer(entry.getKey()), entry.getValue());
		}
		return map2;
	}
	
	/**
	 * eg: driver-class-name -> driverClassName
	 * @param str
	 * @return
	 */
	public static String transfer(String str) {
		if (StringUtils.isBlank(str)) return str;
		
		StringBuffer buf=new StringBuffer(str);
		for (int i = 0; i < buf.length(); i++) {
			if('-'==buf.charAt(i)) {
				buf.deleteCharAt(i);
				if(i<buf.length() ) {
					char temp=buf.charAt(i);
					if(temp>='a' && temp<='z')
					    buf.setCharAt(i, (char)(temp-32));
				}
			}
		}
		return buf.toString();
	}
	
//	public static void main(String[] args) {
//		System.err.println(transfer("driver-class-name")); //driverClassName
//	}

}
