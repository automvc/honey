/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author Kingstar
 * @since  1.9
 */
public class ObjectCreator {
	
	private ObjectCreator() {}

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
	
	public static Float createFloat(String s) {
		if (StringUtils.isBlank(s)) return null;
		return Float.parseFloat(s);
	}
	
	/**
	 * create BigDecimal value
	 * @param v
	 * @return BigDecimal value
	 * @since 2.0
	 */
	public static BigDecimal createBigDecimal(String v) {
		return new BigDecimal(v);
	}
	
	/**
	 * create BigInteger value
	 * @param v
	 * @return BigInteger value
	 * @since 2.0
	 */
	public static BigInteger createBigInteger(String v) {
		return new BigInteger(v);
	}

}
