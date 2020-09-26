package org.teasoft.honey.osql.core;

import java.util.List;

import org.teasoft.bee.logging.Log;
import org.teasoft.honey.logging.LoggerFactory;

/**
 * @author Kingstar
 * @since  1.0
 */
public class Logger {

	private static boolean showSQL = HoneyConfig.getHoneyConfig().isShowSQL();
	private static boolean showSQLShowType = HoneyConfig.getHoneyConfig().isShowSQLShowType();
	private static boolean showExecutableSql = HoneyConfig.getHoneyConfig().isShowExecutableSql();

	private static Log log = null;

	//专门用于Bee框架输出SQL日志.
	public static void logSQL(String hardStr, String sql) {

		if (!showSQL) return;

		//=================================start
		//不能移走,它表示的是调用所在的位置.
//		String callerClass = sun.reflect.Reflection.getCallerClass().getName();

        StackTraceElement[] elements = new Throwable().getStackTrace();
        String  callerClass = elements[1].getClassName();
		resetLog(callerClass);
		//=================================end

		if (showSQL) {
			List list = null;
			String insertIndex = (String) OneTimeParameter.getAttribute("_SYS_Bee_BatchInsert");
			if (HoneyUtil.isMysql() && insertIndex != null) {
				//				mysql批处理,在v1.8开始,不会用于占位设值. 需要清除
				list = HoneyContext.getPreparedValue(sql);
			} else {
				list = HoneyContext._justGetPreparedValue(sql);
			}

			String value = HoneyUtil.list2Value(list, showSQLShowType);

			if (value == null || "".equals(value.trim())) {
				_print("[Bee] " + hardStr, sql);
				//				if(showExecutableSql) _println("[Bee] ExecutableSql: "+hardStr, sql);  //无占位的情况       same as log sql.
			} else {
				if (insertIndex != null) {
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
		//不能移走,它表示的是调用所在的位置.
//		String callerClass = sun.reflect.Reflection.getCallerClass().getName();
		//1.8之后，不建议使用sun.reflect.Reflection 中的内容，所以进行修改
		StackTraceElement[] elements = new Throwable().getStackTrace();
		String  callerClass = elements[1].getClassName();
		resetLog(callerClass);
		//=================================end

		log.debug(msg);
	}

	public static void info(String msg) {

//		String callerClass = sun.reflect.Reflection.getCallerClass().getName();
		//1.8之后，不建议使用sun.reflect.Reflection 中的内容，所以进行修改
		StackTraceElement[] elements = new Throwable().getStackTrace();
		String  callerClass = elements[1].getClassName();
		resetLog(callerClass);

		log.info(msg);
	}

	public static void info(Number msg) {

//		String callerClass = sun.reflect.Reflection.getCallerClass().getName();
		//1.8之后，不建议使用sun.reflect.Reflection 中的内容，所以进行修改
		StackTraceElement[] elements = new Throwable().getStackTrace();
		String  callerClass = elements[1].getClassName();
		resetLog(callerClass);

		log.info(msg + "");
	}

	public static void warn(String msg) {

        StackTraceElement[] elements = new Throwable().getStackTrace();
        String  callerClass = elements[1].getClassName();

        resetLog(callerClass);


		log.warn(msg);
	}

	public static void warn(Number msg) {

//		String callerClass = sun.reflect.Reflection.getCallerClass().getName();

		//1.8之后，不建议使用sun.reflect.Reflection 中的内容，所以进行修改
		StackTraceElement[] elements = new Throwable().getStackTrace();
		String  callerClass = elements[1].getClassName();
		resetLog(callerClass);

		log.warn(msg + "");
	}

	public static void error(String msg) {

//		String callerClass = sun.reflect.Reflection.getCallerClass().getName();

        StackTraceElement[] elements = new Throwable().getStackTrace();
        String  callerClass = elements[1].getClassName();

		resetLog(callerClass);

		log.error(msg);
	}

	public static void error(Number msg) {

//		String callerClass = sun.reflect.Reflection.getCallerClass().getName();
		//1.8之后，不建议使用sun.reflect.Reflection 中的内容，所以进行修改
		StackTraceElement[] elements = new Throwable().getStackTrace();
		String  callerClass = elements[1].getClassName();
		resetLog(callerClass);


		log.error(msg + "");
	}

	private static Log _getLog() {
		Log log = LoggerFactory.getLog();
		return log;
	}

	private static Log _getLog(String className) {
		Log log = LoggerFactory.getLog(className);
		return log;
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
}
