package org.teasoft.honey.osql.core;

import java.util.List;

import org.teasoft.bee.logging.Log;
import org.teasoft.honey.logging.LoggerFactory;

/**
 * @author Kingstar
 * @since  1.0
 */
public class Logger {
	
	private static boolean  showSQL=HoneyConfig.getHoneyConfig().isShowSQL();
	private static boolean  showSQLShowType=HoneyConfig.getHoneyConfig().isShowSQLShowType();
	private static boolean  showExecutableSql=HoneyConfig.getHoneyConfig().isShowExecutableSql();
	
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
/*		if(showSQL){
			String value = HoneyContext.getSqlValue(sql);
			if (value == null || "".equals(value.trim()))
				_println("[Bee] "+hardStr, sql);
			else
				_println("[Bee] "+hardStr, sql +"   [values]: "+ value);
		}*/
		
		if(showSQL){
			List list=HoneyContext._justGetPreparedValue(sql);
			String value=HoneyUtil.list2Value(list,showSQLShowType); 
			String executableSql="";
			if (value == null || "".equals(value.trim())){
				_println("[Bee] "+hardStr, sql);
				if(showExecutableSql) _println("[Bee] ExecutableSql: "+hardStr, sql);  //无占位的情况
			}else{
				String insertIndex=(String)OneTimeParameter.getAttribute("_SYS_Bee_BatchInsert");
				if(insertIndex!=null){
					if("0".equals(insertIndex)){
						_println("[Bee] "+hardStr, sql);
					}
					   
					print("--> index:"+insertIndex+" ,  [values]: "+ value);
					
				}else{
					_println("[Bee] "+hardStr, sql +"   [values]: "+ value);
				}
				
				if(showExecutableSql) {
					executableSql=HoneyUtil.getExecutableSql(sql,list);
					if(showExecutableSql) _println("[Bee] ExecutableSql: "+hardStr, executableSql);
				}
			}
		}
	}
	
	public static void debug(String msg){
		log.debug(msg);
	}
	
	public static void info(String msg){
		log.info(msg);
	}
	
	public static void info(Number msg){
		log.info(msg+"");
	}
	
	public static void warn(String msg){
		log.warn(msg);
	}
	public static void warn(Number msg){
		log.warn(msg+"");
	}
	
	public static void error(String msg){
		log.error(msg);
	}
	
	public static void error(Number msg){
		log.error(msg+"");
	}
}
