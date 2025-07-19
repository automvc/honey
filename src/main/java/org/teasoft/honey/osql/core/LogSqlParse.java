/*
 * Copyright 2020-2025 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.List;

import org.teasoft.honey.logging.Logger;

/**
 * @author Kingstar
 * @since  2.5.2
 */
public class LogSqlParse {
	
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
	
	// 专门用于Bee框架输出SQL日志.
	static void logSQL(String hardStr) {
		print(hardStr);
	}
	// 专门用于Bee框架输出SQL日志.
	static void logSQL(String hardStr, String sql) {
		parseSql(hardStr, sql);
	}
	
	private static void parseSql(String hardStr, String sql) {

		if (isShowSQL()) {
			List list = null;
			String insertIndex = (String) OneTimeParameter.getAttribute("_SYS_Bee_BatchInsert");
			if (HoneyUtil.isMysql() && insertIndex != null) {
				// mysql批处理,在v1.8开始,不会用于占位设值. 需要清除
				list = HoneyContext.getAndClearPreparedValue(sql);
			} else {
				list = HoneyContext.justGetPreparedValue(sql); // enhance 数字也来到这 可能是影响行数?
			}
			String value = HoneyUtil.list2Value(list, isShowSQLShowType());

			sql = HoneyUtil.deletePrefix(sql); // 2.2

			if (value == null) {
				_print("[Bee] " + hardStr, sql);
			} else {
				if (insertIndex != null) {
					print("[Bee] --> index:" + insertIndex + " ,  [values]: " + value);

				} else {
					_print("[Bee] " + hardStr, sql + "   [values]: " + value);
				}

				if (isShowExecutableSql()) {
					String executableSql = HoneyUtil.getExecutableSql(sql, list);
					if (insertIndex != null) {
						int endIndex = executableSql.indexOf("]_End ");
						_println("[Bee] " + hardStr + " ( ExecutableSql " + executableSql.substring(4, endIndex + 1)
								+ " )", HoneyUtil.sqlFormat(executableSql.substring(endIndex + 6)));
					} else {
						_println("[Bee] " + hardStr + " ( ExecutableSql )", HoneyUtil.sqlFormat(executableSql));
					}
				}
			}
		}
	}

	private static void _print(String s1, String s2) {
		print(s1 + s2);
	}

	private static void _println(String s1, String s2) {
		print(s1 + "\n" + s2);
	}
	
	private static void print(String s) {
		// 在此判断输出日志的级别.
		// 用户可以自己定义输出sql的日志级别. 比如定义warn才输出sql.
		// 没意义. 因有一个是否显示sql日志了? 但,如果log4j设置了warn, 它还会输出吗? (用log4j时) 不会输出了,所以还是要设置.

		// v1.9.8
		if (getSqlLoggerLevel() == null)
			Logger.info(s);
		else if ("warn".equalsIgnoreCase(getSqlLoggerLevel()))
			Logger.warn(s);
		else if ("error".equalsIgnoreCase(getSqlLoggerLevel()))
			Logger.error(s);
		else
			Logger.info(s);
	}

}
