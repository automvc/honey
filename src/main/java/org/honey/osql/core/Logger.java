package org.honey.osql.core;

/**
 * @author KingStar
 * @since  1.0
 */
public class Logger {
	
	private static boolean  showSQL=HoneyConfig.getHoneyConfig().isShowSQL();
	
	public static void print(String s1,String s2){
		System.out.println(s1+" :  "  +s2);
	}
	
	public static void println(String s1,String s2){
		System.out.println(s1+"\n"  +s2);
	}
	
	private static void _println(String s1,String s2){
		System.out.println(s1+"\n"  +s2);
	}
	
	public static void logSQL(String hardStr,String sql){
//		System.out.println("select SQL(entity,size):"+sql);
		if(showSQL){
			String value = HoneyContext.getSqlValue(sql);
			if (value == null || "".equals(value.trim()))
				_println(hardStr, sql);
			else
				_println(hardStr, sql +"   [values]: "+ value);
		}
	}
}
