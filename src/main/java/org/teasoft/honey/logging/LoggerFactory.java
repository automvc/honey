/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.logging;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.teasoft.bee.logging.Log;
import org.teasoft.honey.osql.core.HoneyConfig;

/**
 * @author Kingstar
 * @since  1.4
 */
public class LoggerFactory {

	private static Constructor<? extends Log> logConstructor;
	private static Constructor<? extends Log> logNoArgConstructor;
	
	private static ThreadLocal<Map<String, Log>> logLocal;
	
	static {
		logLocal=new ThreadLocal<>();
	}
	
	private static boolean isNoArgInConstructor;
	
	static {
        
		String loggerType = HoneyConfig.getHoneyConfig().loggerType;
		if (loggerType != null && !"".equals(loggerType.trim())) {
			loggerType=loggerType.trim();
			if (loggerType.equalsIgnoreCase("log4j")) {
				tryImplementation("org.apache.log4j.Logger",              "org.teasoft.beex.logging.Log4jImpl"); //优先选择log4j
			
			} else if (loggerType.equalsIgnoreCase("slf4j")) {
				tryImplementation("org.slf4j.Logger",                      "org.teasoft.beex.logging.Slf4jImpl"); //ok,只是要显示多层
			
			} else if (loggerType.equalsIgnoreCase("log4j2")) {
				tryImplementation("org.apache.logging.log4j.Logger",       "org.teasoft.beex.logging.Log4j2Impl"); //Log4j2
			
			} else if (loggerType.equalsIgnoreCase("commonsLog")) {
				tryImplementation("org.apache.commons.logging.LogFactory", "org.teasoft.beex.logging.JakartaCommonsLoggingImpl");//无法显示调用类的信息
			
			} else if (loggerType.equalsIgnoreCase("systemLogger")) {//std
				tryImplementation("",                                      "org.teasoft.honey.logging.SystemLogger");
				
			} else if (loggerType.equalsIgnoreCase("fileLogger")) {
				tryImplementation("",                                      "org.teasoft.honey.logging.FileLogger");
			
			} else if (loggerType.equalsIgnoreCase("noLogging")) {
				tryImplementation("",                                      "org.teasoft.honey.logging.NoLogging");
			
			} else if (loggerType.equalsIgnoreCase("jdkLog")) {
				tryImplementation("java.util.logging.Logger",               "org.teasoft.honey.logging.Jdk14LoggingImpl");//会随着传入的class变化.无行数输出
			}
		}
		
		tryImplementation("org.apache.log4j.Logger",              "org.teasoft.beex.logging.Log4jImpl"); //优先选择log4j
		tryImplementation("org.slf4j.Logger",                     "org.teasoft.beex.logging.Slf4jImpl"); //ok,只是要显示多层
		tryImplementation("org.apache.logging.log4j.Logger",       "org.teasoft.beex.logging.Log4j2Impl"); //Log4j2
		tryImplementation("org.apache.commons.logging.LogFactory", "org.teasoft.beex.logging.JakartaCommonsLoggingImpl");//无法显示调用类的信息
		tryImplementation("",                                       "org.teasoft.honey.logging.SystemLogger");
		tryImplementation("",                                       "org.teasoft.honey.logging.FileLogger");
		tryImplementation("",                                       "org.teasoft.honey.logging.NoLogging");
		tryImplementation("java.util.logging.Logger",               "org.teasoft.honey.logging.Jdk14LoggingImpl");//会随着传入的class变化.无行数输出
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void tryImplementation(String testClassName, String implClassName) {
		if (logConstructor != null) return;

		if (isNoArgInConstructor && logNoArgConstructor != null) return;

		try {
			if (implClassName != null) {
				if (implClassName.endsWith(".Log4jImpl") || implClassName.endsWith(".Slf4jImpl") 
				 || implClassName.endsWith(".SystemLogger") || implClassName.endsWith(".NoLogging")
				 || implClassName.endsWith(".FileLogger")
				 
						) {
					try {
						if (testClassName != null && !"".equals(testClassName)) genClassByName(testClassName); //测试是否存在.如果不存在,则跳到catch

						Class implClassNoArg = genClassByName(implClassName);
						logNoArgConstructor = implClassNoArg.getConstructor();
						isNoArgInConstructor = true;

					} catch (ClassNotFoundException e) {
						// ignore
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
			}

			if (testClassName != null && !"".equals(testClassName)) genClassByName(testClassName); //测试是否存在 
			Class implClass = genClassByName(implClassName);
			logConstructor = implClass.getConstructor(new Class[] { String.class });

			System.out.println("[Bee] LoggerFactory Use the Logger is : " + implClassName);

		} catch (ClassNotFoundException e) {
			// ignore
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public static Log getLog() {
		  
		Log cacheLog=getCacheInfo("NoArg");
		if(cacheLog!=null) return cacheLog;
		
		Log log = null;
		try {
			if (isNoArgInConstructor) {
				log = logNoArgConstructor.newInstance();
				setCacheInfo("NoArg",log);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (log != null) return log;

		log = getLog(LoggerFactory.class.getName());

		return log;
	}

	@SuppressWarnings("rawtypes")
	public static Log getLog(Class clazz) {
		return getLog(clazz.getName());
	}

	public static Log getLog(String loggerName) {
		if (loggerName == null || "".equals(loggerName.trim())) loggerName = LoggerFactory.class.getName();
		
		Log cacheLog=getCacheInfo(loggerName);
		if(cacheLog!=null) return cacheLog;
		
		try {
			Log log=logConstructor.newInstance(loggerName);
			setCacheInfo(loggerName,log);
			return log;
		} catch (Throwable t) {
			throw new RuntimeException("Error creating logger'" + loggerName + "'.  Cause by: " + t, t);
		}
	}

	private static Class<?> genClassByName(String className) throws ClassNotFoundException {
		Class<?> clazz = null;
		try {
			clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
		} catch (Exception e) {
			//ignore
		}
		if (clazz == null) {
			clazz = Class.forName(className);
		}
		return clazz;
	}

	public static boolean isNoArgInConstructor() {
		return isNoArgInConstructor;
	}
	
	private static void setCacheInfo(String key, Log logger) {
		if (logger == null) return;
		if(key==null || "".equals(key.trim())) return;
		Map<String, Log> map = logLocal.get();
		if (null == map) map = new HashMap<>();
		map.put(key, logger); 
		logLocal.set(map);
	}
	
	private static Log getCacheInfo(String key) {
		Map<String, Log> map = logLocal.get();
		if (null == map) return null;
		Log temp=map.get(key);
		return  temp;
	}

}
