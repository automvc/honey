/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(aiteasoft@163.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.util;

import java.util.Map;

import org.teasoft.honey.osql.core.TokenUtil;

/**
 * @author Kingstar
 * @since  1.7.2
 */
public class StringUtil {

//	public static String replaceWithMap0(String text, Map<String,String> map) {
//		for (String key : map.keySet()) {
//			text=text.replace("#{"+key+"}", map.get(key));
//		}
//		return text;
//	}
	
	public static String replaceWithMap(String text, Map<String,String> map) {
		return TokenUtil.processWithMap(text, "#{", "}", map);
	}
	
	//v1.9
	public static String replaceWithMap(String text, Map<String,String> map,String startToken,String endToken) {
		return TokenUtil.processWithMap(text, startToken, endToken, map);
	}

}
