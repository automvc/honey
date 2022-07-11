/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util;

import java.util.regex.Pattern;

/**
 * @author Kingstar
 * @since  1.9
 */
public final class StringUtils {
	
	private StringUtils() {}

	public static boolean isBlank(final String str) {
		return str == null || "".equals(str.trim());
	}

	public static boolean isNotBlank(final String str) {
		return str != null && !"".equals(str.trim());
	}

	public static boolean isEmpty(final String str) {
		return str == null || str.length() == 0;
	}

    public static boolean isNotEmpty(final String str) {
        return !isEmpty(str);
    }
    
    
	public static boolean isEmpty(final String strings[]) {
		return strings == null || strings.length == 0;
	}
	
	public static boolean isNotEmpty(final String strings[]) {
		return !isEmpty(strings);
	}
	
	public static boolean isContainUpperCase(String str) {
		if(isBlank(str)) return false;
		StringBuffer buf = new StringBuffer(str);
		for (int i = 0; i < buf.length(); i++) {
			if (Character.isUpperCase(buf.charAt(i))) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isContainLetter(String str) {
		if(isBlank(str)) return false;
		StringBuffer buf = new StringBuffer(str);
		for (int i = 0; i < buf.length(); i++) {
			if (Character.isLetter(buf.charAt(i))) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 字符串数组转用用逗号分隔的字符串.string array to Strings separated by commas.
	 * @param stringArray string array.
	 * @return
	 */
	public static String toCommasString (String[] stringArray) {
		
		if (stringArray == null) return null;
		if (stringArray.length == 0) return "";
		if (stringArray.length == 1) return stringArray[1];
		String idsStr = "";
		for (int i = 0; i < stringArray.length; i++) {
			idsStr += stringArray[i];
			if (i != stringArray.length - 1) idsStr += ",";
		}

		return idsStr;
	}
	
	/**
	 * 数字数组转用用逗号分隔的字符串.number array to Strings separated by commas.
	 * @param numArray number array.
	 * @return
	 */
	public static String toCommasString (Number[] numArray) {
		
		if (numArray == null) return null;
		if (numArray.length == 0) return "";
		if (numArray.length == 1) {
			if (numArray[1] == null) return null;
			else return numArray[1]+"";
		}
		String idsStr = "";
		for (int i = 0; i < numArray.length; i++) {
			idsStr += numArray[i];
			if (i != numArray.length - 1) idsStr += ",";
		}

		return idsStr;
	}
	
	
	public static boolean isInteger(String str) {
		Pattern pattern = Pattern.compile("^[-\\+]?[\\d]+$");
//		Pattern pattern = Pattern.compile("[-\\+]?[\\d]+");
		
//		"\d+"和"^\d+"
////		Pattern pattern = Pattern.compile("\\d+");
//		Pattern pattern = Pattern.compile("^\\d+");
		return pattern.matcher(str).matches();
	}

	/**
	 * 通过正则表达式判断字符串是否为数字
	 * @param str
	 * @return
	 */
	public static boolean isNumber(String str) {
		Pattern pattern = Pattern.compile("^[-\\+]?[0-9]+(\\\\.[0-9]+)?$");
		// 通过Matcher进行字符串匹配
		// 如果正则匹配通过 m.matches() 方法返回 true ，反之 false
		return pattern.matcher(str).matches();
	}
	
/*	public static void main(String[] args) {
		System.out.println(isInteger(""));
		System.out.println(isNumber(""));
		System.out.println(isInteger(" 0"));
		System.out.println(isNumber(" 0"));
		
		System.out.println(isInteger("-"));
		System.out.println(isNumber("-"));
		
		System.out.println(isInteger("+"));
		System.out.println(isNumber("+"));
		
		System.out.println(isInteger("+0"));
		System.out.println(isNumber("+0"));
		
		System.out.println(isInteger("-0"));
		System.out.println(isNumber("-0"));
		
		
		System.out.println(isInteger("a123456789"));
		System.out.println(isNumber("a123456789"));
		
	}*/

	
}
