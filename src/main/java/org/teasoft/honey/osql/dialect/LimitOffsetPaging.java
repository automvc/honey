/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.dialect;

import org.teasoft.bee.osql.dialect.DbFeature;

/**
 * @author Kingstar
 * @since  1.8.6
 */
public class LimitOffsetPaging implements DbFeature {

	@Override
	public String toPageSql(String sql, int offset, int size) {
		return sql +" limit "+size+" offset "+offset;
	}

	@Override
	public String toPageSql(String sql, int size) {
		return sql +" limit "+size;
	}
	

}
