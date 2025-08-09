package org.teasoft.honey.osql.core;

import java.util.List;

import org.teasoft.bee.logging.Log;
import org.teasoft.honey.logging.LoggerFactory;

/**
 * 提供静态方法的日志类.Logger (use the method by static way).
 * @author Kingstar
 * @since  1.0
 */
public class Logger {

	private Logger() {}

	private static boolean isShowSQL() {
		return HoneyConfig.getHoneyConfig().showSQL;
	}

	private static boolean isShowSQLShowType() {
		return HoneyConfig.getHoneyConfig().showSql_showType;
	}

	private static boolean isShowExecutableSql() {
		return HoneyConfig.getHoneyConfig().showSql_showExecutableSql;
	}

	private static String getSqlLoggerLevel() {
		return HoneyConfig.getHoneyConfig().sqlLoggerLevel;
	}

	private static Log log = null;

	// 专门用于Bee框架输出SQL日志.
	static void logSQL(String hardStr, String sql) {

		if (!isShowSQL()) return;

		// =================================start
		if (LoggerFactory.isNoArgInConstructor()) {
			resetLog();
		} else {
			// 不能移走,它表示的是调用所在的位置.
			String callerClass = "";
			try {
				callerClass = sun.reflect.Reflection.getCallerClass().getName();
			} catch (Error e) {
				try {
					callerClass = sun.reflect.Reflection.getCallerClass(2).getName();
				} catch (Throwable t) {
					try {
						callerClass = new Throwable().getStackTrace()[1].getClassName();
					} catch (Exception e2) {
						callerClass = Logger.class.getName();
					}
				}
			}
			resetLog(callerClass);
		}
		// =================================end

		if (isShowSQL()) {
			List list = null;
			String insertIndex = (String) OneTimeParameter.getAttribute("_SYS_Bee_BatchInsert");
			if (HoneyUtil.isMysql() && insertIndex != null) {
				// mysql批处理,在v1.8开始,不会用于占位设值. 需要清除
				list = HoneyContext.getAndClearPreparedValue(sql);
			} else {
				list = HoneyContext.justGetPreparedValue(sql);
			}
//			list = HoneyContext._justGetPreparedValue(sql); //统一用这个.  bug,只用于打印的,没有删

			String value = HoneyUtil.list2Value(list, isShowSQLShowType());

//			if (value == null || "".equals(value.trim())) {
			if (value == null) {
				_print("[Bee] " + hardStr, sql);
			} else {
//				if(OneTimeParameter.isTrue("_SYS_Bee_BatchInsertFirst")){//batchSize==1	
//					_print("[Bee] " + hardStr, sql);
//				}else 
				if (insertIndex != null) {
//					if ("0".equals(insertIndex) && !HoneyUtil.isMysql()) {
//						_print("[Bee] " + hardStr, sql);
//					}
					print("[Bee] --> index:" + insertIndex + " ,  [values]: " + value);

				} else {
					_print("[Bee] " + hardStr, sql + "   [values]: " + value);
				}

				if (isShowExecutableSql()) {
					String executableSql = HoneyUtil.getExecutableSql(sql, list);
					if (insertIndex != null) {
						int endIndex = executableSql.indexOf("]_End ");
						_println("[Bee] " + hardStr + " ( ExecutableSql " + executableSql.substring(4, endIndex + 1)
								+ " )", executableSql.substring(endIndex + 6) + " ;");
//					    if(OneTimeParameter.isTrue("saveSqlString")) print(executableSql.substring(endIndex + 6));
					} else {
//						if ("0".equals(insertIndex))
//							_println("[Bee] " + hardStr + " ( ExecutableSql [index0])", executableSql);
//						else
						_println("[Bee] " + hardStr + " ( ExecutableSql )", executableSql);
//							if(OneTimeParameter.isTrue("saveSqlString")) print(executableSql);
					}
				}
			}
		}
	}

	// 专门用于Bee框架输出SQL日志.
	private static void print(String s) {
		// 在此判断输出日志的级别.
		// 用户可以自己定义输出sql的日志级别. 比如定义warn才输出sql.
		// 没意义. 因有一个是否显示sql日志了? 但,如果log4j设置了warn, 它还会输出吗? (用log4j时) 不会输出了,所以还是要设置.
//		log.info(s);

		// v1.9.8
		if (getSqlLoggerLevel() == null)
			log.info(s);
		else if ("warn".equalsIgnoreCase(getSqlLoggerLevel()))
			log.warn(s);
		else if ("error".equalsIgnoreCase(getSqlLoggerLevel()))
			log.error(s);
		else
			log.info(s);
	}

