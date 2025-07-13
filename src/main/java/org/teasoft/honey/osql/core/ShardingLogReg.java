/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class ShardingLogReg {

	private static boolean showShardingSQL() {
		return HoneyConfig.getHoneyConfig().showSQL && HoneyConfig.getHoneyConfig().showShardingSQL;
	}

	public static void log(int size) {
		if (!showShardingSQL()) return;
		HoneyUtil.logSQL("========= Do sharding , the size of sub operation is :" + size);
	}

	public static void regShardingSqlLog(String sqlTitle, int index, String sql) {
		if (!showShardingSQL()) return;

		HoneyUtil.logSQL(sqlTitle + "(sharding " + index + ") : ", sql);
	}

}
