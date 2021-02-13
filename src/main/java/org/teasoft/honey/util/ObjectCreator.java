/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util;

/**
 * @author Kingstar
 * @since  1.9
 */
public class ObjectCreator {

	public static Long createLong(String s) {
		if (StringUtils.isBlank(s)) return null;
		return Long.parseLong(s);
	}

	public static Integer createInt(String s) {
		if (StringUtils.isBlank(s)) return null;
		return Integer.parseInt(s);
	}

	public static String createString(String s) {
		return s;
	}

	public static Short createShort(String s) {
		if (StringUtils.isBlank(s)) return null;
		return Short.parseShort(s);
	}

	public static Byte createByte(String s) {
		if (StringUtils.isBlank(s)) return null;
		return Byte.parseByte(s);
	}

	public static Boolean createBoolean(String s) {
		if (StringUtils.isBlank(s)) return false;
		return Boolean.parseBoolean(s);
	}

	public static Double createDouble(String s) {
		if (StringUtils.isBlank(s)) return null;
		return Double.parseDouble(s);
	}

}
