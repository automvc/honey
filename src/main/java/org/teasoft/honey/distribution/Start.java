/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.distribution;

import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.util.DateUtil;

/**
 * @author Kingstar
 * @since  1.17
 */
class Start {

	private static long defaultStart = 1483200000; // 单位：s 2017-01-01 (yyyy-MM-dd)

	public static long getStartSecond() {
		int startYear = HoneyConfig.getHoneyConfig().genid_startYear;
		if (startYear < 1970) return defaultStart;

		long newTime = DateUtil.toTimestamp(startYear + "-01-01 00:00:00").getTime();
		return newTime / 1000;
	}

}
