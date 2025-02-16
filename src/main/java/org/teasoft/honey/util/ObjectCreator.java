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
		try {
			return Long.parseLong(s);
			// V2.1
		} catch (Exception e) {
			if (s.endsWith(".0")) {
				return Long.parseLong(s.substring(0, s.length() - 2));
			} else if (s.endsWith(".00")) {
				return Long.parseLong(s.substring(0, s.length() - 3));
			} else if (s.endsWith(".000")) {
				return Long.parseLong(s.substring(0, s.length() - 4));
			}
		}
		Double d = createDouble(s);
		if (d != null)
			return d.longValue();
		else
			return null;
	}

	public static Integer createInt(String s) {
		if (StringUtils.isBlank(s)) return null;
		try {
			return Integer.parseInt(s);
			// V2.1
		} catch (Exception e) {
			if (s.endsWith(".0")) {
				return Integer.parseInt(s.substring(0, s.length() - 2));
			} else if (s.endsWith(".00")) {
				return Integer.parseInt(s.substring(0, s.length() - 3));
			} else if (s.endsWith(".000")) {
				return Integer.parseInt(s.substring(0, s.length() - 4));
			}
		}

		Double d = createDouble(s);
		if (d != null)
			return d.intValue();
		else
			return null;
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
