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
	
	private static Log log=null; 
	
	//专门用于Bee框架输出SQL日志.
	public static void logSQL(String hardStr,String sql){
		
		if(!showSQL) return ;
		
		//=================================start
		//不能移走,它表示的是调用所在的位置.
		if (LoggerFactory.isNoArgInConstructor()) {
			log = _getLog(); //log4j   要解决同时处理两个构造函数
		} else {
			try {
				log = _getLog(sun.reflect.Reflection.getCallerClass().getName()); 
			} catch (Exception e) {
				log = _getLog(Logger.class.getName());
			}
		}
		//=================================end
		
		if(showSQL){
			List list;
			String insertIndex=(String)OneTimeParameter.getAttribute("_SYS_Bee_BatchInsert");
			if(insertIndex!=null){
//				批处理,在v1.8开始,不会用于占位设值. 需要清除
				list=HoneyContext.getPreparedValue(sql);
			}else{
				list=HoneyContext._justGetPreparedValue(sql);
			}
		
			String value=HoneyUtil.list2Value(list,showSQLShowType); 
			
			if (value == null || "".equals(value.trim())){
				_println("[Bee] "+hardStr, sql);
				if(showExecutableSql) _println("[Bee] ExecutableSql: "+hardStr, sql);  //无占位的情况
			}else{
				if(insertIndex!=null){
//					if("0".equals(insertIndex)){
//						_println("[Bee] "+hardStr, sql);
//					}
					print("--> index:"+insertIndex+" ,  [values]: "+ value);
					
				}else{
					_println("[Bee] "+hardStr, sql +"   [values]: "+ value);
				}
				
				if(showExecutableSql) {
					String executableSql=HoneyUtil.getExecutableSql(sql,list);
					if(showExecutableSql) _println("[Bee] ExecutableSql: "+hardStr, executableSql);
				}
			}
		}
	}
	
	private static void print(String s){
		log.info(s);
	}
	
	private static void _println(String s1,String s2){
//		log.info(s1+"\n"  +s2);
		log.info(s1  +s2);
	}
	
	
	public static void debug(String msg){
		
		//=================================start
		//不能移走,它表示的是调用所在的位置.
		if (LoggerFactory.isNoArgInConstructor()) {
			log = _getLog(); //log4j   要解决同时处理两个构造函数
		} else {
			try {
				log = _getLog(sun.reflect.Reflection.getCallerClass().getName()); 
			} catch (Exception e) {
				log = _getLog(Logger.class.getName());
			}
		}
		//=================================end
		
		log.debug(msg);
	}
	
	public static void info(String msg){
		
		//=================================start
		//不能移走,它表示的是调用所在的位置.
		if (LoggerFactory.isNoArgInConstructor()) {
			log = _getLog(); //log4j   要解决同时处理两个构造函数
		} else {
			try {
				log = _getLog(sun.reflect.Reflection.getCallerClass().getName()); 
			} catch (Exception e) {
				log = _getLog(Logger.class.getName());
			}
		}
		//=================================end
		
		log.info(msg);
	}
	
	public static void info(Number msg){
		
		log.info(msg+"");
	}
	
	public static void warn(String msg){
		
		if (LoggerFactory.isNoArgInConstructor()) {
			log = _getLog(); //log4j   要解决同时处理两个构造函数
		} else {
			//			System.out.println(sun.reflect.Reflection.getCallerClass().getName());  //不能移走,它表示的是调用所在的位置.
			try {
				log = _getLog(sun.reflect.Reflection.getCallerClass().getName()); //logback 类名可以显示正确,但会显示三层深度.
			} catch (Exception e) {
				log = _getLog(Logger.class.getName());
			}
		}
		
		
		log.warn(msg);
	}
	public static void warn(Number msg){
		log.warn(msg+"");
	}
	
	public static void error(String msg){
		
		if (LoggerFactory.isNoArgInConstructor()) {
			log = _getLog(); //log4j   要解决同时处理两个构造函数
		} else {
			//			System.out.println(sun.reflect.Reflection.getCallerClass().getName());  //不能移走,它表示的是调用所在的位置.
			try {
				log = _getLog(sun.reflect.Reflection.getCallerClass().getName()); //logback 类名可以显示正确,但会显示三层深度.
			} catch (Exception e) {
				log = _getLog(Logger.class.getName());
			}
		}
		
		
		log.error(msg);
	}
	
	public static void error(Number msg){
		log.error(msg+"");
	}
	
	
	private  static Log _getLog(String className){
		Log  log=LoggerFactory.getLog(className);
		return log;
	}
	
	private  static Log _getLog(){
		Log  log=LoggerFactory.getLog();
		return log;
	}
	
/*	private static void resetLog(){
//			Log log;
			if (LoggerFactory.isLog4j()) {
				log = _getLog(); //log4j   要解决同时处理两个构造函数
			} else {
				System.out.println(sun.reflect.Reflection.getCallerClass().getName());  //不能移走,它表示的是调用所在的位置.
				try {
					log = _getLog(sun.reflect.Reflection.getCallerClass().getName()); //logback 类名可以显示正确,但会显示三层深度.
				} catch (Exception e) {
					log = _getLog(Logger.class.getName());
				}

			}
	   }*/
}
