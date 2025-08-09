/*
 * Copyright 2013-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.chain;

import org.teasoft.bee.osql.chain.ToSql;

/**
 * @author Kingstar
 * @since  1.3
 */
public abstract class AbstractToSql implements ToSql {

	protected StringBuffer sql = new StringBuffer();

	public String toSQL() {
//		return toSQL(false);
		return toSQL(true); // oracle用jdbc不允许有分号
	}

	public String toSQL(boolean noSemicolon) {
		if (noSemicolon) {
			return sql.toString();
		} else {
			return sql.toString() + ";";
		}
	}

}