	// 专门用于Bee框架输出SQL日志.
	private static void _print(String s1, String s2) {
		// log.info(s1+"\n" +s2);
		// v1.9.8
		if (getSqlLoggerLevel() == null)
			log.info(s1 + s2);
		else if ("warn".equalsIgnoreCase(getSqlLoggerLevel()))
			log.warn(s1 + s2);
		else if ("error".equalsIgnoreCase(getSqlLoggerLevel()))
			log.error(s1 + s2);
		else
			log.info(s1 + s2);
	}

	// 专门用于Bee框架输出SQL日志.
	private static void _println(String s1, String s2) {
		// v1.9.8
		if (getSqlLoggerLevel() == null)
			log.info(s1 + "\n" + s2);
		else if ("warn".equalsIgnoreCase(getSqlLoggerLevel()))
			log.warn(s1 + "\n" + s2);
		else if ("error".equalsIgnoreCase(getSqlLoggerLevel()))
			log.error(s1 + "\n" + s2);
		else
			log.info(s1 + "\n" + s2);
	}

	public static void debug(String msg) {

		// =================================start
		if (LoggerFactory.isNoArgInConstructor()) {
			resetLog();
		} else {
			// 不能移走,它表示的是调用所在的位置.
			String callerClass = "";
			try {
				callerClass = sun.reflect.Reflection.getCallerClass().getName();
			} catch (Error e) {
				try {
					callerClass = sun.reflect.Reflection.getCallerClass(2).getName();
				} catch (Throwable t) {
					try {
						callerClass = new Throwable().getStackTrace()[1].getClassName();
					} catch (Exception e2) {
						callerClass = Logger.class.getName();
					}
				}
			}
			resetLog(callerClass);
		}
		// =================================end

		log.debug(msg);
	}

	public static void debug(String msg, Throwable t) {

		// =================================start
		if (LoggerFactory.isNoArgInConstructor()) {
			resetLog();
		} else {
			// 不能移走,它表示的是调用所在的位置.
			String callerClass = "";
			try {
				callerClass = sun.reflect.Reflection.getCallerClass().getName();
			} catch (Error e) {
				try {
					callerClass = sun.reflect.Reflection.getCallerClass(2).getName();
				} catch (Throwable t2) {
					try {
						callerClass = new Throwable().getStackTrace()[1].getClassName();
					} catch (Exception e2) {
						callerClass = Logger.class.getName();
					}
				}
			}
			resetLog(callerClass);
		}
		// =================================end

		log.debug(msg, t);
	}

	public static void info(String msg) {

		// =================================start
		if (LoggerFactory.isNoArgInConstructor()) {
			resetLog();
		} else {
			// 不能移走,它表示的是调用所在的位置.
			String callerClass = "";
			try {
				callerClass = sun.reflect.Reflection.getCallerClass().getName();
			} catch (Error e) {
				try {
					callerClass = sun.reflect.Reflection.getCallerClass(2).getName();
				} catch (Throwable t) {
					try {
						callerClass = new Throwable().getStackTrace()[1].getClassName();
					} catch (Exception e2) {
						callerClass = Logger.class.getName();
					}
				}
			}
			resetLog(callerClass);
		}
		// =================================end

		log.info(msg);
	}

	public static void info(Number msg) {

		// =================================start
		if (LoggerFactory.isNoArgInConstructor()) {
			resetLog();
		} else {
			// 不能移走,它表示的是调用所在的位置.
			String callerClass = "";
			try {
				callerClass = sun.reflect.Reflection.getCallerClass().getName();
			} catch (Error e) {
				try {
					callerClass = sun.reflect.Reflection.getCallerClass(2).getName();
				} catch (Throwable t) {
					try {
						callerClass = new Throwable().getStackTrace()[1].getClassName();
					} catch (Exception e2) {
						callerClass = Logger.class.getName();
					}
				}
			}
			resetLog(callerClass);
		}
		// =================================end

		log.info(msg + "");
	}

	public static void warn(String msg) {

		// =================================start
		if (LoggerFactory.isNoArgInConstructor()) {
			resetLog();
		} else {
			// 不能移走,它表示的是调用所在的位置.
			String callerClass = "";
			try {
				callerClass = sun.reflect.Reflection.getCallerClass().getName();
			} catch (Error e) {
				try {
					callerClass = sun.reflect.Reflection.getCallerClass(2).getName();
				} catch (Throwable t) {
					try {
						callerClass = new Throwable().getStackTrace()[1].getClassName();
					} catch (Exception e2) {
						callerClass = Logger.class.getName();
					}
				}
			}
			resetLog(callerClass);
		}
		// =================================end

		log.warn(msg);
	}

