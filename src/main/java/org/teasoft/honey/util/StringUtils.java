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
	 * 字符串数组转为用逗号分隔的字符串.string array to Strings separated by commas.
	 * @param stringArray string array.
	 * @return
	 */
	public static String toCommasString (String[] stringArray) {
		
		if (stringArray == null) return null;
		if (stringArray.length == 0) return "";
		if (stringArray.length == 1) return stringArray[1];
		String idsStr = "";
		for (int i = 0; i < stringArray.length; i++) {
			if (i != 0) idsStr += ",";
			idsStr += stringArray[i];
		}

		return idsStr;
	}
	
	/**
	 * 数字数组转为用逗号分隔的字符串.number array to Strings separated by commas.
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
	
	public static boolean justLikeChar(String name) {
		if(name==null) return false;
		String p = "^[%_]+$";
		Pattern pattern = Pattern.compile(p);
		return pattern.matcher(name).find();
	}
	
	//已转义的,unicode测不再转
	public static String escapeLike(String value) {
		if(value==null) return value;
		
//		return value.replace("%", "\\%").replace("_", "\\_");
		
		
		StringBuffer buf = new StringBuffer(value);
		char temp;
		for (int i = 0; i < buf.length(); i++) {
			temp=buf.charAt(i);
			if (temp=='\\') {
				i++;
			}else if (temp=='%' || temp=='_') {
				buf.insert(i++, '\\');
			}
		}
		return buf.toString();
	}
	
	  public static String getUnicode(String str) {
	        String strTemp = "";
	        if (str != null) {
	            for (char c : str.toCharArray()) {
	                if (c > 255) {
	                    strTemp += "\\u" + Integer.toHexString((int)c);
	                } else {
	                    strTemp += "\\u00" + Integer.toHexString((int)c);
	                }
	            }
	        }
	        return strTemp;
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
	
	public static void main(String[] args) {
		String s1="%";
		String s2="%_";
		String s3="_%";
		String s4="_";
		
		String s5="";
		String s6="%a";
		String s7="\\%";
		String s8="%bee";
		
		String s9="\u0025"; //%
		String s10="\u005f"; //_
		
		
		String s11="\u0025\u0025"; //%%
		String s12="\\u0025\\u0025";
		
		
		System.out.println(justLikeChar(s1));
		System.out.println(justLikeChar(s2));
		System.out.println(justLikeChar(s3));
		System.out.println(justLikeChar(s4));
		System.out.println(justLikeChar(s5));
		System.out.println(justLikeChar(s6));
		System.out.println(justLikeChar(s7));
		System.out.println(justLikeChar(s8));
		
		System.out.println(justLikeChar(s9));
		System.out.println(justLikeChar(s10));
		System.out.println(justLikeChar(s11));
		System.out.println(justLikeChar(s12));
		
		
		System.out.println(escapeLike(s1));
		System.out.println(escapeLike(s2));
		System.out.println(escapeLike(s3));
		System.out.println(escapeLike(s4));
		System.out.println(escapeLike(s5));
		System.out.println(escapeLike(s6));
		System.out.println(escapeLike(s7));
		System.out.println(escapeLike(s8));
		
		System.out.println(escapeLike(s9));
		System.out.println(escapeLike(s10));
		System.out.println(escapeLike(s11));
		System.out.println(escapeLike(s12));
		
//		System.out.println(getUnicode("若没有配置bee.dosql.multiDS.type,则根据具体情况确定数据源"));
//		System.out.println(getUnicode("则根据具体情况确定数据源"));
//		System.out.println(getUnicode("%"));
//		System.out.println(getUnicode("_"));
		

	}

	
}
