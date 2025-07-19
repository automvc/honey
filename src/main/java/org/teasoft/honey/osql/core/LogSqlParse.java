/*
 * Copyright 2020-2025 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.List;

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

	static String parseSql(String hardStr, String sql) {

		if (isShowSQL()) {
			List list = null;
			String msg;
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
				msg = _print("[Bee] " + hardStr, sql);
			} else {
				if (insertIndex != null) {
					msg = print("[Bee] --> index:" + insertIndex + " ,  [values]: " + value);

				} else {
					msg = _print("[Bee] " + hardStr, sql + "   [values]: " + value);
				}

				if (isShowExecutableSql()) {
					String msg2;
					String executableSql = HoneyUtil.getExecutableSql(sql, list);
					if (insertIndex != null) {
						int endIndex = executableSql.indexOf("]_End ");
						msg2 = _println("[Bee] " + hardStr + " ( ExecutableSql "
								+ executableSql.substring(4, endIndex + 1) + " )",
								HoneyUtil.sqlFormat(executableSql.substring(endIndex + 6)));
					} else {
						msg2 = _println("[Bee] " + hardStr + " ( ExecutableSql )", HoneyUtil.sqlFormat(executableSql));
					}
					msg = msg + "\n" + msg2;
				}
			}
			return msg;
		}
		return "";
	}

	private static String _print(String s1, String s2) {
		return (s1 + s2);
	}

	private static String _println(String s1, String s2) {
		return (s1 + "\n" + s2);
	}

	private static String print(String s) {
		return s;
	}

}
