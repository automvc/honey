package org.teasoft.honey.osql.core;

import java.util.List;

import org.teasoft.bee.logging.Log;
import org.teasoft.honey.logging.LoggerFactory;

/**
 * @author Kingstar
 * @since  1.0
 */
public class Logger {

	private static boolean showSQL = HoneyConfig.getHoneyConfig().showSQL;
	private static boolean showSQLShowType = HoneyConfig.getHoneyConfig().showSQL_showType;
	private static boolean showExecutableSql = HoneyConfig.getHoneyConfig().showSQL_executableSql;

	private static Log log = null;

	//专门用于Bee框架输出SQL日志.
	 static void logSQL(String hardStr, String sql) {

		if (!showSQL) return;
		
		//=================================start
		if (LoggerFactory.isNoArgInConstructor()) {
			resetLog();
		} else {
			//不能移走,它表示的是调用所在的位置.
			String callerClass = "";
			try {
				callerClass = sun.reflect.Reflection.getCallerClass().getName();
			} catch (InternalError e) {
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
		//=================================end

		if (showSQL) {
			List list = null;
			String insertIndex = (String) OneTimeParameter.getAttribute("_SYS_Bee_BatchInsert");
			if (HoneyUtil.isMysql() && insertIndex != null) {
				//				mysql批处理,在v1.8开始,不会用于占位设值. 需要清除
				list = HoneyContext.getAndClearPreparedValue(sql);
			} else {
				list = HoneyContext.justGetPreparedValue(sql);
			}
//			list = HoneyContext._justGetPreparedValue(sql); //统一用这个.  bug,只用于打印的,没有删

			String value = HoneyUtil.list2Value(list, showSQLShowType);

			if (value == null || "".equals(value.trim())) {
				_print("[Bee] " + hardStr, sql);
				//				if(showExecutableSql) _println("[Bee] ExecutableSql: "+hardStr, sql);  //无占位的情况       same as log sql.
			} else {
				if(OneTimeParameter.isTrue("_SYS_Bee_BatchInsertFirst")){//batchSize==1	
					_print("[Bee] " + hardStr, sql);
				}else if (insertIndex != null) {
					if ("0".equals(insertIndex) && !HoneyUtil.isMysql()) {
						_print("[Bee] " + hardStr, sql);
					}
					print("[Bee] --> index:" + insertIndex + " ,  [values]: " + value);

				} else {
					_print("[Bee] " + hardStr, sql + "   [values]: " + value);
				}

				if (showExecutableSql) {
					String executableSql = HoneyUtil.getExecutableSql(sql, list);
					if (insertIndex != null && !"0".equals(insertIndex)) {
						int endIndex = executableSql.indexOf("]_End ");
						_println("[Bee] " + hardStr + " ( ExecutableSql " + executableSql.substring(4, endIndex + 1) + " )", executableSql.substring(endIndex + 6) + " ;");
					} else {
						if ("0".equals(insertIndex))
							_println("[Bee] " + hardStr + " ( ExecutableSql [index0])", executableSql);
						else
							_println("[Bee] " + hardStr + " ( ExecutableSql )", executableSql);
					}
				}
			}
		}
	}

	private static void print(String s) {
		log.info(s);
	}

	private static void _print(String s1, String s2) {
		//		log.info(s1+"\n"  +s2);
		log.info(s1 + s2);
	}

	private static void _println(String s1, String s2) {
		log.info(s1 + "\n" + s2);
	}

	public static void debug(String msg) {

		//=================================start
		if (LoggerFactory.isNoArgInConstructor()) {
			resetLog();
		} else {
			//不能移走,它表示的是调用所在的位置.
			String callerClass = "";
			try {
				callerClass = sun.reflect.Reflection.getCallerClass().getName();
			} catch (InternalError e) {
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
		//=================================end

		log.debug(msg);
	}

	public static void info(String msg) {

		//=================================start
		if (LoggerFactory.isNoArgInConstructor()) {
			resetLog();
		} else {
			//不能移走,它表示的是调用所在的位置.
			String callerClass = "";
			try {
				callerClass = sun.reflect.Reflection.getCallerClass().getName();
			} catch (InternalError e) {
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
		//=================================end

		log.info(msg);
	}

	public static void info(Number msg) {

		//=================================start
		if (LoggerFactory.isNoArgInConstructor()) {
			resetLog();
		} else {
			//不能移走,它表示的是调用所在的位置.
			String callerClass = "";
			try {
				callerClass = sun.reflect.Reflection.getCallerClass().getName();
			} catch (InternalError e) {
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
		//=================================end

		log.info(msg + "");
	}

	public static void warn(String msg) {

		//=================================start
		if (LoggerFactory.isNoArgInConstructor()) {
			resetLog();
		} else {
			//不能移走,它表示的是调用所在的位置.
			String callerClass = "";
			try {
				callerClass = sun.reflect.Reflection.getCallerClass().getName();
			} catch (InternalError e) {
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
		//=================================end

		log.warn(msg);
	}

	public static void warn(Number msg) {

		//=================================start
		if (LoggerFactory.isNoArgInConstructor()) {
			resetLog();
		} else {
			//不能移走,它表示的是调用所在的位置.
			String callerClass = "";
			try {
				callerClass = sun.reflect.Reflection.getCallerClass().getName();
			} catch (InternalError e) {
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
		//=================================end

		log.warn(msg + "");
	}

	public static void error(String msg) {

		//=================================start
		if (LoggerFactory.isNoArgInConstructor()) {
			resetLog();
		} else {
			//不能移走,它表示的是调用所在的位置.
			String callerClass = "";
			try {
				callerClass = sun.reflect.Reflection.getCallerClass().getName();
			} catch (InternalError e) {
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
		//=================================end

		log.error(msg);
	}

	public static void error(Number msg) {

		//=================================start
		if (LoggerFactory.isNoArgInConstructor()) {
			resetLog();
		} else {
			//不能移走,它表示的是调用所在的位置.
			String callerClass = "";
			try {
				callerClass = sun.reflect.Reflection.getCallerClass().getName();
			} catch (InternalError e) {
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
		//=================================end

		log.error(msg + "");
	}
	
	private static void resetLog() {
		if (LoggerFactory.isNoArgInConstructor()) {
			log = _getLog(); //Log4jImpl,Slf4jImpl,SystemLogger,NoLogging,FileLogger 可以不需要参数
		} 
	}

	private static void resetLog(String callerClass) {

		if (LoggerFactory.isNoArgInConstructor()) {
			log = _getLog(); //log4j   要解决同时处理两个构造函数
		} else {
			try {
				log = _getLog(callerClass);
			} catch (Exception e) {
				log = _getLog(Logger.class.getName());
			}
		}
	}
	
	private static Log _getLog() {
		Log log = LoggerFactory.getLog();
		return log;
	}

	private static Log _getLog(String className) {
		Log log = LoggerFactory.getLog(className);
		return log;
	}
}
