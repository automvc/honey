/**
*@author Kingstar
*Create on 2013-6-30 下午11:19:00
*/
package org.honey.osql.core;

public class ConverString {
	public static String inital(String s) {

		//char c=s.charAt(0);
		String temp = s.substring(0, 1);
		temp = temp.toUpperCase();
		//String sub=s.substring(1);

		return temp + s.substring(1);
	}

	public static String getClassName(String className) {
		int index = className.lastIndexOf(".");
		return className.substring(index + 1);

	}

	public static String getClassName(Object obj) {

		String className = obj.getClass().getName();

		int index = className.lastIndexOf(".");
		return className.substring(index + 1);

	}

	public static String getTableName(Object obj) {

		String s = getClassName(obj);
		
		String firstLetter=s.substring(0,1).toLowerCase();
		s=firstLetter+s.substring(1);
		return transformStr(s);
		//return s.toLowerCase();

	}

	private static String transformStr(String str) {
		if (HoneyConfig.getHoneyConfig().isUnderScoreAndCamelTransform()) {
			return HoneyUtil.toUnderscoreNaming(str);
		} else {
			return str;
		}
	}

}
