package org.teasoft.honey.osql.core;

import org.teasoft.bee.logging.Log;
import org.teasoft.honey.logging.LoggerFactory;

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
	
	//专门用于Bee框架输出SQL日志.
	public static void logSQL(String hardStr,String sql){
		if(showSQL){
			String value = HoneyContext.getSqlValue(sql);
			if (value == null || "".equals(value.trim()))
				_println("[Bee] "+hardStr, sql);
			else
				_println("[Bee] "+hardStr, sql +"   [values]: "+ value);
		}
	}
	
	public static void debug(String msg){
		log.debug(msg);
	}
	
	public static void info(String msg){
		log.info(msg);
	}
	
	public static void warn(String msg){
		log.warn(msg);
	}
	
	public static void error(String msg){
		log.error(msg);
	}
}
