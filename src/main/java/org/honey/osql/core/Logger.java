package org.honey.osql.core;

import org.bee.logging.Log;
import org.honey.logging.LoggerFactory;

/**
 * @author Kingstar
 * @since  1.0
 */
public class Logger {
	
	private static boolean  showSQL=HoneyConfig.getHoneyConfig().isShowSQL();
	
	final static Log log=LoggerFactory.getLogger();
	
	public static void print(String s){
		log.info(s);
	}
	
	public static void print(String s1,String s2){
		log.info(s1+" :  "  +s2);
	}
	
	public static void println(String s1,String s2){
		log.info(s1+"\n"  +s2);
	}
	
	private static void _println(String s1,String s2){
		log.info(s1+"\n"  +s2);
	}
	
	public static void logSQL(String hardStr,String sql){
		if(showSQL){
			String value = HoneyContext.getSqlValue(sql);
			if (value == null || "".equals(value.trim()))
				_println(hardStr, sql);
			else
				_println(hardStr, sql +"   [values]: "+ value);
		}
	}
}