	public static void warn(String msg, Throwable t) {

		// =================================start
		if (LoggerFactory.isNoArgInConstructor()) {
			resetLog();
		} else {
			// 不能移走,它表示的是调用所在的位置.
			String callerClass = "";
			try {
				callerClass = sun.reflect.Reflection.getCallerClass().getName();
			} catch (Error e) {
				try {
					callerClass = sun.reflect.Reflection.getCallerClass(2).getName();
				} catch (Throwable t2) {
					try {
						callerClass = new Throwable().getStackTrace()[1].getClassName();
					} catch (Exception e2) {
						callerClass = Logger.class.getName();
					}
				}
			}
			resetLog(callerClass);
		}
		// =================================end

		log.warn(msg, t);
	}

	public static void warn(Number msg) {

		// =================================start
		if (LoggerFactory.isNoArgInConstructor()) {
			resetLog();
		} else {
			// 不能移走,它表示的是调用所在的位置.
			String callerClass = "";
			try {
				callerClass = sun.reflect.Reflection.getCallerClass().getName();
			} catch (Error e) {
				try {
					callerClass = sun.reflect.Reflection.getCallerClass(2).getName();
				} catch (Throwable t) {
					try {
						callerClass = new Throwable().getStackTrace()[1].getClassName();
					} catch (Exception e2) {
						callerClass = Logger.class.getName();
					}
				}
			}
			resetLog(callerClass);
		}
		// =================================end

		log.warn(msg + "");
	}

	public static void error(String msg) {

		// =================================start
		if (LoggerFactory.isNoArgInConstructor()) {
			resetLog();
		} else {
			// 不能移走,它表示的是调用所在的位置.
			String callerClass = "";
			try {
				callerClass = sun.reflect.Reflection.getCallerClass().getName();
			} catch (Error e) {
				try {
					callerClass = sun.reflect.Reflection.getCallerClass(2).getName();
				} catch (Throwable t) {
					try {
						callerClass = new Throwable().getStackTrace()[1].getClassName();
					} catch (Exception e2) {
						callerClass = Logger.class.getName();
					}
				}
			}
			resetLog(callerClass);
		}
		// =================================end

		log.error(msg);
	}

	public static void error(String msg, Throwable t) {

		// =================================start
		if (LoggerFactory.isNoArgInConstructor()) {
			resetLog();
		} else {
			// 不能移走,它表示的是调用所在的位置.
			String callerClass = "";
			try {
				callerClass = sun.reflect.Reflection.getCallerClass().getName();
			} catch (Error e) {
				try {
					callerClass = sun.reflect.Reflection.getCallerClass(2).getName();
				} catch (Throwable t2) {
					try {
						callerClass = new Throwable().getStackTrace()[1].getClassName();
					} catch (Exception e2) {
						callerClass = Logger.class.getName();
					}
				}
			}
			resetLog(callerClass);
		}
		// =================================end

		log.error(msg, t);
	}

	public static void error(Number msg) {

		// =================================start
		if (LoggerFactory.isNoArgInConstructor()) {
			resetLog();
		} else {
			// 不能移走,它表示的是调用所在的位置.
			String callerClass = "";
			try {
				callerClass = sun.reflect.Reflection.getCallerClass().getName();
			} catch (Error e) {
				try {
					callerClass = sun.reflect.Reflection.getCallerClass(2).getName();
				} catch (Throwable t) {
					try {
						callerClass = new Throwable().getStackTrace()[1].getClassName();
					} catch (Exception e2) {
						callerClass = Logger.class.getName();
					}
				}
			}
			resetLog(callerClass);
		}
		// =================================end

		log.error(msg + "");
	}

	private static void resetLog() {
		if (LoggerFactory.isNoArgInConstructor()) {
			log = _getLog(); // Log4jImpl,Slf4jImpl,SystemLogger,NoLogging,FileLogger 可以不需要参数
		}
	}

	private static void resetLog(String callerClass) {

		if (LoggerFactory.isNoArgInConstructor()) {
			log = _getLog(); // log4j 要解决同时处理两个构造函数
		} else {
			try {
				log = _getLog(callerClass);
			} catch (Exception e) {
				log = _getLog(Logger.class.getName());
			}
		}
	}

	private static Log _getLog() {
		return LoggerFactory.getLog();
	}

	private static Log _getLog(String className) {
		return LoggerFactory.getLog(className);
	}
}
