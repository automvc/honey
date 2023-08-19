/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util.currency;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author Kingstar
 * @since  2.0
 */
public class CurrencyArithmetic {
	
	private CurrencyArithmetic() {
		
	}

//	private static int DEFAULT_ROUND_MODE=BigDecimal.ROUND_HALF_UP;
	private static RoundingMode DEFAULT_ROUND_MODE = RoundingMode.HALF_UP;
	private static int SCALE = 2;

	/**
	 * a+b
	 * @param a
	 * @param b
	 * @return the result of a+b
	 */
	public static String add(String a, String b) {
		BigDecimal one = new BigDecimal(a);
		BigDecimal two = new BigDecimal(b);

		BigDecimal r = one.add(two);

		return r.toPlainString();
	}

	/**
	 * a-b
	 * @param a
	 * @param b
	 * @return the result of a-b
	 */
	public static String subtract(String a, String b) {
		BigDecimal one = new BigDecimal(a);
		BigDecimal two = new BigDecimal(b);

		BigDecimal r = one.subtract(two);

		return r.toPlainString();
	}

	/**
	 * a*b
	 * @param a
	 * @param b
	 * @return the result of a*b
	 */
	public static String multiply(String a, String b) {
		BigDecimal one = new BigDecimal(a);
		BigDecimal two = new BigDecimal(b);

		BigDecimal r = one.multiply(two);

		return r.toPlainString();
	}

	/**
	 * 
	 * @param a
	 * @param b
	 * @return the result of a/b
	 */
	public static String divide(String a, String b) {
//		BigDecimal de=new BigDecimal("1.5546");
//		BigDecimal one = new BigDecimal(a);
//		BigDecimal two = new BigDecimal(b);
//		BigDecimal r = one.divide(two, 2, BigDecimal.ROUND_HALF_UP);

		return divide(a, b, SCALE, DEFAULT_ROUND_MODE);
	}

	public static String divide(String a, String b, int scale, RoundingMode roundingMode) {
		BigDecimal one = new BigDecimal(a);
		BigDecimal two = new BigDecimal(b);

		BigDecimal r = one.divide(two, scale, roundingMode);

		return r.toPlainString();
	}

	/**
	 * 对两个操作数进行加减乘除运行 a op b  
	 * @param a
	 * @param op
	 * @param b
	 * @return result of (a op b)
	 */
	public static String calculate(String a, String op, String b) {
		return calculate(a, op, b, SCALE);
	}

	/**
	 * 对两个操作数进行加减乘除运行 a op b  
	 * 不是除法的,精度可以根据自身调整.
	 * @param a
	 * @param op
	 * @param b
	 * @param scale scale除法时会用到.
	 * @return result of (a op b)
	 */
	public static String calculate(String a, String op, String b, int scale) {
		return calculate(a, op, b, scale, DEFAULT_ROUND_MODE);
	}
	
	/**
	 * a % b
	 * @param a
	 * @param b
	 * @return
	 */
	public static String remainder(String a, String b) {
		BigDecimal one = new BigDecimal(a);
		BigDecimal two = new BigDecimal(b);

		BigDecimal r = one.remainder(two);

		return r.toPlainString();
	}
	
	

	/**
	 * 对两个操作数进行加减乘除运行 a op b  
	 * 不是除法的,精度可以根据自身调整.
	 * @param a
	 * @param op
	 * @param b
	 * @param scale除法时会用到.
	 * @param roundingMode除法时会用到.
	 * @return result of (a op b)
	 */
	public static String calculate(String a, String op, String b, int scale, RoundingMode roundingMode) {
		if ("+".equals(op))
			return add(a, b);
		else if ("-".equals(op))
			return subtract(a, b);
		else if ("*".equals(op))
			return multiply(a, b);
		else if ("/".equals(op)) 
			return divide(a, b, scale, roundingMode); //V2.1.8
		else if ("%".equals(op))
			return remainder(a, b);
		return null;
	}

}
