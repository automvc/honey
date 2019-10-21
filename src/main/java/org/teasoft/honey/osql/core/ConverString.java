/**
*@author Kingstar
*Create on 2013-6-30 下午11:19:00
*/
package org.teasoft.honey.osql.core;

public class ConverString {
	public static String inital(String s) {
		String temp = s.substring(0, 1);
		temp = temp.toUpperCase();
		return temp + s.substring(1);
	}

	public static String getClassName(Object obj) {

//		String className = obj.getClass().getName();
//		int index = className.lastIndexOf(".");
//		return className.substring(index + 1);
		return obj.getClass().getSimpleName();

	}
	
	public static String getTableName(Class c) {
//		String className=c.getName();
//		int index = className.lastIndexOf(".");
//		String s=className.substring(index + 1);
		
		String s=c.getSimpleName();
		String firstLetter=s.substring(0,1).toLowerCase();
		s=firstLetter+s.substring(1);
		return transformStr(s);
	}

	public static String getTableName(Object obj) {
		String s = getClassName(obj);
		String firstLetter=s.substring(0,1).toLowerCase();
		s=firstLetter+s.substring(1);
		return transformStr(s);
	}
	
	private static String transformStr(String str) {
		if (HoneyConfig.getHoneyConfig().isUnderScoreAndCamelTransform()) {
			return HoneyUtil.toUnderscoreNaming(str);
		} else {
			return str;
		}
	}

}
