/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util;

import java.util.Random;

/**
 * @author Kingstar
 * @since  2.1.8
 */
public class NumUtil {
	
	private static final String n = "0123456789";
	private static final char[] num = n.toCharArray();
	private static Random random = new Random();
	
	private NumUtil() {}
	
	/**
	 * get Random number, the amount is size.
	 * @param size
	 * @return random number,the amount is size.
	 */
	public static String getRandomNum(int size) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < size; i++) {
			s.append(getOneNum());
		}
		return s.toString();
	}

	/**
	 * 6 Random number.
	 * @return 6 Random number
	 */
	public static String getRandomNum6() {
		return getRandomNum(6);
	}

	private static char getOneNum() {
		int a = random.nextInt(num.length);
		return num[a];
	}

	
	/**
	 * 检测是否是11位手机号.
	 * @param mobileNum
	 * @return
	 */
	public static boolean isMobileNum11(String mobileNum) {
		String regex = "^1\\d{10}$";
		boolean isMatch = mobileNum.matches(regex);
		return isMatch;
	}

}
