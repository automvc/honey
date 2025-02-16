/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.name;

/**
 * @author Kingstar
 * @since  1.5
 */
public class NameUtil {

	private NameUtil() {}

	public static String getClassFullName(Object obj) {
		return obj.getClass().getName(); // include package name
	}

	public static String getClassName(Object obj) {
		return obj.getClass().getSimpleName();
	}

	/**
	 * 驼峰转下划线命名.CamelNaming to UnderscoreNaming
	 * @param name
	 * @return UnderscoreNaming String
	 * eg: beeName->bee_name,beeTName->bee_t_name
	 */
	public static String toUnderscoreNaming(String name) {
		StringBuffer buf = new StringBuffer(name);
		for (int i = 1; i < buf.length(); i++) {
			if (Character.isUpperCase(buf.charAt(i))) {
				buf.setCharAt(i, (char) (buf.charAt(i) + 32));
				buf.insert(i++, '_');
			}
		}
		return buf.toString();
	}

	/**
	 * 下划线转驼峰命名.UnderscoreNaming to CamelNaming
	 * 首个与最后一个若是下划线则不会处理
	 * @param name
	 * @return a string of CamelNaming
	 * eg:  bee_name->beeName,bee_t_name->beeTName
	 */
	public static String toCamelNaming(String name) {
//		StringBuffer buf = new StringBuffer(name.toLowerCase()); //HELLO_WORLD->HelloWorld 字段名有可能是全大写的
		// 在具体接口实现类可先转成小写(如果有必要)
		StringBuffer buf = new StringBuffer(name.trim());
		char temp;
		for (int i = 1; i < buf.length() - 1; i++) {
			temp = buf.charAt(i);
			if (temp == '_') {
				buf.deleteCharAt(i);
				temp = buf.charAt(i);
				if (temp >= 'a' && temp <= 'z') buf.setCharAt(i, (char) (temp - 32));
			}
		}
		return buf.toString();
	}

	/**
	 * 首字母转换成大写
	 * @param str
	 * @return 处理后的字符串
	 */
	public static String firstLetterToUpperCase(String str) {
		String result = "";
		if (str.length() > 1) {
			result = str.substring(0, 1).toUpperCase() + str.substring(1);
		} else {
			result = str.toUpperCase();
		}

		return result;
	}

	/**
	 * 首字母转换成小写
	 * @param str 
	 * @return 处理后的字符串
	 */
	public static String firstLetterToLowerCase(String str) {
		String result = "";
		if (str.length() > 1) {
			result = str.substring(0, 1).toLowerCase() + str.substring(1);
		} else {
			result = str.toLowerCase();
		}

		return result;
	}

}
