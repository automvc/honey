/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core.name;


/**
 * @author Kingstar
 * @since  1.5
 */
public class NameUtil {
	
	public static String getClassFullName(Object obj) {
		return obj.getClass().getName();  //include package name
	}
	
	public static String getClassName(Object obj) {
		return obj.getClass().getSimpleName();
	}
	
	/**
	 * @param name
	 * @return UnderscoreNaming String
	 * eg: beeName->bee_name,beeTName->bee_t_name
	 */
	public static String toUnderscoreNaming(String name) {
		StringBuffer buf = new StringBuffer(name);
		for (int i = 1; i < buf.length() - 1; i++) {
			if (Character.isUpperCase(buf.charAt(i))) {
				buf.insert(i++, '_');
			}
		}
		return buf.toString().toLowerCase();
	}
	
	/**
	 * @param name
	 * @return a string of CamelNaming
	 * eg:  bee_name->beeName,bee_t_name->beeTName
	 */
	public static String toCamelNaming(String name){
//		StringBuffer buf = new StringBuffer(name.toLowerCase()); //HELLO_WORLD->HelloWorld 字段名有可能是全大写的
		//在具体接口实现类会先转成小写(如果有必要)
		StringBuffer buf = new StringBuffer(name.trim());
		char temp;
		for (int i = 1; i < buf.length() - 1; i++) {
			temp=buf.charAt(i);
			if (buf.charAt(i)=='_') {
				buf.deleteCharAt(i);
				temp=buf.charAt(i);
				if(temp>='a' && temp<='z')
				    buf.setCharAt(i, (char)(temp-32));
			}
		}
		return buf.toString();
	}
	
	/*
	 * 首字母转换成大写
	 */
	public static  String firstLetterToUpperCase(String str) {
		String result = "";
		if (str.length() > 1) {
			result = str.substring(0, 1).toUpperCase()+ str.substring(1);
		} else {
			result = str.toUpperCase();
		}

		return result;
	}
	
	
/*	public static void main(String[] args) {
		System.out.println(getClassFullName(Class.class));
		
		System.out.println(getClassFullName(User.class));
		System.out.println(getClassFullName(new User()));
		
//		beeName->bee_name,beeTName->bee_t_name
		System.out.println(toUnderscoreNaming("beeName"));
		System.out.println(toUnderscoreNaming("beeTName"));
		
//		bee_name->beeName,bee_t_name->beeTName
		System.out.println(toCamelNaming("bee_name"));
		System.out.println(toCamelNaming("bee_t_name"));
		
		System.out.println(toCamelNaming("BEE_NAME"));
		System.out.println(toCamelNaming("BEE_T_NAME"));
	}*/

}
