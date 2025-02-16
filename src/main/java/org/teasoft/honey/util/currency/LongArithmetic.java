/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util.currency;

/**
 * @author Kingstar
 * @since  1.11
 */
public class LongArithmetic {

	private LongArithmetic() {

	}

	public static String add(String a, String b) {
		Long a1 = Long.parseLong(a);
		Long b1 = Long.parseLong(b);

		return (a1 + b1) + "";
	}

	public static String subtract(String a, String b) {
		Long a1 = Long.parseLong(a);
		Long b1 = Long.parseLong(b);

		return (a1 - b1) + "";
	}

	public static String multiply(String a, String b) {
		Long a1 = Long.parseLong(a);
		Long b1 = Long.parseLong(b);

		return (a1 * b1) + "";
	}

	public static String divide(String a, String b) {
		Long a1 = Long.parseLong(a);
		Long b1 = Long.parseLong(b);

		return (a1 / b1) + "";
	}

	public static String remainder(String a, String b) {
		Long a1 = Long.parseLong(a);
		Long b1 = Long.parseLong(b);

		return (a1 % b1) + "";
	}

	public static String calculate(String a, String op, String b) {
		if ("+".equals(op))
			return add(a, b);
		else if ("-".equals(op))
			return subtract(a, b);
		else if ("*".equals(op))
			return multiply(a, b);
		else if ("/".equals(op))
			return divide(a, b);
		else if ("%".equals(op))
			return remainder(a, b);

		return null;
	}

}
