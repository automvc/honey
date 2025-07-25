///*
// * Copyright 2016-2022 the original author.All rights reserved.
// * Kingstar(honeysoft@126.com)
// * The license,see the LICENSE file.
// */
//
//package org.teasoft.honey.util;
//
//import java.util.Map;
//import java.util.TreeMap;
//
//import org.teasoft.bee.osql.LowerKey;
//import org.teasoft.honey.osql.core.HoneyUtil;
//
///**
// * @author Kingstar
// * @since  1.11
// */
//public class SqlKeyCheck {
//
//	private static final String ONE = "1";
//	private static Map<String, String> keyMap = new TreeMap<>();
//	private static final String keyStr = "table,column,key," + "Explain,comment,key" + "group,By,order,Null,is,for,"
//			+ "inner,left,right,join";
//
//	static {
//		initKeyMap(keyStr);
//
//		if (HoneyUtil.isOracle()) {
//			String oracleKey = "user,level,role";
//			initKeyMap(oracleKey);
//		}
//
//		String fs = EntityUtil.getColumnNames(new LowerKey());
//		initKeyMap2(fs);
//	}
//
//	private SqlKeyCheck() {}
//
//	private static void initKeyMap(String keyStr) {
//		String keys[] = keyStr.split(",");
//		for (int i = 0; i < keys.length; i++) {
//			keyMap.put(keys[i].toLowerCase(), ONE);
//		}
//	}
//
//	// 不包含大写字母才是独立关键字
//	private static void initKeyMap2(String keyStr) {
//		String keys[] = keyStr.split(",");
//		for (int i = 0; i < keys.length; i++) {
//			if (!StringUtils.isContainUpperCase(keys[i])) keyMap.put(keys[i].toLowerCase(), ONE);
//		}
//	}
//
//	public static boolean isKeyWord(String word) {
//		if (keyMap.containsKey(word.toLowerCase()))
//			return true;
//		else
//			return false;
//	}
//
//}
