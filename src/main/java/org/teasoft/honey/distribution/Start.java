/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.distribution;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.teasoft.honey.osql.core.HoneyConfig;

/**
 * @author Kingstar
 * @since  1.17
 */
class Start {

	private static long defaultStart = 1483200000; // 单位：s 2017-01-01 (yyyy-MM-dd)

	public static long getStartSecond() {
		int startYear = HoneyConfig.getHoneyConfig().genid_startYear;
		if (startYear < 1970) return defaultStart;

		try {
			long newTime = toTimestamp(startYear + "-01-01 00:00:00").getTime();
			return newTime / 1000;
		} catch (Exception e) {
			return defaultStart;
		}

	}

	private static Timestamp toTimestamp(String dateString) throws Exception {
		SimpleDateFormat defaultFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = defaultFormat.parse(dateString);
		return new Timestamp(date.getTime());
	}

}
