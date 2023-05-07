/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.dialect;

import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.osql.core.K;
import org.teasoft.honey.osql.core.Logger;

/**
 * @author Kingstar
 * @since  2.1
 */
public class HSqlDbFrontLimitPaging implements DbFeature {

	private static final String KEY = "select ";

	@Override
	public String toPageSql(String sql, int start, int size) {
		HoneyUtil.isRegPagePlaceholder(); // consume,不使用

		String temp = sql.toLowerCase();
		int index = temp.indexOf(KEY);
		if (index >= 0) {
			String part = K.limit + " " + start + " " + size + " ";
			return sql.substring(0, index + KEY.length()) + part
					+ sql.substring(index + KEY.length(), sql.length());
		} else {
			Logger.debug("Error, the sql is not select type!");
			return sql;
		}
	}

	@Override
	public String toPageSql(String sql, int size) {
		return toPageSql(sql, 0, size);
	}

}
