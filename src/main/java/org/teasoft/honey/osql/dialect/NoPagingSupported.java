/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.dialect;

import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.osql.exception.NotSupportedException;

/**
 * @author Kingstar
 * @since  1.8.15
 */
public class NoPagingSupported implements DbFeature {

	@Override
	public String toPageSql(String sql, int start, int size) {
		throw new NotSupportedException("Select result did not support paging! You can set the DbFeature implements class with HoneyFactory.");
	}

	@Override
	public String toPageSql(String sql, int size) {
		throw new NotSupportedException("Select result did not support paging! You can set the DbFeature implements class with HoneyFactory.");
	}

}
